
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
package com.vividsolutions.jts.geom;

/**
 * Represents a planar triangle, and provides methods for calculating various
 * properties of triangles.
 *
 * @version 1.7
 */
public class Triangle
{
  public Coordinate p0, p1, p2;

  public Triangle(Coordinate p0, Coordinate p1, Coordinate p2)
  {
    this.p0 = p0;
    this.p1 = p1;
    this.p2 = p2;
  }

  /**
   * The inCentre of a triangle is the point which is equidistant
   * from the sides of the triangle.  This is also the point at which the bisectors
   * of the angles meet.
   *
   * @return the point which is the inCentre of the triangle
   */
  public Coordinate inCentre()
  {
    // the lengths of the sides, labelled by their opposite vertex
    double len0 = p1.distance(p2);
    double len1 = p0.distance(p2);
    double len2 = p0.distance(p1);
    double circum = len0 + len1 + len2;

    double inCentreX = (len0 * p0.x + len1 * p1.x +len2 * p2.x)  / circum;
    double inCentreY = (len0 * p0.y + len1 * p1.y +len2 * p2.y)  / circum;
    return new Coordinate(inCentreX, inCentreY);
  }
}
