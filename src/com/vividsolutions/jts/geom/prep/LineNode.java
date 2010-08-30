package com.vividsolutions.jts.geom.prep;



import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.noding.SegmentNode;

public class LineNode 
{
	public static final int FORWARD = 0;
	public static final int BACKWARD = 1;
	
	private SegmentNode node;
	private int[] location = new int[] { Location.EXTERIOR, Location.EXTERIOR };
	
  public LineNode(SegmentNode node, int locForward, int locBackward)
  {
  	this.node = node;
  	location[FORWARD] = locForward;
  	location[BACKWARD] = locBackward;
  }

  public void mergeLabel(int locForward, int locBackward)
  {
  	location[FORWARD] = mergeLocation(location[FORWARD], locForward);
  	location[BACKWARD] = mergeLocation(location[BACKWARD], locForward);
  }
  
  private static int mergeLocation(int loc1, int loc2)
  {
  	int mergeLoc = loc1;
  	if (loc2 == Location.INTERIOR) {
  		mergeLoc = Location.INTERIOR;
  	}
  	return mergeLoc;
  }
  
  public int[] getLocations() 
  {
  	return location;
  }
  
  public int getLocation(int position)
  {
  	return location[position];
  }
  
  public SegmentNode getNode()
  { return node; }
  
}
