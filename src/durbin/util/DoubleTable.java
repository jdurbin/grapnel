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


/**
* A high performance 2D table of doubles with methods to read from file, 
* iterate over rows or columns, reorder rows or columns, transpose the table, 
* and support for Groovy closures.  A DoubleTable is a 2D collection of cells that
* can be accessed either by index or by name.  It is somewhat similar to an 
* R data-frame.  Currently it is backed by the Cern colt DenseDoubleMatrix2D 
* high performance matrix class.<br><br>
* 
*/
public class DoubleTable extends GroovyObjectSupport{

	public DenseDoubleMatrix2D matrix; // Backing storage. 
	public String[] colNames;   
	public String[] rowNames;
	public int numCols;
	public int numRows;
	
	public int colOffset = 1; // Default doesn't include first column in table. 
	boolean bFirstColInTable = false;
	
  public HashMap<String,Integer> colName2Idx = new HashMap<String,Integer>();
  public HashMap<String,Integer> rowName2Idx = new HashMap<String,Integer>();
  
	/**
	* Create an empty DoubleTable
	*/
  public DoubleTable(){  	
  }
	
	/**
	* Create a DoubleTable from a 2D array with no row/column names. 
	*/
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
		
	/**
	* Create with a list of row and column names and a double 2D array. 
	*/
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
	
	/**
	* Create an empty table of size given by list of row and column names. 
	*/
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
  
	/**
	* Create an empty table of given size. 
	*/
  public DoubleTable(int rows,int cols){
    numRows = rows;
    numCols = cols;
    // Create an empty object matrix...
	  matrix = new DenseDoubleMatrix2D(numRows,numCols);
  }  
  
	/**
	* Create a new table by reading from given file with default tab delimiter. 
	*/  
  public DoubleTable(String fileName) throws Exception{
    readFile(fileName,"\t");
		
  }

	/**
	* Create a new table by reading from the given csv/tab file with specified delimiter
	*/
  public DoubleTable(String fileName,String delimiter) throws Exception{
    readFile(fileName,delimiter);
		
  }

	/**
	* The first row will always populate the rowNames, but sometimes we want
	* to put the first col in the table itself (e.g. when stuffing into JTable)
	*/
 	public DoubleTable(String fileName,boolean bFirstColInTable) throws Exception{
		setFirstColInTable(bFirstColInTable);
    readFile(fileName,"\t");
  }
      
  /**
  * Create and read a table from a file, applying the closure to each cell 
  * in the table as it is read and before it is saved to the table (e.g. to 
  * parse out a substring of each cell, or convert to Double). 
  */ 
	public DoubleTable(String fileName,String delimiter,Closure c) throws Exception{
     readFile(fileName,delimiter,c);
		 
  }
	

	/**
	* Create and read a table from a file, applying the closure to each cell 
	* before it is saved to the table. 
	*/
  public DoubleTable(String fileName,Closure c) throws Exception{
    readFile(fileName,"\t",c);
		
  }
		
		
	/**
	*	The first row will always populate the rowNames, but sometimes we want
	* to put the first col in the table itself (e.g. when stuffing into JTable)
	*/
	public void setFirstColInTable(boolean firstColInTable){		
		bFirstColInTable= firstColInTable;
			if(bFirstColInTable){
			colOffset = 0;			
		}else{
			colOffset = 1;
		}
	}

	/**
	* Convert matrix to a double array. 
	*/ 
	public double[][] toArray(){
		return(matrix.toArray());
	}
		
		
	/**
	* Compares this table with another to see if they match within epsilon
	*/
	public boolean equalsTable(DoubleTable t2,Double epsilon){
		boolean bMismatch = false;
		for(int r = 0;r < rows();r++){
			for(int c = 0;c < cols();c++){
				Double v1 = matrix.getQuick(r,c);
				Double v2 = t2.matrix.getQuick(r,c);
		
				Double delta = Math.abs(v1 - v2);
				if (delta > epsilon) bMismatch = true;
			}
		}
		return(!bMismatch);
	}
	
	/**
	* Returns true if the table is symmetric to within epsilon
	*/
	public boolean isSymmetric(Double epsilon){
		
		if (rows() != cols()) return(false);
		
		for(int i = 0;i<rows();i++){
			for(int j = 0;j < cols();j++){
				Double a = matrix.getQuick(i,j);
				Double b = matrix.getQuick(j,i);
				Double delta = Math.abs(a-b);
				if (delta > epsilon) return(false);
			}			
		}
		return(true);
	}
	
	
	/**
	* Add a row to the table 
	*/ 
	public DoubleTable addRow(Collection<Double> row,int rowIdx,String rowName){
		DoubleTable newt = new DoubleTable(rows()+1,cols());
		newt.colNames = colNames.clone();
		newt.rowNames = new String[rows()+1];
		int offset = 0;
		for(int r =0;r < rows()+1;r++){			
			if (r == rowIdx){
				newt.rowNames[r] = rowName;
				int colIdx =0;
				for(Double val : row){
					newt.matrix.setQuick(r,colIdx++,val);
				}				
				offset = 1; // After inserted row, need to shift index by one
			}else{
				int oldrow = r - offset;
				newt.rowNames[r] = rowNames[oldrow];
				for(int c = 0;c < cols();c++){
					newt.matrix.setQuick(r,c,matrix.getQuick(oldrow,c));			
				}
			}
		}
		return(newt);		
	}	
		
	/**
	* Add a column to this table. 
	*/ 
	public DoubleTable addCol(Collection<Double> col,int colIdx,String colName){
		DoubleTable newt = new DoubleTable(rows(),cols()+1);
		newt.rowNames = rowNames.clone();
		newt.colNames = new String[cols()+1];
		int offset = 0;
		for(int c =0;c < cols()+1;c++){			
			if (c == colIdx){
				newt.colNames[c] = colName;
				int rowIdx =0;
				for(Double val : col){
					newt.matrix.setQuick(rowIdx++,c,val);
				}				
				offset = 1; // After inserted row, need to shift index by one
			}else{
				int oldcol = c - offset;
				newt.colNames[c] = colNames[oldcol];
				for(int r = 0;r < rows();r++){
					newt.matrix.setQuick(r,c,matrix.getQuick(r,oldcol));			
				}
			}
		}
		return(newt);		
	}	
	
	/**
	* Return a transposed copy of this table. 
	*/ 
	public DoubleTable transpose(){
		DoubleTable ttable = new DoubleTable(this.numCols,this.numRows);
		ttable.rowNames = colNames.clone();
		ttable.colNames = rowNames.clone();
		DoubleMatrix2D diceView = matrix.viewDice();
		ttable.matrix = (DenseDoubleMatrix2D) diceView.copy();
		return(ttable);
	}	
	
	/**
	* Returns a new DoubleTable with rows reordered according to the given list.
	*/
	public DoubleTable orderRowsBy(List newOrder){			
		DoubleTable newt = new DoubleTable(rows(),cols());
		newt.colNames = colNames.clone();
		newt.rowNames = new String[rows()];
		for(int r =0;r < rows();r++){
			int oldr = ((int)newOrder.get(r));
			newt.rowNames[r] = rowNames[oldr];
			for(int c = 0;c < cols();c++){
				newt.matrix.setQuick(r,c,matrix.getQuick(oldr,c));			
			}
		}
		return(newt);
	}
	
	/**
	* Returns a new DoubleTable with columns reordered according to the given list. 
	*/
	public DoubleTable orderColumnsBy(List newOrder){			
		DoubleTable newt = new DoubleTable(rows(),cols());
		newt.rowNames = rowNames.clone();
		newt.colNames = new String[cols()];
		for(int c =0;c < cols();c++){
			int oldc = ((int)newOrder.get(c));
			newt.colNames[c] = colNames[oldc];
			for(int r = 0;r < rows();r++){
				newt.matrix.setQuick(r,c,matrix.getQuick(r,oldc));			
			}
		}
		return(newt);
	}
	
	/**
	* Number of columns in table
	*/
	public int cols() {
		
		return(numCols);
		
	}
	
  /**
	* Number of rows in table
	*/
	public int rows() {
		return(numRows);	
			
	}
	
	/**
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
	
	
	/**
  * Convenience method to initialize a name2Idx map from an array of Strings
  */ 
	public static void createNameMap(String[] names,HashMap<String,Integer> name2IdxMap){	  
	  for(int i = 0;i < names.length;i++){
	    name2IdxMap.put(names[i],i);
	  }
	}

  /**
  * Write table to a file
  */ 
	public void write(String fileName,String delimiter) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		write(out,delimiter);
		out.close();
	}

	/**
	* Return a string representation of the table. 
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
			
			// Don't tack on \n to last row...
			if (r == (numRows-1)) sb.append(entry);
			else sb.append(entry+"\n");
   	}
   	return(sb.toString());		
	}


  /**
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
	

	/**
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
	

	
  /**
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
	
	/**
	*  Read a delimited table from a file.
	*
	*  Some attention has been paid to performance, since this is meant to be
	*  a core class.  Additional performance gains are no doubt possible.
	*/
	public void readWithoutHeadings(String fileName,String regex) throws Exception {
		colOffset = 0;
		numRows = FileUtils.fastCountLines(fileName); // no heading so count all lines
		rowNames = new String[numRows];
		for(int i = 0;i< numRows;i++){
			rowNames[i] = "Row"+i;
		}

		// Read the col headings and figure out the number of columns in the table..
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = reader.readLine();
		colNames = parseColNames(line,regex);
		numCols = colNames.length;
		colNames = new String[numCols];
		for(int i = 0;i < numCols;i++){
			colNames[i] = "Col"+i;
		}
		createNameMap(colNames,colName2Idx);
		numCols = colNames.length; 

		System.err.print("Reading "+numRows+" x "+numCols+" table...");

		// Create an empty object matrix...
		matrix = new DenseDoubleMatrix2D(numRows,numCols);

		// Reopen the file...
		reader = new BufferedReader(new FileReader(fileName));

		// Populate the matrix with values...
		int rowIdx = 0;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(regex,-1);
			rowNames[rowIdx] = "Row "+rowIdx;
									      
      for(int colIdx = 0;colIdx < (tokens.length-colOffset);colIdx++){
				//System.err.println("rowIdx:"+rowIdx+" colIdx:"+colIdx+" colOffset:"+colOffset+" tokens.length:"+tokens.length);
        matrix.setQuick(rowIdx,colIdx,Double.parseDouble(tokens[colIdx+colOffset]));                
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

	
	/***************************************************
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




//import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.RangeInfo;

// KJD GroovyLab and the Matrix package might be mature enough that this should be 
// backed with that.  Basically end up having a Table as a frame for a Matrix like in R. 
// http://code.google.com/p/groovylab/



	/**
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


	/**
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


	/**
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
			

	/**
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
	
	/**
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
	
