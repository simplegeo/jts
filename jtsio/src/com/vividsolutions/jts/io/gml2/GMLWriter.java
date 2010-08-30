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
package com.vividsolutions.jts.io.gml2;

import java.io.*;

import com.vividsolutions.jts.geom.*;

/**
 * Writes JTS Geometries as GML2 into the writer provided, or as a string.
 *
 * @author David Zwiers, Vivid Solutions. 
 */
public class GMLWriter {
	private final String INDENT = "  ";
	
	private int startingIndentIndex = 0;
	private int maxCoordinatesPerLine = 2;
	
	private String prefix = GMLConstants.GML_PREFIX;
	
	/**
	 * Allows the user to force a prefix for the GML namespace. 
	 * 
	 * In XML blobs, the user may wish to leave the polygons un-qualified, thus setting the prefix to the empty string
	 * 
	 * @param prefix
	 */
	public void setPrefix(String prefix){
		this.prefix =prefix;
	}
	
	/**
	 * Sets the starting index for preaty printing
	 * 
	 * @param arg
	 */
	public void setStartingIndentIndex(int arg){
		if(arg<0)
			throw new IndexOutOfBoundsException("In-valid index, must be > or = 0");
		startingIndentIndex = arg;
	}
	
	/**
	 * Sets the number of coordinates printed per line. 
	 * 
	 * Use full when configuring preaty printing.
	 * 
	 * @param arg
	 */
	public void setMaxCoordinatesPerLine(int arg){
		if(arg<1)
			throw new IndexOutOfBoundsException("In-valid coordinate count per line, must be > 0");
		maxCoordinatesPerLine = arg;
	}
	
	/**
	 * @param geom
	 * @return String GML2 Encoded Geometry
	 * @throws IOException 
	 */
	public String write(Geometry geom) throws IOException{
		StringWriter writer = new StringWriter();
		write(geom,writer);
		return writer.getBuffer().toString();
	}
	
	/**
	 * Writes the JTS Geometry provided as GML2 into the writer provided.
	 * 
	 * @param geom Geometry to encode
	 * @param writer Stream to encode to.
	 * @throws IOException 
	 */
	public void write(Geometry geom, Writer writer) throws IOException{
		write(geom,writer,startingIndentIndex);
	}

	private void write(Geometry geom, Writer writer, int level) throws IOException{
		if(writer == null)
	           throw new NullPointerException("Writer is null");
		if (geom == null) {
           throw new NullPointerException("Geometry is null");
        } else if (geom instanceof Point) {
        	writePoint((Point)geom,writer,level);
        } else if (geom instanceof LineString) {
        	writeLineString((LineString)geom,writer,level);
        } else if (geom instanceof Polygon) {
        	writePolygon((Polygon)geom,writer,level);
        } else if (geom instanceof MultiPoint) {
        	writeMultiPoint((MultiPoint)geom,writer,level);
        } else if (geom instanceof MultiLineString) {
        	writeMultiLineString((MultiLineString)geom,writer,level);
        } else if (geom instanceof MultiPolygon) {
        	writeMultiPolygon((MultiPolygon)geom,writer,level);
        } else if (geom instanceof GeometryCollection) {
        	writeGeometryCollection((GeometryCollection)geom,writer,startingIndentIndex);
        }else{
	        throw new IllegalArgumentException("Cannot encode JTS "
	            + geom.getGeometryType() + " as SDO_GTEMPLATE "
	            + "(Limitied to Point, Line, Polygon, GeometryCollection, MultiPoint,"
	            + " MultiLineString and MultiPolygon)");
        }
		writer.flush();
	}

	  //<gml:Point><gml:coordinates>1195156.78946687,382069.533723461</gml:coordinates></gml:Point>
	  private void writePoint(Point p, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_POINT,p,writer);
	      
	      write(new Coordinate[] { p.getCoordinate() },writer, level + 1);

	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_POINT,writer);
	  }

	  //<gml:LineString><gml:coordinates>1195123.37289257,381985.763974674 1195120.22369473,381964.660533343 1195118.14929823,381942.597718511</gml:coordinates></gml:LineString>
	  private void writeLineString(LineString ls, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_LINESTRING,ls,writer);
	      
	      write(ls.getCoordinates(),writer, level + 1);

	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_LINESTRING,writer);
	  }

	  //<gml:LinearRing><gml:coordinates>1226890.26761027,1466433.47430292 1226880.59239079,1466427.03208053...></coordinates></gml:LinearRing>
	  private void writeLinearRing(LinearRing lr, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_LINEARRING,lr,writer);
	      
	      write(lr.getCoordinates(),writer, level + 1);

	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_LINEARRING,writer);
	  }

	  private void writePolygon(Polygon p, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_POLYGON,p,writer);
	      

	      startLine(level+1,writer);
	      startGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS,null,writer);

		    writeLinearRing((LinearRing) p.getExteriorRing(), writer, level + 2);

	      startLine(level+1,writer);
	      endGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS,writer);
	      

	    for (int t = 0; t < p.getNumInteriorRing(); t++) {
		      startLine(level+1,writer);
		      startGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS,null,writer);

			    writeLinearRing((LinearRing) p.getInteriorRingN(t), writer, level + 2);

		      startLine(level+1,writer);
		      endGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS,writer);
	    }


	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_POLYGON,writer);
	  }

	  private void writeMultiPoint(MultiPoint mp, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_MULTI_POINT,mp,writer);
	      
		    for (int t = 0; t < mp.getNumGeometries(); t++) {
			  startLine(level+1,writer);
			  startGeomTag(GMLConstants.GML_POINT_MEMBER,null,writer);
			      
		      writePoint((Point) mp.getGeometryN(t), writer, level + 2);
		      
		      startLine(level+1,writer);
		      endGeomTag(GMLConstants.GML_POINT_MEMBER,writer);
		    }
	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_MULTI_POINT,writer);
	  }

	  private void writeMultiLineString(MultiLineString mls, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_MULTI_LINESTRING,mls,writer);
	      
		    for (int t = 0; t < mls.getNumGeometries(); t++) {
			  startLine(level+1,writer);
			  startGeomTag(GMLConstants.GML_LINESTRING_MEMBER,null,writer);
			      
		      writeLineString((LineString) mls.getGeometryN(t), writer, level + 2);
		      
		      startLine(level+1,writer);
		      endGeomTag(GMLConstants.GML_LINESTRING_MEMBER,writer);
		    }
	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_MULTI_LINESTRING,writer);
	  }

	  private void writeMultiPolygon(MultiPolygon mp, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_MULTI_POLYGON,mp,writer);
	      
		    for (int t = 0; t < mp.getNumGeometries(); t++) {
			  startLine(level+1,writer);
			  startGeomTag(GMLConstants.GML_POLYGON_MEMBER,null,writer);
			      
		      writePolygon((Polygon) mp.getGeometryN(t), writer, level + 2);
		      
		      startLine(level+1,writer);
		      endGeomTag(GMLConstants.GML_POLYGON_MEMBER,writer);
		    }
	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_MULTI_POLYGON,writer);
	  }

	  private void writeGeometryCollection(GeometryCollection gc, Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_MULTI_GEOMETRY,gc,writer);
	      
		    for (int t = 0; t < gc.getNumGeometries(); t++) {
			  startLine(level+1,writer);
			  startGeomTag(GMLConstants.GML_GEOMETRY_MEMBER,null,writer);
			      
		      write(gc.getGeometryN(t), writer, level + 2);
		      
		      startLine(level+1,writer);
		      endGeomTag(GMLConstants.GML_GEOMETRY_MEMBER,writer);
		    }
	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_MULTI_GEOMETRY,writer);
	  }

	  private static final String coordinateSeparator = ",";
	  private static final String tupleSeparator = " ";
	  
	  /**
	   * Takes a list of coordinates and converts it to GML.<br>
	   * 2d and 3d aware.
	   * 
	   * @param coords array of coordinates
	 * @throws IOException 
	   */
	  private void write(Coordinate[] coords,Writer writer, int level) throws IOException {
	      startLine(level,writer);
	      startGeomTag(GMLConstants.GML_COORDINATES,null,writer);
	      
	      int dim = 2;

	      if (coords.length > 0) {
	        if (!(Double.isNaN(coords[0].z)))
	          dim = 3;
	      }

	      boolean isNewLine = false;
	      for (int i = 0; i < coords.length; i++) {
	        if (isNewLine) {
	  	      startLine(level+1,writer);
	          isNewLine = false;
	        }
	        if (dim == 2) {
	        	writer.write(""+coords[i].x);
	        	writer.write(coordinateSeparator);
	        	writer.write(""+coords[i].y);
	        } else if (dim == 3) {
	        	writer.write(""+coords[i].x);
	        	writer.write(coordinateSeparator);
	        	writer.write(""+coords[i].y);
	        	writer.write(coordinateSeparator);
	        	writer.write(""+coords[i].z);
	        }
	        writer.write(tupleSeparator);

	        // break output lines to prevent them from getting too long
	        if ((i + 1) % maxCoordinatesPerLine == 0 && i < coords.length - 1) {
		      writer.write("\n");
	          isNewLine = true;
	        }
	      }
	      if(!isNewLine)
	    	  writer.write("\n");

	      startLine(level,writer);
	      endGeomTag(GMLConstants.GML_COORDINATES,writer);
	  }


	  private void startLine(int level, Writer writer) throws IOException
	  {
		  for(int i=0;i<level;i++)
			  writer.write(INDENT);
	  }

	  private void startGeomTag(String geometryName, Geometry g, Writer writer) throws IOException
	  {
		writer.write("<"+((prefix == null || "".equals(prefix))?"":prefix+":"));
		writer.write(geometryName);
	    printAttr(g,writer);
	    writer.write(">\n");
	  }

	  private void printAttr(Geometry geom, Writer writer) throws IOException
	  {
		  if(geom == null)
			  return;
		  writer.write(" "+GMLConstants.GML_ATTR_SRSNAME+"='");
		  writer.write(geom.getSRID()+"");
		  writer.write("'");
	  }

	  private void endGeomTag(String geometryName, Writer writer) throws IOException
	  {
		  writer.write("</"+((prefix == null || "".equals(prefix))?"":prefix+":"));
		  writer.write(geometryName);
		  writer.write(">\n");
	  }
}
