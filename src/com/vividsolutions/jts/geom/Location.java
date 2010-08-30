

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
 *  Constants representing the location of a point relative to a geometry. They
 *  can also be thought of as the row or column index of a DE-9IM matrix. For a
 *  description of the DE-9IM, see the <A
 *  HREF="http://www.opengis.org/techno/specs.htm">OpenGIS Simple Features
 *  Specification for SQL</A> .
 *
 *@version 1.7
 */
public class Location {
  /**
   *  DE-9IM row index of the interior of the first geometry and column index of
   *  the interior of the second geometry. Location value for the interior of a
   *  geometry.
   */
  public final static int INTERIOR = 0;
  /**
   *  DE-9IM row index of the boundary of the first geometry and column index of
   *  the boundary of the second geometry. Location value for the boundary of a
   *  geometry.
   */
  public final static int BOUNDARY = 1;
  /**
   *  DE-9IM row index of the exterior of the first geometry and column index of
   *  the exterior of the second geometry. Location value for the exterior of a
   *  geometry.
   */
  public final static int EXTERIOR = 2;

  /**
   *  Used for uninitialized location values.
   */
  public final static int NONE = -1;

  /**
   *  Converts the location value to a location symbol, for example, <code>EXTERIOR => 'e'</code>
   *  .
   *
   *@param  locationValue  either EXTERIOR, BOUNDARY, INTERIOR or NONE
   *@return                either 'e', 'b', 'i' or '-'
   */
  public static char toLocationSymbol(int locationValue) {
    switch (locationValue) {
      case EXTERIOR:
        return 'e';
      case BOUNDARY:
        return 'b';
      case INTERIOR:
        return 'i';
      case NONE:
        return '-';
    }
    throw new IllegalArgumentException("Unknown location value: " + locationValue);
  }
}


