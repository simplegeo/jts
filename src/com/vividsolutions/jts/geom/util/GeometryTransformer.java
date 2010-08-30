package com.vividsolutions.jts.geom.util;

import java.util.*;
import com.vividsolutions.jts.geom.*;

/**
 * A framework for processes which transform an input {@link Geometry} into
 * an output {@link Geometry}, possibly changing its structure and type(s).
 * This class is a framework for implementing subclasses
 * which perform transformations on
 * various different Geometry subclasses.
 * It provides an easy way of applying specific transformations
 * to given geometry types, while allowing unhandled types to be simply copied.
 * Also, the framework ensures that if subcomponents change type
 * the parent geometries types change appropriately to maintain valid structure.
 * Subclasses will override whichever <code>transformX</code> methods
 * they need to to handle particular Geometry types.
 * <p>
 * A typically usage would be a transformation that may transform Polygons into
 * Polygons, LineStrings
 * or Points.  This class would likely need to override the {@link transformMultiPolygon}
 * method to ensure that if input Polygons change type the result is a GeometryCollection,
 * not a MultiPolygon
 * <p>
 * The default behaviour of this class is to simply recursively transform
 * each Geometry component into an identical object by copying.
 * <p>
 * Note that all <code>transformX</code> methods may return <code>null</code>,
 * to avoid creating empty geometry objects. This will be handled correctly
 * by the transformer.
 * The @link transform} method itself will always
 * return a geometry object.
 *
 * @version 1.7
 *
 * @see GeometryEditor
 */
public class GeometryTransformer
{

  /**
   * Possible extensions:
   * getParent() method to return immediate parent e.g. of LinearRings in Polygons
   */

  private Geometry inputGeom;

  protected GeometryFactory factory = null;

  // these could eventually be exposed to clients
  /**
   * <code>true</code> if empty geometries should not be included in the result
   */
  private boolean pruneEmptyGeometry = true;

  /**
   * <code>true</code> if a homogenous collection result
   * from a {@link GeometryCollection} should still
   * be a general GeometryCollection
   */
  private boolean preserveGeometryCollectionType = true;

  /**
   * <code>true</code> if the output from a collection argument should still be a collection
   */
  private boolean preserveCollections = false;

  /**
   * <code>true</code> if the type of the input should be preserved
   */
  private boolean preserveType = false;

  public GeometryTransformer() {
  }

  public Geometry getInputGeometry() { return inputGeom; }

  public final Geometry transform(Geometry inputGeom)
  {
    this.inputGeom = inputGeom;
    this.factory = inputGeom.getFactory();

    if (inputGeom instanceof Point)
      return transformPoint((Point) inputGeom, null);
    if (inputGeom instanceof MultiPoint)
      return transformMultiPoint((MultiPoint) inputGeom, null);
    if (inputGeom instanceof LinearRing)
      return transformLinearRing((LinearRing) inputGeom, null);
    if (inputGeom instanceof LineString)
      return transformLineString((LineString) inputGeom, null);
    if (inputGeom instanceof MultiLineString)
      return transformMultiLineString((MultiLineString) inputGeom, null);
    if (inputGeom instanceof Polygon)
      return transformPolygon((Polygon) inputGeom, null);
    if (inputGeom instanceof MultiPolygon)
      return transformMultiPolygon((MultiPolygon) inputGeom, null);
    if (inputGeom instanceof GeometryCollection)
      return transformGeometryCollection((GeometryCollection) inputGeom, null);

    throw new IllegalArgumentException("Unknown Geometry subtype: " + inputGeom.getClass().getName());
  }

  /**
   * Convenience method which provides standard way of
   * creating a {@link CoordinateSequence}
   *
   * @param coords the coordinate array to copy
   * @return a coordinate sequence for the array
   */
  protected final CoordinateSequence createCoordinateSequence(Coordinate[] coords)
  {
    return factory.getCoordinateSequenceFactory().create(coords);
  }

  /**
   * Convenience method which provides statndard way of copying {@link CoordinateSequence}s
   * @param seq the sequence to copy
   * @return a deep copy of the sequence
   */
  protected final CoordinateSequence copy(CoordinateSequence seq)
  {
    return (CoordinateSequence) seq.clone();
  }

  protected CoordinateSequence transformCoordinates(CoordinateSequence coords, Geometry parent)
  {
    return copy(coords);
  }

  protected Geometry transformPoint(Point geom, Geometry parent) {
    return factory.createPoint(
        transformCoordinates(geom.getCoordinateSequence(), geom));
  }

  protected Geometry transformMultiPoint(MultiPoint geom, Geometry parent) {
    List transGeomList = new ArrayList();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry transformGeom = transformPoint((Point) geom.getGeometryN(i), geom);
      if (transformGeom == null) continue;
      if (transformGeom.isEmpty()) continue;
      transGeomList.add(transformGeom);
    }
    return factory.buildGeometry(transGeomList);
  }

  protected Geometry transformLinearRing(LinearRing geom, Geometry parent) {
    CoordinateSequence seq = transformCoordinates(geom.getCoordinateSequence(), geom);
    int seqSize = seq.size();
    // ensure a valid LinearRing
    if (seqSize > 0 && seqSize < 4 && ! preserveType)
      return factory.createLineString(seq);
    return factory.createLinearRing(seq);

  }

  protected Geometry transformLineString(LineString geom, Geometry parent) {
    // should check for 1-point sequences and downgrade them to points
    return factory.createLineString(
        transformCoordinates(geom.getCoordinateSequence(), geom));
  }

  protected Geometry transformMultiLineString(MultiLineString geom, Geometry parent) {
    List transGeomList = new ArrayList();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry transformGeom = transformLineString((LineString) geom.getGeometryN(i), geom);
      if (transformGeom == null) continue;
      if (transformGeom.isEmpty()) continue;
      transGeomList.add(transformGeom);
    }
    return factory.buildGeometry(transGeomList);
  }

  protected Geometry transformPolygon(Polygon geom, Geometry parent) {
    boolean isAllValidLinearRings = true;
    Geometry shell = transformLinearRing((LinearRing) geom.getExteriorRing(), geom);

    if (shell == null
        || ! (shell instanceof LinearRing)
        || shell.isEmpty() )
      isAllValidLinearRings = false;
//return factory.createPolygon(null, null);

    ArrayList holes = new ArrayList();
    for (int i = 0; i < geom.getNumInteriorRing(); i++) {
      Geometry hole = transformLinearRing((LinearRing) geom.getInteriorRingN(i), geom);
      if (hole == null || hole.isEmpty()) {
        continue;
      }
      if (! (hole instanceof LinearRing))
        isAllValidLinearRings = false;

      holes.add(hole);
    }

    if (isAllValidLinearRings)
      return factory.createPolygon((LinearRing) shell,
                                   (LinearRing[]) holes.toArray(new LinearRing[] {  }));
    else {
      List components = new ArrayList();
      if (shell != null) components.add(shell);
      components.addAll(holes);
      return factory.buildGeometry(components);
    }
  }

  protected Geometry transformMultiPolygon(MultiPolygon geom, Geometry parent) {
    List transGeomList = new ArrayList();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry transformGeom = transformPolygon((Polygon) geom.getGeometryN(i), geom);
      if (transformGeom == null) continue;
      if (transformGeom.isEmpty()) continue;
      transGeomList.add(transformGeom);
    }
    return factory.buildGeometry(transGeomList);
  }

  protected Geometry transformGeometryCollection(GeometryCollection geom, Geometry parent) {
    List transGeomList = new ArrayList();
    for (int i = 0; i < geom.getNumGeometries(); i++) {
      Geometry transformGeom = transform(geom.getGeometryN(i));
      if (transformGeom == null) continue;
      if (pruneEmptyGeometry && transformGeom.isEmpty()) continue;
      transGeomList.add(transformGeom);
    }
    if (preserveGeometryCollectionType)
      return factory.createGeometryCollection(GeometryFactory.toGeometryArray(transGeomList));
    return factory.buildGeometry(transGeomList);
  }

}