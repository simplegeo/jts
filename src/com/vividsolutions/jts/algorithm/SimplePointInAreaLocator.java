


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

/**
 * Computes whether a point
 * lies in the interior of an area {@link Geometry}.
 * The algorithm used is only guaranteed to return correct results
 * for points which are <b>not</b> on the boundary of the Geometry.
 *
 * @version 1.7
 */
public class SimplePointInAreaLocator
{

  /**
   * locate is the main location function.  It handles both single-element
   * and multi-element Geometries.  The algorithm for multi-element Geometries
   * is more complex, since it has to take into account the boundaryDetermination rule
   */
  public static int locate(Coordinate p, Geometry geom)
  {
    if (geom.isEmpty()) return Location.EXTERIOR;

    if (containsPoint(p, geom))
      return Location.INTERIOR;
    return Location.EXTERIOR;
  }

  private static boolean containsPoint(Coordinate p, Geometry geom)
  {
    if (geom instanceof Polygon) {
      return containsPointInPolygon(p, (Polygon) geom);
    }
    else if (geom instanceof GeometryCollection) {
      Iterator geomi = new GeometryCollectionIterator((GeometryCollection) geom);
      while (geomi.hasNext()) {
        Geometry g2 = (Geometry) geomi.next();
        if (g2 != geom)
          if (containsPoint(p, g2))
            return true;
      }
    }
    return false;
  }

  public static boolean containsPointInPolygon(Coordinate p, Polygon poly)
  {
    if (poly.isEmpty()) return false;
    LinearRing shell = (LinearRing) poly.getExteriorRing();
    if (! CGAlgorithms.isPointInRing(p, shell.getCoordinates())) return false;
    // now test if the point lies in or on the holes
    for (int i = 0; i < poly.getNumInteriorRing(); i++) {
      LinearRing hole = (LinearRing) poly.getInteriorRingN(i);
      if (CGAlgorithms.isPointInRing(p, hole.getCoordinates())) return false;
    }
    return true;
  }


}
