package com.vividsolutions.jts.operation.buffer.validate;

import com.vividsolutions.jts.geom.*;

/**
 * Validates that the result of a buffer operation
 * is geometrically correct, within a computed tolerance.
 * <p>
 * This is a heuristic test, and may return false positive results
 * (I.e. it may fail to detect an invalid result.)
 * It should never return a false negative result, however
 * (I.e. it should never report a valid result as invalid.)
 * <p>
 * This test may be (much) more expensive than the original
 * buffer computation.
 *
 * @author Martin Davis
 */
public class BufferResultValidator 
{
  public static boolean isValid(Geometry g, double distance, Geometry result)
  {
  	BufferResultValidator validator = new BufferResultValidator(g, distance, result);
    return validator.isValid();
  }

  private Geometry input;
  private double distance;
  private Geometry result;
  private boolean isValid = true;
  private String errorMsg = null;
  private Coordinate errorLocation = null;
  
  public BufferResultValidator(Geometry input, double distance, Geometry result)
  {
  	this.input = input;
  	this.distance = distance;
  	this.result = result;
  }
  
  public boolean isValid()
  {
  	checkPolygonal();
  	if (! isValid) return isValid;
  	checkExpectedEmpty();
  	if (! isValid) return isValid;
  	checkEnvelope();
  	if (! isValid) return isValid;
  	checkArea();
  	if (! isValid) return isValid;
  	
  	checkDistance();
  	
  	return isValid;
  }
  
  public String getErrorMessage()
  {
  	return errorMsg;
  }
  
  public Coordinate getErrorLocation()
  {
  	return errorLocation;
  }
  
  private void checkPolygonal()
  {
  	if (! (result instanceof Polygon 
  			|| result instanceof MultiPolygon))
  	isValid = false;
  	errorMsg = "Result is not polygonal";
  }
  
  private void checkExpectedEmpty()
  {
  	// can't check areal features
  	if (input.getDimension() >= 2) return;
  	// can't check positive distances
  	if (distance > 0.0) return;
  		
  	// at this point can expect an empty result
  	if (! result.isEmpty()) {
  		isValid = false;
  		errorMsg = "Result is non-empty";
  	}
  }
  
  private void checkEnvelope()
  {
  	if (distance < 0.0) return;
  	
  	double padding = distance * 0.01;
  	if (padding == 0.0) padding = 0.001;

  	Envelope expectedEnv = new Envelope(input.getEnvelopeInternal());
  	expectedEnv.expandBy(distance);
  	
  	Envelope bufEnv = new Envelope(result.getEnvelopeInternal());
  	bufEnv.expandBy(padding);

  	if (! bufEnv.contains(expectedEnv)) {
  		isValid = false;
  		errorMsg = "Buffer envelope is incorrect";
  	}
  }
  
  private void checkArea()
  {
  	double inputArea = input.getArea();
  	double resultArea = result.getArea();
  	
  	if (distance > 0.0
  			&& inputArea > resultArea) {
  		isValid = false;
  		errorMsg = "Area of positive buffer is smaller than input";
  	}
  	if (distance < 0.0
  			&& inputArea < resultArea) {
  		isValid = false;
  		errorMsg = "Area of negative buffer is larger than input";
  	}
  }
  
  private void checkDistance()
  {
  	BufferDistanceValidator distValid = new BufferDistanceValidator(input, distance, result);
  	if (! distValid.isValid()) {
  		isValid = false;
  		errorMsg = "Buffer curve is incorrect distance from input";
  		errorLocation = distValid.getErrorLocation();
  	}
  }
}
