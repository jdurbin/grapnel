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
* A 2D table of objects with methods to read from file, iterate over
* rows or columns, and support for Groovy closures.  A Table is a 
* 2D collection of cells.  Table cells can be accessed by index or by 
* name.<br><br>
* 
*	Note: There is a specialized table, DoubleTable, that is a more efficient version
*	of table when the data is all numeric plus some added numeric functionality.   
* Eventually the api for Table should be made to match in some subset and made 
* into a default Table interface with two implementations: 
* <pre>
*	
*	Table ->  DoubleTable
*	          ObjectTable
*	</pre>
*/
public class Table extends GroovyObjectSupport{

	public ObjectMatrix2D matrix; // Backing storage. 
	public String[] colNames;
	public String[] rowNames;
	public int numCols;
	public int numRows;
	
	public int colOffset = 1; // Default doesn't include first column in table. 
	boolean bFirstColInTable = false;	
		
	public HashMap<String,Integer> colName2Idx = new HashMap<String,Integer>();
	public HashMap<String,Integer> rowName2Idx = new HashMap<String,Integer>();
  
	/**
		* Create empty Table
		*/
	public Table(){
	}
  
	/**
		* Create a Table from a DoubleTable
		*/
	public Table(DoubleTable t){
		numRows = t.rows();
		numCols = t.cols();
		matrix = new DenseObjectMatrix2D(numRows,numCols);
		colNames = t.colNames.clone();
		rowNames = t.rowNames.clone();
		for(int r = 0;r < numRows;r++){
			for(int c = 0;c < numCols;c++){
				matrix.setQuick(r,c,t.matrix.getQuick(r,c));
			}
		}
	}
	
	/**
		* Create an empty Table with the given dimensions. 
		*/
	public Table(int rows,int cols){
		numRows = rows;
		numCols = cols;
		// Create an empty object matrix...
		matrix = new DenseObjectMatrix2D(numRows,numCols);
	}  
  
	/**
		* Create a Table by reading a file delimited by delimiter
		*/
	public Table(String fileName,String delimiter) throws Exception{
		
		readFile(fileName,delimiter);
		
	}

	/**
		* The first row will always populate the rowNames, but sometimes we want to put the 
		* first col in the table itself (e.g. when stuffing into JTable)
		*/
	public Table(String fileName,String delimiter,boolean bFirstRowInTable) throws Exception{
		setFirstColInTable(bFirstRowInTable);
		readFile(fileName,delimiter);
	}

	/**
		* Create a new table by reading from given file with default tab delimiter.
		*/
	public Table(String fileName) throws Exception{
		
		readFile(fileName,"\t");
		
	}
  
	/**
		* Create and read a table from a file, applying the closure to each cell 
		* before it is saved to the table.
		*/
	public Table(String fileName,Closure c) throws Exception{
		
		readFile(fileName,"\t",c);
		
	}
  
  
	/**
		* Create and read a table from a file, applying the closure to each cell 
		* in the table as it is read and before it is saved to the table (e.g. to 
		* parse out a substring of each cell, or convert to Double). 
		*/ 
	public Table(String fileName,String delimiter,Closure c) throws Exception{
		
		readFile(fileName,delimiter,c);
		 
	}

	/**
		* Create an empty table of size given by list of row and column names.
		*/
	public Table(ArrayList<String> rNames,ArrayList<String> cNames){
		numRows = rNames.size();
		numCols = cNames.size();
		matrix = new DenseObjectMatrix2D(numRows,numCols);

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
		* Assigns every cell in the table to the given object.
		*/
	public void assign(Object o){
		
		matrix.assign(o);
		
	}
	
	
	/**
		* Add a row to this table.
		*/ 
	public Table addRow(Collection<Object> row,int rowIdx,String rowName){
		Table newt = new Table(rows()+1,cols());
		newt.colNames = colNames.clone();
		newt.rowNames = new String[rows()+1];
		int offset = 0;
		for(int r =0;r < rows()+1;r++){			
			if (r == rowIdx){
				newt.rowNames[r] = rowName;
				int colIdx =0;
				for(Object val : row){
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
		* Add a column to this table
		*/ 
	public Table addCol(Collection<Object> col,int colIdx,String colName){
		Table newt = new Table(rows(),cols()+1);
		newt.rowNames = rowNames.clone();
		newt.colNames = new String[cols()+1];
		int offset = 0;
		for(int c =0;c < cols()+1;c++){			
			if (c == colIdx){
				newt.colNames[c] = colName;
				int rowIdx =0;
				for(Object val : col){
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
		* Returns true if the table contains a column with the given column name. 
		*/
	public boolean containsCol(String colName){
		return(colName2Idx.keySet().contains(colName));
		
	}
	
	/**
		* Returns true if the table contains a row with the given row name. 
		*/
	public boolean containsRow(String rowName){
		return(rowName2Idx.keySet().contains(rowName));
		
	}
	
  
	/**
		* Returns number of rows in table
		*/
	public int rows() {
		
		return(numRows);
		
	}
	
	/**
		* Returns number of columns in table. 
		*/
	public int cols() {
		
		return(numCols);
		
	}
	
	/**
		* Set flag to indicate whether or not first column of a file should be in the 
		* table or used as row names. 
		*/
	public void setFirstColInTable(boolean firstColInTable){		
		// The first row will always populate the rowNames, but sometimes we want
		// to put the first row in the table itself (e.g. when stuffing into JTable)
		bFirstColInTable = firstColInTable;
		if(bFirstColInTable){
			colOffset = 0;			
		}else{
			colOffset = 1;
		}
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
		* Parse the column names from a line. 
		*/ 
	public String[] parseColNames(String line,String regex){
		// Not quite right, because includes spurious 0,0 column. 
		String[] fields = line.split(regex,-1); // -1 to include empty cols.
				
		String[] colNames = new String[fields.length - colOffset];
		
		for(int i = colOffset;i < fields.length;i++){
			//System.err.println("i-colOffset: "+(i-colOffset)+" i :"+i);
			colNames[i-colOffset] = (fields[i]).trim();
		}				
		return(colNames);
	}

	/**
		* Write Table to a file
		*/ 
	public void write(String fileName,String delimiter) throws Exception {
		
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		write(out,delimiter);
		out.close();
		
	}

	/**
		* Converts to a string representation of the table. 
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
				Object entry = matrix.getQuick(r,c);
				sb.append(entry+delimiter);
			}
			Object entry = matrix.getQuick(r,(numCols-1));
			// Don't tack on \n to last row...
			if (r == (numRows-1)) sb.append(entry);
			else sb.append(entry+"\n");
		}
		return(sb.toString());		
	}

	/**
		* Attempts to convert the object table to a DenseDoubleMatrix2D
		* Assumes cells are strings and interprets empty string, null, and NA
		* as 0 entries. 
		*/
	public DenseDoubleMatrix2D toDenseDoubleMatrix2D(){
		DenseDoubleMatrix2D t = new DenseDoubleMatrix2D(rows(),cols());
		for(int i =0;i < rows();i++){
			for(int j = 0; j< cols();j++){
				String val = (String) get(i,j);
				if ((val == "") || (val == "null") || (val == "NA")){  // limited missing value support
					val = "0.0"; // ??? has to be some number.. 
				}
				t.setQuick(i,j,Double.parseDouble(val));
			}
		}
		return(t);
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
				Object entry = matrix.getQuick(r,c);
				br.write(entry+delimiter);
			}
			Object entry = matrix.getQuick(r,(numCols-1));
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

		System.err.print("Reading "+numRows+" x "+numCols+" table...");

		// Create an empty object matrix...
		matrix = new DenseObjectMatrix2D(numRows,numCols);

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
				//System.err.println("rowIdx:"+rowIdx+" colIdx:"+colIdx+" colOffset:"+colOffset+" tokens.length:"+tokens.length);
				matrix.setQuick(rowIdx,colIdx,tokens[colIdx+colOffset]);                
			}     
			rowIdx++;
		}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
	
	/**
		*  Create a table from the given tab delimited file applying closure to 
		*  each element before assigning to table. 
		*/
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
		matrix = new DenseObjectMatrix2D(numRows,numCols);

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
				matrix.setQuick(rowIdx,colIdx,c.call(tokens[colIdx+colOffset]));                
			}     
			rowIdx++;
		}
		createNameMap(rowNames,rowName2Idx);
		System.err.println("done");
	}
	
	
	/**
		* Returns the object stored at indices (row,col)
		*/
	public Object get(int row,int col) {
		
		return(matrix.getQuick(row,col));
		
	}
	
	/**
		* Sets the cell specified by a row and column index to contain data. 
		*/
	public void set(int row,int col,Object data){
		
		matrix.setQuick(row,col,data);
		
	}
	
	/**
		* Sets the cell specified by a row and column name to contain data. 
		*/
	public void set(String rowStr,String colStr,Object data){
		int row = getRowIdx(rowStr);
		int col = getColIdx(colStr);
		matrix.setQuick(row,col,data);
	}
	
	/**
		* Returns a list of the indices of rows that contain the given substring
		* in the row name.
		*/
	public List<Integer> getRowIndicesContaining(String substring){
		ArrayList<Integer> rvals = new ArrayList<Integer>();
		for(int r = 0;r < numRows;r++){
			if (rowNames[r].contains(substring)){
				rvals.add(r);
			}
		}
		return(rvals);
	}
	
	/**
		* Returns a list of the indices of columns that contain the given substring
		* in the column name. 
		*/
	public List<Integer> getColIndicesContaining(String substring){
		ArrayList<Integer> rvals = new ArrayList<Integer>();
		for(int c = 0;c < numCols;c++){
			if (colNames[c].contains(substring)){
				rvals.add(c);
			}
		}
		return(rvals);
	}
	
	/**
		* Get a row by index as a DoubleArrayList
		*/
	public DoubleArrayList getRowAsDoubleArrayList(int row){
		DoubleArrayList dal = new DoubleArrayList();
		ObjectMatrix1D dm1D  = matrix.viewRow(row);
		for(int r = 0;r < dm1D.size();r++){
			dal.add((Double)dm1D.get(r));
		}
		return(dal);
	}
  
	/**
		* Get a column by index as a DoubleArrayList
		*/
	public DoubleArrayList getColAsDoubleArrayList(int col){
		DoubleArrayList dal = new DoubleArrayList();
		ObjectMatrix1D dm1D  = matrix.viewColumn(col);
		for(int r = 0;r < dm1D.size();r++){
			dal.add((Double)dm1D.get(r));
		}
		return(dal);
	}
	
	/**
		* Get the index of the row with the given name. 
		*/
	public int getRowIdx(String row){
		
		return(rowName2Idx.get(row));
	
	}
	
	/**
		* Get the index of the column with the given name. 
		*/
	public int getColIdx(String col){
		
		return(colName2Idx.get(col));
	
	}
	
	/**
		* Get row by index as an array of objects. 
		*/
	public Object[] getRowAsArray(int row){
		
		return(matrix.viewRow(row).toArray());
	}
	
	/**
		* Get column by index as an array of objects
		*/
	public Object[] getColAsArray(int col){
		
		return(matrix.viewColumn(col).toArray());
	}
	
 
	
	/**
		* Get row at row index with [] notation in Groovy 
		*/
	public TableMatrix1D getAt(int ridx){
		
		return(getRow(ridx));
		
	}
	
	/**
		* Get row at row name with [] notation in Groovy
		*/	
	public TableMatrix1D getAt(String rowName){
		int ridx = getRowIdx(rowName);
		return(getRow(ridx));
	}
	
	/**
		* Get column by index of the column. 
		*/
	public TableMatrix1D getCol(int col){
		
		return(new TableMatrix1D(matrix.viewColumn(col),rowName2Idx,colNames[col]));
		 
	}
	
	/**
		* Get column by the name of the column
		*/
	public TableMatrix1D getCol(String colStr){
		int col = getColIdx(colStr);
		return(new TableMatrix1D(matrix.viewColumn(col),rowName2Idx,colNames[col]));
		
	}
	
	/**
		* Get row by index of the row.  
		*/
	public TableMatrix1D getRow(int row){
		
		return(new TableMatrix1D(matrix.viewRow(row),colName2Idx,rowNames[row]));
		 
	}
	
	/**
		* Get row by the name of the row. 
		*/
	public TableMatrix1D getRow(String rowStr){
		
		int row = getRowIdx(rowStr);
		return(new TableMatrix1D(matrix.viewRow(row),colName2Idx,rowNames[row]));
		
	}

	/**
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

	/**
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


	/**
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

	/**
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
			TableMatrix1D column = new TableMatrix1D(matrix.viewColumn(c),rowName2Idx,colNames[c]);			
			closure.call(new Object[] {column});
		}
		return this;
	}

	/**
		* Provide support for iterating over rows
		*/
	public Table eachRow(Closure closure) {
		for (int r = 0;r < numRows;r++) {
			//Object[] row = matrix.viewRow(r).toArray();  bit costly to make a copy...
			TableMatrix1D row = new TableMatrix1D(matrix.viewRow(r),colName2Idx,rowNames[r]);
			closure.call(new Object[] {row});
		}
		return this;
	}


	
	
	/**
		* Reorders rows according to the given list.
		*/
	public Table orderRowsBy(List newOrder){			
		Table newt = new Table(rows(),cols());
		newt.colNames = colNames.clone();
		newt.rowNames = new String[rows()];
		for(int r =0;r < rows();r++){
			//int oldr = ((int)newOrder.get(r)) -1; // for zero based coordnates.
			int oldr = ((int)newOrder.get(r));
			newt.rowNames[r] = rowNames[oldr];
			for(int c = 0;c < cols();c++){
				newt.matrix.setQuick(r,c,matrix.getQuick(oldr,c));			
			}
		}
		return(newt);
	}
	
	/**
		* Reorders columns according to the given list.
		*/
	public Table orderColumnsBy(List newOrder){	
		System.err.println("newOrder: "+newOrder);
		System.err.println("newOrder.size:"+newOrder.size());		
		Table newt = new Table(rows(),cols());
		newt.rowNames = rowNames.clone();
		newt.colNames = new String[cols()];
		
		System.err.println("DEBUG cols: "+cols());
		
		for(int c =0;c < cols();c++){
			//int oldc = ((int)newOrder.get(c)) -1; -1 is just for 1 based coord offset, right?
			int oldc = ((int)newOrder.get(c));
			
			System.err.println("\tDEBUG oldc: "+oldc);
			System.err.println("\tDEBUG c: "+c);
			
			newt.colNames[c] = colNames[oldc];
			for(int r = 0;r < rows();r++){
				newt.matrix.setQuick(r,c,matrix.getQuick(r,oldc));			
			}
		}
		return(newt);
	}
	
	/**
		* Returns a copy that is the transposition of this table. 
		*/
	public Table transpose(){
		Table ttable = new Table(this.numCols,this.numRows);
		ttable.rowNames = colNames.clone();
		ttable.colNames = rowNames.clone();
		ObjectMatrix2D diceView = matrix.viewDice();
		ttable.matrix = diceView.copy();
		return(ttable);
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



