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
package com.vividsolutions.jts.geom.prep;

import java.util.*;


import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.noding.*;
import com.vividsolutions.jts.geom.util.*;

/**
 * Computes the <tt>intersection</tt> spatial overlay function
 * for a target {@link PreparedLineString} relative to other {@link Geometry} classes.
 * Uses indexing to improve performance. 
 * 
 * @author Martin Davis
 *
 */
public class PreparedPolygonLineIntersection 
{
	/**
	 * Computes the intersection between a {@link PreparedLineString}
	 * and a {@link Geometry}.
	 * 
	 * @param prep the prepared linestring
	 * @param geom a test geometry
	 * @return the intersection geometry
	 */
	public static Geometry intersection(PreparedPolygon prep, Geometry geom)
	{
		PreparedPolygonLineIntersection op = new PreparedPolygonLineIntersection(prep);
    return op.intersection(geom);
	}

	protected PreparedPolygon prepPoly;

  /**
   * Creates an instance of this operation.
   * 
   * @param prepPoly the target PreparedPolygon
   */
	public PreparedPolygonLineIntersection(PreparedPolygon prepPoly)
	{
		this.prepPoly = prepPoly;
	}
	
	/**
	 * Computes the intersection of this geometry with the given geometry.
	 * 
	 * @param geom the test geometry
	 * @return a geometry corresponding to the intersection point set
	 */
	public Geometry intersection(Geometry geom)
	{
		// only handle A/L case for now
		if (! (geom instanceof LineString))
				return prepPoly.getGeometry().intersection(geom);
		
		// TODO: handle multilinestrings
		Coordinate[] pts = geom.getCoordinates();
		LineTopology lineTopo = new LineTopology(pts, geom.getFactory());
		computeIntersection(lineTopo);
		return lineTopo.getResult();

	}
	  
  LineIntersector li = new RobustLineIntersector();

	private void computeIntersection(LineTopology lineTopo)
	{
		SegmentIntersectionDetector intDetector = new SegmentIntersectionDetector(li);
		intDetector.setFindAllIntersectionTypes(true);
//		prepPoly.getIntersectionFinder().intersects(lineSegStr, intDetector);
	}
	
	
}
