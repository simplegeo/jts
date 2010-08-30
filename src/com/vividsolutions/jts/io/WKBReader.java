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
package com.vividsolutions.jts.io;

import java.io.IOException;
import com.vividsolutions.jts.geom.*;

/**
 * Reads a {@link Geometry}from a byte stream in Well-Known Binary format.
 * Supports use of an {@link InStream}, which allows easy use
 * with arbitary byte stream sources.
 * <p>
 * This class is designed to support reuse of a single instance to read multiple
 * geometries. This class is not thread-safe; each thread should create its own
 * instance.
 *
 * @see WKBWriter
 */
public class WKBReader
{
  private static final String INVALID_GEOM_TYPE_MSG
  = "Invalid geometry type encountered in ";

  private GeometryFactory factory;
  private PrecisionModel precisionModel;
  // default dimension - will be set on read
  private int inputDimension = 2;
  private ByteOrderDataInStream dis = new ByteOrderDataInStream();
  private double[] ordValues;

  public WKBReader() {
    this(new GeometryFactory());
  }

  public WKBReader(GeometryFactory geometryFactory) {
    this.factory = geometryFactory;
    precisionModel = factory.getPrecisionModel();
  }

  /**
   * Reads a single {@link Geometry} from a byte array.
   *
   * @param bytes the byte array to read from
   * @return the geometry read
   * @throws IOException if an input exception occurs
   * @throws ParseException if a parse exception occurs
   */
  public Geometry read(byte[] bytes) throws IOException, ParseException
  {
    // possibly reuse the ByteArrayInStream?
    return read(new ByteArrayInStream(bytes));
  }

  /**
   * Reads a {@link Geometry} from an {@link InStream).
   *
   * @param is the stream to read from
   * @return the Geometry read
   * @throws IOException
   * @throws ParseException
   */
  public Geometry read(InStream is)
  throws IOException, ParseException
  {
    dis.setInStream(is);


    return readGeometry();
  }

  private Geometry readGeometry()
  throws IOException, ParseException
  {
    // determine byte order
    byte byteOrder = dis.readByte();
    // default is big endian
    if (byteOrder == WKBConstants.wkbNDR)
      dis.setOrder(ByteOrderValues.LITTLE_ENDIAN);

    int typeInt = dis.readInt();
    int geometryType = typeInt & 0xff;
    boolean hasZ = (typeInt & 0x80000000) != 0;
    inputDimension =  hasZ ? 3 : 2;

    // only allocate ordValues buffer if necessary
    if (ordValues == null || ordValues.length < inputDimension)
      ordValues = new double[inputDimension];

    switch (geometryType) {
      case WKBConstants.wkbPoint :
        return readPoint();
      case WKBConstants.wkbLineString :
        return readLineString();
      case WKBConstants.wkbPolygon :
        return readPolygon();
      case WKBConstants.wkbMultiPoint :
        return readMultiPoint();
      case WKBConstants.wkbMultiLineString :
        return readMultiLineString();
      case WKBConstants.wkbMultiPolygon :
        return readMultiPolygon();
      case WKBConstants.wkbGeometryCollection :
        return readGeometryCollection();
    }
    throw new ParseException("Unknown WKB type " + geometryType);
    //return null;
  }

  private Point readPoint() throws IOException
  {
    CoordinateSequence pts = readCoordinateSequence(1);
    return factory.createPoint(pts);
  }

  private LineString readLineString() throws IOException
  {
    int size = dis.readInt();
    CoordinateSequence pts = readCoordinateSequence(size);
    return factory.createLineString(pts);
  }

  private LinearRing readLinearRing() throws IOException
  {
    int size = dis.readInt();
    CoordinateSequence pts = readCoordinateSequence(size);
    return factory.createLinearRing(pts);
  }

  private Polygon readPolygon() throws IOException
  {
    int numRings = dis.readInt();
    LinearRing[] holes = null;
    if (numRings > 1)
      holes = new LinearRing[numRings - 1];

    LinearRing shell = readLinearRing();
    for (int i = 0; i < numRings - 1; i++) {
      holes[i] = readLinearRing();
    }
    return factory.createPolygon(shell, holes);
  }

  private MultiPoint readMultiPoint() throws IOException, ParseException
  {
    int numGeom = dis.readInt();
    Point[] geoms = new Point[numGeom];
    for (int i = 0; i < numGeom; i++) {
      Geometry g = readGeometry();
      if (! (g instanceof Point))
        throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPoint");
      geoms[i] = (Point) g;
    }
    return factory.createMultiPoint(geoms);
  }

  private MultiLineString readMultiLineString() throws IOException, ParseException
  {
    int numGeom = dis.readInt();
    LineString[] geoms = new LineString[numGeom];
    for (int i = 0; i < numGeom; i++) {
      Geometry g = readGeometry();
      if (! (g instanceof LineString))
        throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiLineString");
      geoms[i] = (LineString) g;
    }
    return factory.createMultiLineString(geoms);
  }

  private MultiPolygon readMultiPolygon() throws IOException, ParseException
  {
    int numGeom = dis.readInt();
    Polygon[] geoms = new Polygon[numGeom];
    for (int i = 0; i < numGeom; i++) {
      Geometry g = readGeometry();
      if (! (g instanceof Polygon))
        throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPolygon");
      geoms[i] = (Polygon) g;
    }
    return factory.createMultiPolygon(geoms);
  }

  private GeometryCollection readGeometryCollection() throws IOException, ParseException
  {
    int numGeom = dis.readInt();
    Geometry[] geoms = new Geometry[numGeom];
    for (int i = 0; i < numGeom; i++) {
      geoms[i] = readGeometry();
    }
    return factory.createGeometryCollection(geoms);
  }

  private CoordinateSequence readCoordinateSequence(int size) throws IOException
  {
    CoordinateSequence seq = factory.getCoordinateSequenceFactory().create(size, inputDimension);
    int targetDim = seq.getDimension();
    if (targetDim > inputDimension)
      targetDim = inputDimension;
    for (int i = 0; i < size; i++) {
      readCoordinate();
      for (int j = 0; j < targetDim; j++) {
        seq.setOrdinate(i, j, ordValues[j]);
      }
    }
    return seq;
  }

  /**
   * Reads a coordinate value with the specified dimensionality.
   * Makes the X and Y ordinates precise according to the precision model
   * in use.
   */
  private void readCoordinate() throws IOException
  {
    for (int i = 0; i < inputDimension; i++) {
      if (i <= 1) {
        ordValues[i] = precisionModel.makePrecise(dis.readDouble());
      }
      else {
        ordValues[i] = dis.readDouble();
      }

    }
  }

}