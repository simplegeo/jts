package com.vividsolutions.jts.operation.buffer.validate;

import java.util.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.*;
import com.vividsolutions.jts.operation.distance.*;

/**
 * Validates that a given buffer curve lies an appropriate distance
 * from the input generating it. 
 * 
 * @author mbdavis
 *
 */
public class BufferDistanceValidator 
{
	private static final double MAX_DISTANCE_DIFF_FRAC = .01;
	
  private Geometry input;
  private double distance;
  private Geometry result;
  
  private double minValidDistance;
  private double maxValidDistance;
  
  private boolean isValid = true;
  private Coordinate errorLocation = null;
  
  public BufferDistanceValidator(Geometry input, double distance, Geometry result)
  {
  	this.input = input;
  	this.distance = distance;
  	this.result = result;
  }
  
  public boolean isValid()
  {
  	double distDelta = MAX_DISTANCE_DIFF_FRAC * distance;
  	minValidDistance = distance - distDelta;
  	maxValidDistance = distance + distDelta;
  	
  	// can't use this test if either is empty
  	if (input.isEmpty() || result.isEmpty())
  		return true;
  	
  	if (distance > 0.0) {
  		checkPositiveValid();
  	}
  	else {
  		checkNegativeValid();
  	}
  	return isValid;
  }
  
  public Coordinate getErrorLocation()
  {
  	return errorLocation;
  }
  
  private void checkPositiveValid()
  {
  	Geometry bufCurve = result.getBoundary();
  	checkMinimumDistance(input, bufCurve, minValidDistance);
  	if (! isValid) return;
  	
  	checkMaximumDistance(input, bufCurve, maxValidDistance);
  }
  
  private void checkNegativeValid()
  {
  	// Assert: only polygonal inputs can be checked for negative buffers
  	
  	// MD - could generalize this to handle GCs too
  	if (! (input instanceof Polygon 
  			|| input instanceof MultiPolygon
  			|| input instanceof GeometryCollection
  			)) {
  		return;
  	}
  	Geometry inputCurve = getPolygonLines(input);
  	checkMinimumDistance(inputCurve, result, minValidDistance);
  	if (! isValid) return;
  	
  	checkMaximumDistance(inputCurve, result, maxValidDistance);
  }
  
  private Geometry getPolygonLines(Geometry g)
  {
  	List lines = new ArrayList();
  	LinearComponentExtracter lineExtracter = new LinearComponentExtracter(lines);
  	List polys = PolygonExtracter.getPolygons(g);
  	for (Iterator i = polys.iterator(); i.hasNext(); ) {
  		Polygon poly = (Polygon) i.next();
  		poly.apply(lineExtracter);
  	}
  	return g.getFactory().buildGeometry(lines);
  }
  
  /**
   * Checks that the buffer curve is at least minDist from the input curve.
   * 
   * @param inputCurve
   * @param buf
   * @param minDist
   */
  private void checkMinimumDistance(Geometry inputCurve, Geometry buf, double minDist)
  {
  	DistanceOp distOp = new DistanceOp(input, buf, minDist);
  	double dist = distOp.distance();
  	if (dist < minDist) {
  		isValid = false;
  		errorLocation = distOp.closestPoints()[1];
  	}
  }
  
  /**
   * Checks that the Hausdorff distance from g1 to g2
   * is less than the given maximum distance.
   * This uses the oriented Hausdorff distance measure; it corresponds to finding
   * the point on g1 which is furthest from <i>some</i> point on g2.
   * 
   * @param g1 a geometry
   * @param g2 a geometry
   * @param maxDist the maximum distance that a buffer result can be from the input
   */
  private void checkMaximumDistance(Geometry input, Geometry buf, double maxDist)
  {
  	// TODO: to be developed
  }
  
  
}
