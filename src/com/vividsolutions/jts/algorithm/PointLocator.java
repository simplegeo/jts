


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
package com.vividsolutions.jts.algorithm;

import java.util.Iterator;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geomgraph.GeometryGraph;

/**
 * Computes the topological relationship ({@link Location})
 * of a single point to a {@link Geometry}.
 * The algorithm obeys the SFS Boundary Determination Rule to determine
 * whether the point lies on the boundary or not.
 * <p>
 * Instances of this class are not reentrant.
 *
 * @version 1.7
 */
public class PointLocator
{
  private boolean isIn;         // true if the point lies in or on any Geometry element
  private int numBoundaries;    // the number of sub-elements whose boundaries the point lies in

  public PointLocator() {
  }

  /**
   * Convenience method to test a point for intersection with
   * a Geometry
   * @param p the coordinate to test
   * @param geom the Geometry to test
   * @return <code>true</code> if the point is in the interior or boundary of the Geometry
   */
  public boolean intersects(Coordinate p, Geometry geom)
  {
    return locate(p, geom) != Location.EXTERIOR;
  }

  /**
   * Computes the topological relationship ({@link Location}) of a single point
   * to a Geometry.
   * It handles both single-element
   * and multi-element Geometries.
   * The algorithm for multi-part Geometries
   * takes into account the SFS Boundary Determination Rule.
   *
   * @return the {@link Location} of the point relative to the input Geometry
   */
  public int locate(Coordinate p, Geometry geom)
  {
    if (geom.isEmpty()) return Location.EXTERIOR;

    if (geom instanceof LinearRing) {
      return locate(p, (LinearRing) geom);
    }
    if (geom instanceof LineString) {
      return locate(p, (LineString) geom);
    }
    else if (geom instanceof Polygon) {
      return locate(p, (Polygon) geom);
    }

    isIn = false;
    numBoundaries = 0;
    computeLocation(p, geom);
    if (GeometryGraph.isInBoundary(numBoundaries)) return Location.BOUNDARY;
    if (numBoundaries > 0 || isIn) return Location.INTERIOR;
    return Location.EXTERIOR;
  }

  private void computeLocation(Coordinate p, Geometry geom)
  {
    if (geom instanceof LinearRing) {
      updateLocationInfo(locate(p, (LinearRing) geom));
    }
    if (geom instanceof LineString) {
      updateLocationInfo(locate(p, (LineString) geom));
    }
    else if (geom instanceof Polygon) {
      updateLocationInfo(locate(p, (Polygon) geom));
    }
    else if (geom instanceof MultiLineString) {
      MultiLineString ml = (MultiLineString) geom;
      for (int i = 0; i < ml.getNumGeometries(); i++) {
        LineString l = (LineString) ml.getGeometryN(i);
        updateLocationInfo(locate(p, l));
      }
    }
    else if (geom instanceof MultiPolygon) {
      MultiPolygon mpoly = (MultiPolygon) geom;
      for (int i = 0; i < mpoly.getNumGeometries(); i++) {
        Polygon poly = (Polygon) mpoly.getGeometryN(i);
        updateLocationInfo(locate(p, poly));
      }
    }
    else if (geom instanceof GeometryCollection) {
      Iterator geomi = new GeometryCollectionIterator((GeometryCollection) geom);
      while (geomi.hasNext()) {
        Geometry g2 = (Geometry) geomi.next();
        if (g2 != geom)
          computeLocation(p, g2);
      }
    }
  }

  private void updateLocationInfo(int loc)
  {
    if (loc == Location.INTERIOR) isIn = true;
    if (loc == Location.BOUNDARY) numBoundaries++;
  }

  private int locate(Coordinate p, LineString l)
  {
    Coordinate[] pt = l.getCoordinates();
    if (! l.isClosed()) {
      if (p.equals(pt[0])
          || p.equals(pt[pt.length - 1]) ) {
        return Location.BOUNDARY;
      }
    }
    if (CGAlgorithms.isOnLine(p, pt))
      return Location.INTERIOR;
    return Location.EXTERIOR;
  }

  private int locate(Coordinate p, LinearRing ring)
  {
    // can this test be folded into isPointInRing ?
    if (CGAlgorithms.isOnLine(p, ring.getCoordinates())) {
      return Location.BOUNDARY;
    }
    if (CGAlgorithms.isPointInRing(p, ring.getCoordinates()))
      return Location.INTERIOR;
    return Location.EXTERIOR;
  }

  private int locate(Coordinate p, Polygon poly)
  {
    if (poly.isEmpty()) return Location.EXTERIOR;
    LinearRing shell = (LinearRing) poly.getExteriorRing();

    int shellLoc = locate(p, shell);
    if (shellLoc == Location.EXTERIOR) return Location.EXTERIOR;
    if (shellLoc == Location.BOUNDARY) return Location.BOUNDARY;
    // now test if the point lies in or on the holes
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      LinearRing hole = (LinearRing) poly.getInteriorRingN(i);
      int holeLoc = locate(p, hole);
      if (holeLoc == Location.INTERIOR) return Location.EXTERIOR;
      if (holeLoc == Location.BOUNDARY) return Location.BOUNDARY;
    }
    return Location.INTERIOR;
  }
}
