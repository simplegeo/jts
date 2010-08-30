package com.vividsolutions.jts.geom.prep;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.noding.*;

public class LineTopology 
{
	private GeometryFactory geomFact;
	private NodedSegmentString segStr  = null;
	
	public LineTopology(Coordinate[] pts, GeometryFactory geomFact)
	{
		segStr = new NodedSegmentString(pts, this);
		this.geomFact = geomFact;
	}
	
	public void addIntersection(Coordinate intPt, int segmentIndex)
	{
		segStr.addIntersection(intPt, segmentIndex);
	}
	
	public Geometry getResult()
	{
		Coordinate[] resultPts = new Coordinate[0];
		return geomFact.createLineString(resultPts);
	}
	
}
