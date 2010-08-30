package com.vividsolutions.jts.simplify;

import java.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.*;

/**
 * Simplifies a {@link Geometry} using the standard Douglas-Peucker algorithm.
 * Ensures that any polygonal geometries returned are valid.
 * Simple lines are not guaranteed to remain simple after simplification.
 * <p>
 * Note that in general D-P does not preserve topology -
 * e.g. polygons can be split, collapse to lines or disappear
 * holes can be created or disappear,
 * and lines can cross.
 * To simplify geometry while preserving topology use {@link TopologyPreservingSimplifier}.
 * (However, using D-P is significantly faster).
 *
 * @version 1.7
 */
public class DouglasPeuckerSimplifier
{

  public static Geometry simplify(Geometry geom, double distanceTolerance)
  {
    DouglasPeuckerSimplifier tss = new DouglasPeuckerSimplifier(geom);
    tss.setDistanceTolerance(distanceTolerance);
    return tss.getResultGeometry();
  }

  private Geometry inputGeom;
  private double distanceTolerance;

  public DouglasPeuckerSimplifier(Geometry inputGeom)
  {
    this.inputGeom = inputGeom;
  }

  /**
   * Sets the distance tolerance for the simplification.
   * All vertices in the simplified geometry will be within this
   * distance of the original geometry.
   * The tolerance value must be non-negative.  A tolerance value
   * of zero is effectively a no-op.
   *
   * @param distanceTolerance the approximation tolerance to use
   */
  public void setDistanceTolerance(double distanceTolerance) {
    if (distanceTolerance < 0.0)
      throw new IllegalArgumentException("Tolerance must be non-negative");
    this.distanceTolerance = distanceTolerance;
  }

  public Geometry getResultGeometry()
  {
    return (new DPTransformer()).transform(inputGeom);
  }

class DPTransformer
    extends GeometryTransformer
{
  protected CoordinateSequence transformCoordinates(CoordinateSequence coords, Geometry parent)
  {
    Coordinate[] inputPts = coords.toCoordinateArray();
    Coordinate[] newPts = DouglasPeuckerLineSimplifier.simplify(inputPts, distanceTolerance);
    return factory.getCoordinateSequenceFactory().create(newPts);
  }

  protected Geometry transformPolygon(Polygon geom, Geometry parent) {
    Geometry roughGeom = super.transformPolygon(geom, parent);
    // don't try and correct if the parent is going to do this
    if (parent instanceof MultiPolygon) {
      return roughGeom;
    }
    return createValidArea(roughGeom);
  }

  protected Geometry transformMultiPolygon(MultiPolygon geom, Geometry parent) {
    Geometry roughGeom = super.transformMultiPolygon(geom, parent);
    return createValidArea(roughGeom);
  }

  /**
   * Creates a valid area geometry from one that possibly has
   * bad topology (i.e. self-intersections).
   * Since buffer can handle invalid topology, but always returns
   * valid geometry, constructing a 0-width buffer "corrects" the
   * topology.
   * Note this only works for area geometries, since buffer always returns
   * areas.  This also may return empty geometries, if the input
   * has no actual area.
   *
   * @param roughAreaGeom an area geometry possibly containing self-intersections
   * @return a valid area geometry
   */
  private Geometry createValidArea(Geometry roughAreaGeom)
  {
    return roughAreaGeom.buffer(0.0);
  }
}

}


