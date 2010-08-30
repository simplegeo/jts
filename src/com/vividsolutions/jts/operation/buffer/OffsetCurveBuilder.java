/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.vividsolutions.jts.operation.buffer;

import java.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geomgraph.*;

/**
 * Computes the raw offset curve for a
 * single {@link Geometry} component (ring, line or point).
 * A raw offset curve line is not noded -
 * it may contain self-intersections (and usually will).
 * The final buffer polygon is computed by forming a topological graph
 * of all the noded raw curves and tracing outside contours.
 * The points in the raw curve are rounded to the required precision model.
 *
 * @version 1.7
 */
public class OffsetCurveBuilder {

  private static double PI_OVER_2 = Math.PI / 2.0;

  /**
   * The default number of facets into which to divide a fillet of 90 degrees.
   * A value of 8 gives less than 2% max error in the buffer distance.
   * For a max error of < 1%, use QS = 12
   */
  public static final int DEFAULT_QUADRANT_SEGMENTS = 8;

  private static final Coordinate[] arrayTypeCoordinate = new Coordinate[0];
  private CGAlgorithms cga = new RobustCGAlgorithms();
  private LineIntersector li;

  /**
   * The angle quantum with which to approximate a fillet curve
   * (based on the input # of quadrant segments)
   */
  private double filletAngleQuantum;
  /**
   * the max error of approximation between a quad segment and the true fillet curve
   */
  private double maxCurveSegmentError = 0.0;
  private ArrayList ptList;
  private double distance = 0.0;
  private PrecisionModel precisionModel;
  private int endCapStyle = BufferOp.CAP_ROUND;
  private int joinStyle;

  public OffsetCurveBuilder(
                PrecisionModel precisionModel)
  {
    this(precisionModel, DEFAULT_QUADRANT_SEGMENTS);
  }

  public OffsetCurveBuilder(
                PrecisionModel precisionModel,
                int quadrantSegments)
  {
    this.precisionModel = precisionModel;
    // compute intersections in full precision, to provide accuracy
    // the points are rounded as they are inserted into the curve line
    li = new RobustLineIntersector();

    int limitedQuadSegs = quadrantSegments < 1 ? 1 : quadrantSegments;
    filletAngleQuantum = Math.PI / 2.0 / limitedQuadSegs;
  }

  public void setEndCapStyle(int endCapStyle)
  {
    this.endCapStyle = endCapStyle;
  }
  /**
   * This method handles single points as well as lines.
   * Lines are assumed to <b>not</b> be closed (the function will not
   * fail for closed lines, but will generate superfluous line caps).
   *
   * @return a List of Coordinate[]
   */
  public List getLineCurve(Coordinate[] inputPts, double distance)
  {
    List lineList = new ArrayList();
    // a zero or negative width buffer of a line/point is empty
    if (distance <= 0.0) return lineList;

    init(distance);
    if (inputPts.length <= 1) {
      switch (endCapStyle) {
        case BufferOp.CAP_ROUND:
          addCircle(inputPts[0], distance);
          break;
        case BufferOp.CAP_SQUARE:
          addSquare(inputPts[0], distance);
          break;
          // default is for buffer to be empty (e.g. for a butt line cap);
      }
    }
    else
      computeLineBufferCurve(inputPts);
    Coordinate[] lineCoord = getCoordinates();
    lineList.add(lineCoord);
    return lineList;
  }

  /**
   * This method handles the degenerate cases of single points and lines,
   * as well as rings.
   *
   * @return a List of Coordinate[]
   */
  public List getRingCurve(Coordinate[] inputPts, int side, double distance)
  {
    List lineList = new ArrayList();
    init(distance);
    if (inputPts.length <= 2)
      return getLineCurve(inputPts, distance);

    // optimize creating ring for for zero distance
    if (distance == 0.0) {
      lineList.add(copyCoordinates(inputPts));
      return lineList;
    }
    computeRingBufferCurve(inputPts, side);
    lineList.add(getCoordinates());
    return lineList;
  }

  private static Coordinate[] copyCoordinates(Coordinate[] pts)
  {
    Coordinate[] copy = new Coordinate[pts.length];
    for (int i = 0; i < copy.length; i++) {
      copy[i] = new Coordinate(pts[i]);
    }
    return copy;
  }
  private void init(double distance)
  {
    this.distance = distance;
    maxCurveSegmentError = distance * (1 - Math.cos(filletAngleQuantum / 2.0));
    ptList = new ArrayList();
  }
  private Coordinate[] getCoordinates()
  {
    // check that points are a ring - add the startpoint again if they are not
    if (ptList.size() > 1) {
      Coordinate start  = (Coordinate) ptList.get(0);
      Coordinate end    = (Coordinate) ptList.get(1);
      if (! start.equals(end) ) addPt(start);
    }

    Coordinate[] coord = (Coordinate[]) ptList.toArray(arrayTypeCoordinate);
    return coord;
  }

  private void computeLineBufferCurve(Coordinate[] inputPts)
  {
    int n = inputPts.length - 1;

    // compute points for left side of line
    initSideSegments(inputPts[0], inputPts[1], Position.LEFT);
    for (int i = 2; i <= n; i++) {
      addNextSegment(inputPts[i], true);
    }
    addLastSegment();
    // add line cap for end of line
    addLineEndCap(inputPts[n - 1], inputPts[n]);

    // compute points for right side of line
    initSideSegments(inputPts[n], inputPts[n - 1], Position.LEFT);
    for (int i = n - 2; i >= 0; i--) {
      addNextSegment(inputPts[i], true);
    }
    addLastSegment();
    // add line cap for start of line
    addLineEndCap(inputPts[1], inputPts[0]);

    closePts();
  }

  private void computeRingBufferCurve(Coordinate[] inputPts, int side)
  {
    int n = inputPts.length - 1;
    initSideSegments(inputPts[n - 1], inputPts[0], side);
    for (int i = 1; i <= n; i++) {
      boolean addStartPoint = i != 1;
      addNextSegment(inputPts[i], addStartPoint);
    }
    closePts();
  }

  private void addPt(Coordinate pt)
  {
    Coordinate bufPt = new Coordinate(pt);
    precisionModel.makePrecise(bufPt);
    // don't add duplicate points
    Coordinate lastPt = null;
    if (ptList.size() >= 1)
      lastPt = (Coordinate) ptList.get(ptList.size() - 1);
    if (lastPt != null && bufPt.equals(lastPt)) return;

    ptList.add(bufPt);
//System.out.println(bufPt);
  }
  private void closePts()
  {
    if (ptList.size() < 1) return;
    Coordinate startPt = new Coordinate((Coordinate) ptList.get(0));
    Coordinate lastPt = (Coordinate) ptList.get(ptList.size() - 1);
    Coordinate last2Pt = null;
    if (ptList.size() >= 2)
      last2Pt = (Coordinate) ptList.get(ptList.size() - 2);
    if (startPt.equals(lastPt)) return;
    ptList.add(startPt);
  }

  private Coordinate s0, s1, s2;
  private LineSegment seg0 = new LineSegment();
  private LineSegment seg1 = new LineSegment();
  private LineSegment offset0 = new LineSegment();
  private LineSegment offset1 = new LineSegment();
  private int side = 0;

  private void initSideSegments(Coordinate s1, Coordinate s2, int side)
  {
    this.s1 = s1;
    this.s2 = s2;
    this.side = side;
    seg1.setCoordinates(s1, s2);
    computeOffsetSegment(seg1, side, distance, offset1);
  }

  private static double MAX_CLOSING_SEG_LEN = 3.0;

  private void addNextSegment(Coordinate p, boolean addStartPoint)
  {
    // s0-s1-s2 are the coordinates of the previous segment and the current one
    s0 = s1;
    s1 = s2;
    s2 = p;
    seg0.setCoordinates(s0, s1);
    computeOffsetSegment(seg0, side, distance, offset0);
    seg1.setCoordinates(s1, s2);
    computeOffsetSegment(seg1, side, distance, offset1);

    // do nothing if points are equal
    if (s1.equals(s2)) return;

    int orientation = cga.computeOrientation(s0, s1, s2);
    boolean outsideTurn =
          (orientation == CGAlgorithms.CLOCKWISE        && side == Position.LEFT)
      ||  (orientation == CGAlgorithms.COUNTERCLOCKWISE && side == Position.RIGHT);

    if (orientation == 0) { // lines are collinear
      li.computeIntersection( s0, s1,
                              s1, s2  );
      int numInt = li.getIntersectionNum();
      /**
       * if numInt is < 2, the lines are parallel and in the same direction.
       * In this case the point can be ignored, since the offset lines will also be
       * parallel.
       */
      if (numInt >= 2) {
      /**
       * segments are collinear but reversing.  Have to add an "end-cap" fillet
       * all the way around to other direction
       * This case should ONLY happen for LineStrings, so the orientation is always CW.
       * (Polygons can never have two consecutive segments which are parallel but reversed,
       * because that would be a self intersection.
       */
        addFillet(s1, offset0.p1, offset1.p0, CGAlgorithms.CLOCKWISE, distance);
      }
    }
    else if (outsideTurn) {
        // add a fillet to connect the endpoints of the offset segments
        if (addStartPoint) addPt(offset0.p1);
        // TESTING - comment out to produce beveled joins
        addFillet(s1, offset0.p1, offset1.p0, orientation, distance);
        addPt(offset1.p0);
    }
    else { // inside turn
      /**
       * add intersection point of offset segments (if any)
       */
        li.computeIntersection( offset0.p0, offset0.p1,
                              offset1.p0, offset1.p1  );
        if (li.hasIntersection()) {
          addPt(li.getIntersection(0));
        }
        else {
      /**
       * If no intersection, it means the angle is so small and/or the offset so large
       * that the offsets segments don't intersect.
       * In this case we must add a offset joining curve to make sure the buffer line
       * is continuous and tracks the buffer correctly around the corner.
       * Note that the joining curve won't appear in the final buffer.
       *
       * The intersection test above is vulnerable to robustness errors;
       * i.e. it may be that the offsets should intersect very close to their
       * endpoints, but don't due to rounding.  To handle this situation
       * appropriately, we use the following test:
       * If the offset points are very close, don't add a joining curve
       * but simply use one of the offset points
       */
            if (offset0.p1.distance(offset1.p0) < distance / 1000.0) {
              addPt(offset0.p1);
            }
            else {
              // add endpoint of this segment offset
              addPt(offset0.p1);
// <FIX> MD - add in centre point of corner, to make sure offset closer lines have correct topology
              addPt(s1);
              addPt(offset1.p0);
            }
        }
    }
  }
  /**
   * Add last offset point
   */
  private void addLastSegment()
  {
    addPt(offset1.p1);
  }

  /**
   * Compute an offset segment for an input segment on a given side and at a given distance.
   * The offset points are computed in full double precision, for accuracy.
   *
   * @param seg the segment to offset
   * @param side the side of the segment ({@link Position}) the offset lies on
   * @param distance the offset distance
   * @param offset the points computed for the offset segment
   */
  private void computeOffsetSegment(LineSegment seg, int side, double distance, LineSegment offset)
  {
    int sideSign = side == Position.LEFT ? 1 : -1;
    double dx = seg.p1.x - seg.p0.x;
    double dy = seg.p1.y - seg.p0.y;
    double len = Math.sqrt(dx * dx + dy * dy);
    // u is the vector that is the length of the offset, in the direction of the segment
    double ux = sideSign * distance * dx / len;
    double uy = sideSign * distance * dy / len;
    offset.p0.x = seg.p0.x - uy;
    offset.p0.y = seg.p0.y + ux;
    offset.p1.x = seg.p1.x - uy;
    offset.p1.y = seg.p1.y + ux;
  }

  /**
   * Add an end cap around point p1, terminating a line segment coming from p0
   */
  private void addLineEndCap(Coordinate p0, Coordinate p1)
  {
    LineSegment seg = new LineSegment(p0, p1);

    LineSegment offsetL = new LineSegment();
    computeOffsetSegment(seg, Position.LEFT, distance, offsetL);
    LineSegment offsetR = new LineSegment();
    computeOffsetSegment(seg, Position.RIGHT, distance, offsetR);

    double dx = p1.x - p0.x;
    double dy = p1.y - p0.y;
    double angle = Math.atan2(dy, dx);

    switch (endCapStyle) {
      case BufferOp.CAP_ROUND:
        // add offset seg points with a fillet between them
        addPt(offsetL.p1);
        addFillet(p1, angle + Math.PI / 2, angle - Math.PI / 2, CGAlgorithms.CLOCKWISE, distance);
        addPt(offsetR.p1);
        break;
      case BufferOp.CAP_BUTT:
        // only offset segment points are added
        addPt(offsetL.p1);
        addPt(offsetR.p1);
        break;
      case BufferOp.CAP_SQUARE:
        // add a square defined by extensions of the offset segment endpoints
        Coordinate squareCapSideOffset = new Coordinate();
        squareCapSideOffset.x = Math.abs(distance) * Math.cos(angle);
        squareCapSideOffset.y = Math.abs(distance) * Math.sin(angle);

        Coordinate squareCapLOffset = new Coordinate(
            offsetL.p1.x + squareCapSideOffset.x,
            offsetL.p1.y + squareCapSideOffset.y);
        Coordinate squareCapROffset = new Coordinate(
            offsetR.p1.x + squareCapSideOffset.x,
            offsetR.p1.y + squareCapSideOffset.y);
        addPt(squareCapLOffset);
        addPt(squareCapROffset);
        break;

    }
  }
  /**
   * @param p base point of curve
   * @param p0 start point of fillet curve
   * @param p1 endpoint of fillet curve
   */
  private void addFillet(Coordinate p, Coordinate p0, Coordinate p1, int direction, double distance)
  {
    double dx0 = p0.x - p.x;
    double dy0 = p0.y - p.y;
    double startAngle = Math.atan2(dy0, dx0);
    double dx1 = p1.x - p.x;
    double dy1 = p1.y - p.y;
    double endAngle = Math.atan2(dy1, dx1);

    if (direction == CGAlgorithms.CLOCKWISE) {
      if (startAngle <= endAngle) startAngle += 2.0 * Math.PI;
    }
    else {    // direction == COUNTERCLOCKWISE
      if (startAngle >= endAngle) startAngle -= 2.0 * Math.PI;
    }
    addPt(p0);
    addFillet(p, startAngle, endAngle, direction, distance);
    addPt(p1);
  }

  /**
   * Adds points for a fillet.  The start and end point for the fillet are not added -
   * the caller must add them if required.
   *
   * @param direction is -1 for a CW angle, 1 for a CCW angle
   */
  private void addFillet(Coordinate p, double startAngle, double endAngle, int direction, double distance)
  {
    int directionFactor = direction == CGAlgorithms.CLOCKWISE ? -1 : 1;

    double totalAngle = Math.abs(startAngle - endAngle);
    int nSegs = (int) (totalAngle / filletAngleQuantum + 0.5);

    if (nSegs < 1) return;    // no segments because angle is less than increment - nothing to do!

    double initAngle, currAngleInc;

    // choose angle increment so that each segment has equal length
    initAngle = 0.0;
    currAngleInc = totalAngle / nSegs;

    double currAngle = initAngle;
    Coordinate pt = new Coordinate();
    while (currAngle < totalAngle) {
      double angle = startAngle + directionFactor * currAngle;
      pt.x = p.x + distance * Math.cos(angle);
      pt.y = p.y + distance * Math.sin(angle);
      addPt(pt);
      currAngle += currAngleInc;
    }
  }


  /**
   * Adds a CW circle around a point
   */
  private void addCircle(Coordinate p, double distance)
  {
    // add start point
    Coordinate pt = new Coordinate(p.x + distance, p.y);
    addPt(pt);
    addFillet(p, 0.0, 2.0 * Math.PI, -1, distance);
  }

  /**
   * Adds a CW square around a point
   */
  private void addSquare(Coordinate p, double distance)
  {
    // add start point
    addPt(new Coordinate(p.x + distance, p.y + distance));
    addPt(new Coordinate(p.x + distance, p.y - distance));
    addPt(new Coordinate(p.x - distance, p.y - distance));
    addPt(new Coordinate(p.x - distance, p.y + distance));
    addPt(new Coordinate(p.x + distance, p.y + distance));
  }
}
