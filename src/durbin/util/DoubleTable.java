package durbin.util;

import java.util.*;
import java.io.*;
import java.lang.*;


import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
import cern.colt.list.*;
import cern.colt.matrix.impl.AbstractMatrix2D;

import groovy.lang.*;
import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.codehaus.groovy.runtime.*;

//import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.RangeInfo;

// KJD GroovyLab and the Matrix package might be mature enough that this should be 
// backed with that.  Basically end up having a Table as a frame for a Matrix like in R. 
// http://code.google.com/p/groovylab/


/***
* A 2D table of objects with methods to read from file, iterate over
* rows or columns, and support for Groovy closures.  A DoubleTable is a 
* 2D collection of cells.  DoubleTable cells can be accessed by index or by 
* name.<br><br>
* 
* Note:  I intended for this to be a high performance 2D table that could
* be accessed by row/column index or by row/column name.  However, in practice
* I am using Multidimensional map for most things now, with adequate performance. 
* This is still used in a few places, though. 
*/
public class DoubleTable extends GroovyObjectSupport{

	// An somewhat efficient place to store the data...
	public DenseDoubleMatrix2D matrix;
	public String[] colNames;  // Groovy getters/setters only gen for non-public variables. 
	public String[] rowNames;
	public int numCols;
	public int numRows;
	
	public int colOffset = 1; // Default doesn't include first column in table. 
	boolean bFirstColInTable = false;
	
  public HashMap<String,Integer> colName2Idx = new HashMap<String,Integer>();
  public HashMap<String,Integer> rowName2Idx = new HashMap<String,Integer>();
  
  public DoubleTable(){  	
  }
	
	public DoubleTable(double[][] dataMatrix){			
		// Generate generic default names if none given...
		numRows = dataMatrix.length;
		numCols = dataMatrix[0].length;
		rowNames = new String[numRows];
		colNames = new String[numCols];
		for(int i = 0;i < numRows;i++){
			rowNames[i] = "Row"+i;
		}
			
		for(int i = 0;i < numCols;i++){
			colNames[i] = "Col"+i;
		}								
			
		matrix = new DenseDoubleMatrix2D(dataMatrix);
		createNameMap(colNames,colName2Idx);			
		createNameMap(rowNames,rowName2Idx);	
	}
		
	
	public DoubleTable(double[][] dataMatrix,ArrayList<String> rNames,ArrayList<String> cNames){
		numRows = rNames.size();
		numCols = cNames.size();
		matrix = new DenseDoubleMatrix2D(dataMatrix);

		rowNames = new String[numRows];
		for(int i = 0;i < rNames.size();i++){
			rowNames[i] = rNames.get(i);
		}
		colNames = new String[numCols];
		for(int i = 0;i < cNames.size();i++){
			colNames[i] = cNames.get(i);
		}

		createNameMap(colNames,colName2Idx);
		createNameMap(rowNames,rowName2Idx);
	}
	
	public DoubleTable(ArrayList<String> rNames,ArrayList<String> cNames){
		numRows = rNames.size();
		numCols = cNames.size();
		matrix = new DenseDoubleMatrix2D(numRows,numCols);
		
		rowNames = new String[numRows];
		for(int i = 0;i < rNames.size();i++){
			rowNames[i] = rNames.get(i);
		}
		
		colNames = new String[numCols];
		for(int i = 0;i < cNames.size();i++){
			colNames[i] = cNames.get(i);
		}
		
		createNameMap(colNames,colName2Idx);
		createNameMap(rowNames,rowName2Idx);
	}
  
  public DoubleTable(int rows,int cols){
    numRows = rows;
    numCols = cols;
    // Create an empty object matrix...
	  matrix = new DenseDoubleMatrix2D(numRows,numCols);
  }  
  
  public DoubleTable(String fileName,String delimiter) throws Exception{
    readFile(fileName,delimiter);
  }
    
  public DoubleTable(String fileName) throws Exception{
    readFile(fileName,"\t");
  }

 	public DoubleTable(String fileName,boolean bFirstColInTable) throws Exception{
		setFirstColInTable(bFirstColInTable);
    readFile(fileName,"\t");
  }
  
  public DoubleTable(String fileName,Closure c) throws Exception{
    readFile(fileName,"\t",c);
  }

	public double[][] toArray(){
		return(matrix.toArray());
	}


	public void setFirstColInTable(boolean firstColInTable){		
		bFirstColInTable= firstColInTable;
		// The first row will always populate the rowNames, but sometimes we want
		// to put the first row in the table itself (e.g. when stuffing into JTable)
		if(bFirstColInTable){
			colOffset = 0;			
		}else{
			colOffset = 1;
		}
	}
    
  /***
  * Create and read a table from a file, applying the closure to each cell 
  * in the table as it is read and before it is saved to the table (e.g. to 
  * parse out a substring of each cell, or convert to Double). 
  */ 
	public DoubleTable(String fileName,String delimiter,Closure c) throws Exception{
     readFile(fileName,delimiter,c);
  }
  
	public int rows() {
		return(numRows);
	}
	public int cols() {
		return(numCols);
	}
	
	/***
  * Parse the column names from a line. 
  */ 
  public String[] parseColNames(String line,String regex){
    // Not quite right, because includes spurious 0,0 column. 
		String[] fields = line.split(regex,-1); // -1 to include empty cols.
		String[] colNames = new String[fields.length-colOffset];
		
		//System.err.println("colNames.length="+colNames.length+"fields.length="+fields.length);
		//System.err.println("fields:");
		//for(int i = 0;i < fields.length;i++){
		//	System.err.println("\tfields ["+i+"]="+fields[i]);
		//}
		
		for(int i = colOffset;i < fields.length;i++){
		  //System.err.println("i-colOffset: "+(i-colOffset)+" i :"+i);
		  colNames[i-colOffset] = (fields[i]).trim();
		}		
		return(colNames);
	}
	
	
	/***
  * Convenience method to initialize a name2Idx map from an array of Strings
  * 
  */ 
	public static void createNameMap(String[] names,HashMap<String,Integer> name2IdxMap){	  
	  for(int i = 0;i < names.length;i++){
	    name2IdxMap.put(names[i],i);
	  }
	}

  /***
  * Write table to a file
  */ 
	public void write(String fileName,String delimiter) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		write(out,delimiter);
		out.close();
	}

	/***
	*
	*/ 
  public String toString(){
    String delimiter = "\t";
    StringBuilder sb = new StringBuilder();
    // Write first line of column names...
    sb.append("feature_name\t");
   	for(int c = 0;c < (numCols-1);c++){
   	  sb.append(colNames[c]+delimiter);
   	}
   	sb.append(colNames[numCols-1]);
   	sb.append("\n");

   	for (int r = 0;r < numRows;r++) {
   	  // First column of each line is a row name...
   	  sb.append(rowNames[r]+delimiter);
   		for (int c = 0;c < (numCols -1);c++) {
   			Double entry = matrix.getQuick(r,c);
   			sb.append(entry+delimiter);
   		}
   		Double entry = matrix.getQuick(r,(numCols-1));
      sb.append(entry+"\n");
   	}
   	return(sb.toString());		
	}


  /***
  * Write table to a file
  */ 
	public void write(BufferedWriter br,String delimiter) throws Exception{	  	  
	  // Write first line of column names...
	  br.write("rowName"+delimiter);
	  for(int c = 0;c < (numCols-1);c++){
	    String str = colNames[c]+delimiter;
	    br.write(str);
	  }
	  br.write(colNames[numCols-1]+"\n");
	  	  
		for (int r = 0;r < numRows;r++) {
		  // First column of each line is a row name...
		  br.write(rowNames[r]+delimiter);
			for (int c = 0;c < (numCols -1);c++) {
				Double entry = matrix.getQuick(r,c);
        br.write(entry+delimiter);
			}
			Double entry = matrix.getQuick(r,(numCols-1));
      br.write(entry+"\n");
		}		
	}
	

	/***
	*  Read a delimited table from a file.
	*
	*  Some attention has been paid to performance, since this is meant to be
	*  a core class.  Additional performance gains are no doubt possible.
	*/
	public void readFile(String fileName,String regex) throws Exception {

		numRows = FileUtils.fastCountLines(fileName) -1; // -1 exclude heading.
		rowNames = new String[numRows];

		// Read the col headings and figure out the number of columns in the table..
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = reader.readLine();

		colNames = parseColNames(line,regex);
		createNameMap(colNames,colName2Idx);
		numCols = colNames.length; 

		//System.err.print("Reading "+numRows+" x "+numCols+" table...");

		// Create an empty object matrix...
		matrix = new DenseDoubleMatrix2D(numRows,numCols);

		// Populate the matrix with values...
		int rowIdx = 0;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(regex,-1);
			if (bFirstColInTable){
				rowNames[rowIdx] = "Row "+rowIdx;
			}else{
				rowNames[rowIdx] = tokens[0].trim();
			}
									      
      for(int colIdx = 0;colIdx < (tokens.length-colOffset);colIdx++){
				//System.err.println("colIdx:"+colIdx+" tokens: "+tokens[colIdx+1]);				
        matrix.setQuick(rowIdx,colIdx,Double.parseDouble(tokens[colIdx+colOffset]));                
      }     
			rowIdx++;
		}
		createNameMap(rowNames,rowName2Idx);
		//System.err.println("done");
	}
	
	public void readFile(String fileName,Closure c) throws Exception {
	  readFile(fileName,"\t",c);
	}
	

	
  /***
	*  Read a delimited table from a file.
	*  Same as other readFile, except this one accepts a closure 
	*  to apply to each value before storing it in the matrix.
	*/
	public void readFile(String fileName,String regex,Closure c) throws Exception {

		numRows = FileUtils.fastCountLines(fileName) -1; // -1 exclude heading.
		rowNames = new String[numRows];

		// Read the col headings and figure out the number of columns in the table..
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = reader.readLine();
		colNames = parseColNames(line,regex);
		createNameMap(colNames,colName2Idx);
		numCols = colNames.length;

		System.err.print("Reading "+numRows+" x "+numCols+" table...");

		// Create an empty object matrix...
		matrix = new DenseDoubleMatrix2D(numRows,numCols);

    // Populate the matrix with values...
  	int rowIdx = 0;
  	while ((line = reader.readLine()) != null) {
  		String[] tokens = line.split(regex,-1);  
			if (bFirstColInTable){
				rowNames[rowIdx] = "Row "+rowIdx;
			}else{
				rowNames[rowIdx] = tokens[0].trim();
			}		  		

      for(int colIdx = 0;colIdx < (tokens.length-colOffset);colIdx++){
        matrix.setQuick(rowIdx,colIdx,(Double) c.call(Double.parseDouble(tokens[colIdx+colOffset])));                
      }     
  		rowIdx++;
  	}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
	
	public DenseDoubleMatrix2D getMatrix(){
		return(matrix);
	}

	public Double get(int row,int col) {
		return(matrix.getQuick(row,col));
	}
	
	public void set(int row,int col,Double data){
	  matrix.setQuick(row,col,data);
	}
	
	public void set(String rowStr,String colStr,Double data){
	  int row = getRowIdx(rowStr);
	  int col = getColIdx(colStr);
	  matrix.setQuick(row,col,data);
	}
	
	
	public int getRowIdx(String row){return(rowName2Idx.get(row));}
	public int getColIdx(String col){return(colName2Idx.get(col));}
	
	/***
	*
	*/
  public DoubleArrayList getRowAsDoubleArrayList(int row){
    DoubleArrayList dal = new DoubleArrayList();
    DenseDoubleMatrix1D dm1D  = (DenseDoubleMatrix1D) matrix.viewRow(row);
    for(int r = 0;r < dm1D.size();r++){
      dal.add((Double)dm1D.get(r));
    }
    return(dal);
  }
  
	
	public double[] getRowAsDoubleArray(int row){
	  return(matrix.viewRow(row).toArray());
	}
		
	public double[] getColAsDoubleArray(int col){
	  return(matrix.viewColumn(col).toArray());
	}
		

	public DoubleVector getAt(int ridx){
    return(getRow(ridx));
  }
		
  public DoubleVector getAt(String rowName){
		//System.err.println("rowName: "+rowName);
	
    int ridx = getRowIdx(rowName);
    return(getRow(ridx));
  }

	
	// KJD This is part of a change that isn't done...
	//public DoubleVector getAt(IntRange r){ 
		
	//}

	
	/****************************************************
  * Returns a view corresponding to the given range. 
  */ 
/*
  public DoubleVector getAt(IntRange r){       
		int from = r.getFromInt();
		int to = r.getToInt();
		// when one value is negative, to and from will be reversed...
		if (from < 0) {
			int oldto = to;
			to =(int) data.size()+from; // 5 -1 = 4
			from = oldto;						
			if (from < 0){
				from = (int)data.size()+from;
			}
		}
    int start = from;
    int width = to-start+1; 
    return(new DoubleVector(data.viewPart(start,width)));
  }*/
	
	
	public DoubleVector getRow(int row){
			// When return a row, want to have names for columns within the row...
	   return(new DoubleVector(matrix.viewRow(row),colNames,colName2Idx));
	}
	
	public DoubleVector getCol(int col){
	   return(new DoubleVector(matrix.viewColumn(col),rowNames,rowName2Idx));
	}
	
	public DoubleVector getCol(String colStr){
		int col = getColIdx(colStr);
		return(new DoubleVector(matrix.viewColumn(col),rowNames,rowName2Idx));
	}
	
	
	
	
	public DoubleVector getRow(String rowStr){
		int row = getRowIdx(rowStr);
		return(new DoubleVector(matrix.viewRow(row),colNames,colName2Idx));
	}
	
	
	
	//==========================================================
	
	public boolean containsCol(String colName){
		return(colName2Idx.keySet().contains(colName));
	}
	
	public boolean containsRow(String rowName){
		return(rowName2Idx.keySet().contains(rowName));
	}
	
	public List<Integer> getRowIndicesContaining(String substring){
	  ArrayList<Integer> rvals = new ArrayList<Integer>();
	  for(int r = 0;r < numRows;r++){
	    if (rowNames[r].contains(substring)){
	      rvals.add(r);
	    }
	  }
	  return(rvals);
	}
	
	public List<Integer> getColIndicesContaining(String substring){
	  ArrayList<Integer> rvals = new ArrayList<Integer>();
	  for(int c = 0;c < numCols;c++){
	    if (colNames[c].contains(substring)){
	      rvals.add(c);
	    }
	  }
	  return(rvals);
	}
	
}




	/***
	* Provide support for iterating over table by rows...
	public DoubleTable each(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			for (int c = 0;c < numCols;c++) {
				Double entry = matrix.getQuick(r,c);
				closure.call(new Double[] {r,c,entry});
			}
		}
		return this;
	}


	/***
	* Provide support for iterating over table by rows...

	public DoubleTable eachByRows(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			for (int c = 0;c < numCols;c++) {
				Double entry = matrix.getQuick(r,c);
				closure.call(new Double[] {r,c,entry});
			}
		}
		return this;
	}
		*/


	/***
	* Provide support for iterating over table by columns...
	public DoubleTable eachByCols(Closure closure) {
		for (int c = 0;c < numCols;c++) {
			for (int r = 0;r < numRows;r++) {
				Double entry = matrix.getQuick(r,c);
				closure.call(new Double[] {c,r,entry});
			}
		}
		return this;
	}
		*/
			

	/***
	* Provide support for iterating over columns
	*
	* Note: I should be able to provide my own
	* column view object that supports iteration so
	* that I don't have to pay the cost of making a toArray
	* copy.

	public DoubleTable eachColumn(Closure closure) {
		for (int c = 0;c < numCols;c++) {
			//Double[] column = matrix.viewColumn(c).toArray();			
			Vector column = new Vector(matrix.viewColumn(c));			
			closure.call(new Double[] {column});
		}
		return this;
	}
	*/
	
	/***
	* Provide support for iterating over rows

	public DoubleTable eachRow(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			//Double[] row = matrix.viewRow(r).toArray();  bit costly to make a copy...
			Vector row = new Vector(matrix.viewRow(r));
			closure.call(new Double[] {row});
		}
		return this;
	}
		*/
	
