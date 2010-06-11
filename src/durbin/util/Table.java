package durbin.util;

import java.util.*;
import java.io.*;
import java.lang.*;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
import cern.colt.list.*;

import groovy.lang.*;
import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.codehaus.groovy.runtime.*;

//import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.RangeInfo;

/**************************************************
* An iterator for a row or a column of a table
*/
class TableMatrix1DIterator implements Iterator{
  TableMatrix1D table;
  int idx = 0;
  
  TableMatrix1DIterator(TableMatrix1D t){
    table = t;
  }
  
  public boolean hasNext(){
    if (idx < table.size()) return(true);
    else return(false);
  }
  
  public Object next(){
    Object rval = table.get(idx);
    idx++;
    return(rval);
  }
  public void remove(){}
}

/****************************************************
* A row or a column in the table.   Specifically wrapper to make the 
* ObjectMatrix1D returned by matrix.viewColumn/Row 
* into something that is iterable, since ObjectMatrix1D doesn't
* implement iterable (probably because colt is old as dirt). 
* 
* KJD: I've gone off the deep end here... I'm sure this is not
* the Right Way to do this, although it works...
* all of this probably should have been handled  from the Table.groovy 
* wrapper, using some kind of expando magic or some such...
* 
* Ugh... even worse, all this hasn't helped performance compared to 
* getting a copy of the array for each row... 
*/
class TableMatrix1D extends DefaultGroovyMethodsSupport implements Iterable{
  
  ObjectMatrix1D data;
  
  public TableMatrix1D(ObjectMatrix1D dom){
    data = dom;
  }
  
  public int size(){ return(data.size()); }
  public Object get(int idx){ return(data.get(idx)); }  
  public void set(int idx,Object value){ data.set(idx,value); }
  
  public TableMatrix1DIterator iterator(){
    return(new TableMatrix1DIterator(this));
  }
  
  public Object getAt(int idx){
    if (idx < 0) idx = data.size()+idx; // 5 -1 = 4
    return(data.get(idx));
  }
    
  
  /****************************************************
  * Returns a view corresponding to the given range. 
  */ 
  public TableMatrix1D getAt(IntRange r){
    
    // KJD:  This range is coming in with from/to swapped, but why? 
    // HACK ALERT: Until I understand why, I'm just going to swap them 
    // back... so there...  
    IntRange r2 = new IntRange(r.getToInt(),r.getFromInt());
    
    // Convert Groovy relative range values (e.g. -2) into actual 
    // range numbers...
    RangeInfo ri = DefaultGroovyMethodsSupport.subListBorders(this.size(),r2);        
    int start = ri.from;
    int width = ri.to-start; 
    return(new TableMatrix1D(data.viewPart(start,width)));
  }  
}

/********************************************************************
* A 2D table of objects with methods to read from file, iterate over
* rows or columns, and support for Groovy closures.  A Table is a 
* 2D collection of cells.  Table cells can be accessed by index or by 
* name. 
*
********************************************************************/
public class Table extends GroovyObjectSupport{

	// An somewhat efficient place to store the data...
	public ObjectMatrix2D matrix;
	public String[] colNames;
	public String[] rowNames;
	public int numCols;
	public int numRows;
	
  public HashMap<String,Integer> colName2Idx = new HashMap<String,Integer>();
  public HashMap<String,Integer> rowName2Idx = new HashMap<String,Integer>();
  
  public Table(){}
  
  public Table(int rows,int cols){
    numRows = rows;
    numCols = cols;
    // Create an empty object matrix...
	  matrix = new DenseObjectMatrix2D(numRows,numCols);
  }  
  
  public Table(String fileName,String delimiter) throws Exception{
    readFile(fileName,delimiter);
  }
  
  
  public Table(String fileName) throws Exception{
    readFile(fileName,"\t");
  }
  
  public Table(String fileName,Closure c) throws Exception{
    readFile(fileName,"\t",c);
  }
  
  
  /**********************************
  * Create and read a table from a file, applying the closure to each cell 
  * in the table as it is read and before it is saved to the table (e.g. to 
  * parse out a substring of each cell, or convert to Double). 
  */ 
  public Table(String fileName,String delimiter,Closure c) throws Exception{
     readFile(fileName,delimiter,c);
   }
  
	public int rows() {
		return(numRows);
	}
	public int cols() {
		return(numCols);
	}
	
	/***************************************************************
  * Parse the column names from a line. 
  */ 
  public String[] parseColNames(String line,String regex){
    // Not quite right, because includes spurious 0,0 column. 
		String[] fields = line.split(regex,-1); // -1 to include empty cols.
		String[] colNames = new String[fields.length-1];
		
		//System.out.println("colNames.length="+colNames.length+" fields.lenght="+fields.length);
		
		for(int i = 1;i < fields.length;i++){
		  //System.out.println("i-1 : "+(i-1)+" i :"+i);
		  colNames[i-1] = (fields[i]).trim();
		}		
		return(colNames);
	}
	
	
	/***************************************************************
  * Convenience method to initialize a name2Idx map from an array of Strings
  * 
  */ 
	public static void createNameMap(String[] names,HashMap<String,Integer> name2IdxMap){	  
	  for(int i = 0;i < names.length;i++){
	    name2IdxMap.put(names[i],i);
	  }
	}

  /***************************************************************
  * Write table to a file
  */ 
	public void write(String fileName,String delimiter) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		write(out,delimiter);
		out.close();
	}

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
   			Object entry = matrix.getQuick(r,c);
   			sb.append(entry+delimiter);
   		}
   		Object entry = matrix.getQuick(r,(numCols-1));
      sb.append(entry+"\n");
   	}
   	return(sb.toString());		
   }


  /***************************************************************
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
				Object entry = matrix.getQuick(r,c);
        br.write(entry+delimiter);
			}
			Object entry = matrix.getQuick(r,(numCols-1));
      br.write(entry+"\n");
		}		
	}
	

	/**************************************
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

		System.err.print("Reading "+numRows+" x "+numCols+" table...");

		// Create an empty object matrix...
		matrix = new DenseObjectMatrix2D(numRows,numCols);

		// Populate the matrix with values...
		int rowIdx = 0;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(regex,-1);
			rowNames[rowIdx] = tokens[0].trim();
									      
      for(int colIdx = 0;colIdx < (tokens.length-1);colIdx++){
        matrix.setQuick(rowIdx,colIdx,tokens[colIdx+1]);                
      }     
			rowIdx++;
		}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
	
	public void readFile(String fileName,Closure c) throws Exception {
	  readFile(fileName,"\t",c);
	}
	
  /***************************************************************
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
		matrix = new DenseObjectMatrix2D(numRows,numCols);

    // Populate the matrix with values...
  	int rowIdx = 0;
  	while ((line = reader.readLine()) != null) {
  		String[] tokens = line.split(regex,-1);  		  		
  		rowNames[rowIdx] = tokens[0].trim();

      for(int colIdx = 0;colIdx < (tokens.length-1);colIdx++){
        matrix.setQuick(rowIdx,colIdx,c.call(tokens[colIdx+1]));                
      }     
  		rowIdx++;
  	}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
	
	

	public Object get(int row,int col) {
		return(matrix.getQuick(row,col));
	}
	
	public void set(int row,int col,Object data){
	  matrix.setQuick(row,col,data);
	}
	
	public void set(String rowStr,String colStr,Object data){
	  int row = getRowIdx(rowStr);
	  int col = getColIdx(colStr);
	  matrix.setQuick(row,col,data);
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
	
	
	public int getRowIdx(String row){return(rowName2Idx.get(row));}
	public int getColIdx(String col){return(colName2Idx.get(col));}
	
	/**********************************
	*
	*/
  public DoubleArrayList getRowAsDoubleArrayList(int row){
    DoubleArrayList dal = new DoubleArrayList();
    ObjectMatrix1D dm1D  = matrix.viewRow(row);
    for(int r = 0;r < dm1D.size();r++){
      dal.add((Double)dm1D.get(r));
    }
    return(dal);
  }
  
  /**********************************
	*
	*/
  public DoubleArrayList getColAsDoubleArrayList(int col){
    DoubleArrayList dal = new DoubleArrayList();
    ObjectMatrix1D dm1D  = matrix.viewColumn(col);
    for(int r = 0;r < dm1D.size();r++){
      dal.add((Double)dm1D.get(r));
    }
    return(dal);
  }
	
	public Object[] getRowAsArray(int row){
	  return(matrix.viewRow(row).toArray());
	}
		
	public Object[] getColAsArray(int col){
	  return(matrix.viewColumn(col).toArray());
	}
		
	public TableMatrix1D getRow(int row){
	   return(new TableMatrix1D(matrix.viewRow(row)));
	}
	
	public TableMatrix1D getAt(int ridx){
    return(getRow(ridx));
  }
		
  public TableMatrix1D getAt(String rowName){
    int ridx = getRowIdx(rowName);
    return(getRow(ridx));
  }
	
	
	public TableMatrix1D getCol(int col){
	   return(new TableMatrix1D(matrix.viewRow(col)));
	}

	/***********************************
	* Provide support for iterating over table by rows...
	*/
	public Table each(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			for (int c = 0;c < numCols;c++) {
				Object entry = matrix.getQuick(r,c);
				closure.call(new Object[] {r,c,entry});
			}
		}
		return this;
	}

	/***********************************
	* Provide support for iterating over table by rows...
	*/
	public Table eachByRows(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			for (int c = 0;c < numCols;c++) {
				Object entry = matrix.getQuick(r,c);
				closure.call(new Object[] {r,c,entry});
			}
		}
		return this;
	}


	/***********************************
	* Provide support for iterating over table by columns...
	*/
	public Table eachByCols(Closure closure) {
		for (int c = 0;c < numCols;c++) {
			for (int r = 0;r < numRows;r++) {
				Object entry = matrix.getQuick(r,c);
				closure.call(new Object[] {c,r,entry});
			}
		}
		return this;
	}

	/***********************************
	* Provide support for iterating over columns
	*
	* Note: I should be able to provide my own
	* column view object that supports iteration so
	* that I don't have to pay the cost of making a toArray
	* copy.
	*/
	public Table eachColumn(Closure closure) {
		for (int c = 0;c < numCols;c++) {
			//Object[] column = matrix.viewColumn(c).toArray();			
			TableMatrix1D column = new TableMatrix1D(matrix.viewColumn(c));			
			closure.call(new Object[] {column});
		}
		return this;
	}

	/***********************************
	* Provide support for iterating over rows
	*/
	public Table eachRow(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			//Object[] row = matrix.viewRow(r).toArray();  bit costly to make a copy...
			TableMatrix1D row = new TableMatrix1D(matrix.viewRow(r));
			closure.call(new Object[] {row});
		}
		return this;
	}
	
	/***************************************
	* Crude tests. 
	*/ 
	public static void main(String args[]) throws Exception {
	  Table tc = new Table();
	  tc.readFile(args[0],"\t");
	  System.gc();

	  Double onevalue = (Double) tc.matrix.getQuick(500,500);
	  System.err.println("One value: "+onevalue);
  }
  
  	
	/**************************************
	*  DEPRECATED SLOW (MAY TRY TO SPEED UP)
	*  Read a delimited table from a file.
	*
	*  Some attention has been paid to performance, since this is meant to be
	*  a core class.  Additional performance gains are no doubt possible.
	*/
	public void readFile0(String fileName,String regex) throws Exception {

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
		matrix = new DenseObjectMatrix2D(numRows,numCols);

    // Populate the matrix with values...
  	int rowIdx = 0;
  	while ((line = reader.readLine()) != null) {
  	  //		StringTokenizer parser = new StringTokenizer(line,"\t");
  	  Scanner sc = new Scanner(line).useDelimiter(regex);
  	  rowNames[rowIdx] = sc.next().trim();
  	  int colIdx = 0;
  	  while (sc.hasNext()) {
  	    String next = sc.next();
  		  matrix.setQuick(rowIdx,colIdx,sc.next());
  			colIdx++;
  		}
  	  rowIdx++;
  	}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
}


//Put split back in, because StringTokenizer doesn't handle missing values 
//easily. 
//time ./testscripts/tabletest.groovy data/chinSFGenomeBio2007_data_overlap.tab
//Reading 8566 x 220 table...done
//
//real	0m3.452s

// With Scanner (naive, new Scanner(line))
// time ./testscripts/tabletest.groovy data/chinSFGenomeBio2007_data_overlap.tab
// Reading 8566 x 220 table...done
// real	0m5.739s


// time ./scripts/tab2arff -d data/chinSFGenomeBio2007_data_overlap.tab > data.arff/test.arff
// Reading 8566 x 220 table...done
// Writing output...
// 
// real	1m6.706s
// 
// Damn.  No better than copying each slice [0..-2] as an array... so so far all of this
// fancy TableMatrixIterator stuff isn't doing me any good at all...
// 
// With ArffHelper time drops to 20.669s!
// With ArffHelper and a BufferedWriter, drops to 10-11 sec.   
//
// 


// Performance 220 x 8566 file
//
// line.split()                  2.07s
// w/o any split
// StringTokenizer               1.75s
// StringTokenizer via Groovy    3.25s
// StringTokenizer getBytes      2.79s


// ***NOTE MISTAKEN BAD PERFORMANCE RESULTS
// Below times are with compilethreshold set to 0 and via Groovy.  Apparently that really burns up the time.
// Performance 220 x 8566 file
//
// line.split()     23.47
// w/o any split     9.17  (implication... split consumes 14 seconds of 23 seconds)
// StringTokenizer  16.05  (implication... tokenizer consumes 7 seconds... so 2x fast as split)



// RAM Performance
//
// Theoretical:
// 8566 x 220 = 1,884,520 array elements.
//
//                         0123456789012345678
// Each string is about... -0.046845,-0.057836   20 characters long.
//
// So raw total space about: 37,690,400.
//
// Base memory for dense matrix:  8*rows()*columns() = 15,076,160
//
// Total memory with no funny business:  52,766,560.
//
// Final heap size from top:
//
// PID COMMAND      %CPU   TIME   #TH #PRTS #MREGS RPRVT  RSHRD  RSIZE  VSIZE
// 10036 java        98.9%  0:29.19  13   115    371  330M  6116K   336M  1006M
//
// Apparent memory (336M?):  6x this amount.
//
// A length 10 string, BTW, takes 56 bytes!!  Not sure what a 20 character string
// takes, but at 10 characters we're 5.6x over the character count, which is
// enough to account for the added space.
//



