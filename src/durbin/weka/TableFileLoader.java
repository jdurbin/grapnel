
package durbin.weka;

import durbin.util.*;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

import groovy.lang.Closure;



/***
*  My own version of CSVLoader.  It loads a delimiter separated file
*  into a Table object than converts that to a set of instances. CSV 
*  loader is OK, but balks at transposed tables, and I sometimes have 
*  table cells that need pre-processing (via Closures) before being
*  saved into the table.  
*
*  This class assumes that all attributes stored in the table file
*  are numeric.  These may be converted to nominal by a subsequent
*  filter of some form.
*
*/
public class TableFileLoader {
  
  boolean addInstanceNamesAsFeatures = false;
  
  Collection<String> instanceNames;
  Instances mergeData;
  Collection<String> attributesToMerge;
  
  /*********************************************
  * Indicate that instance names should be added as a feature (default = false)
  */ 
  public void setAddInstanceNamesAsFeatures(boolean val){addInstanceNamesAsFeatures = val;}
  
  /*********************************************
  * Set the instances to use in subsequent calls to mergeRead
  */ 
  public void setMergeSource(Instances data){
    mergeData = data;
  }
  
  /******************************************
  * 
  */ 
  public void setAttributesToMerge(Collection<String> att2Merge){
    attributesToMerge = att2Merge;
  }
    
  
	// The default read assumes that columns are instances...
  public Instances read(String fileName,String delimiter,Closure c) throws Exception {
		Instances data = read(fileName,fileName,delimiter,c);
		return(data);
	}
  
	public Instances read(String fileName,String relationName,String delimiter,Closure c) throws Exception {
		return(read(fileName,relationName,delimiter,false,c));
	}

	public Instances read(String fileName,String relationName,String delimiter,boolean rowsAreInstances,
	                      Closure c) throws Exception {
		Table t = new Table(fileName,delimiter,c);
		Instances data = tableToInstances(t,relationName,rowsAreInstances);
		return(data);
	}

	
	public Instances tableToInstances(Table t,String relationName) throws Exception {
		// Default to columns as instances, rows as attributes..
		return(tableToInstances(t,relationName,false));
	}

	public Instances tableToInstances(Table t,String relationName,boolean rowsAreInstances) {
		if (rowsAreInstances) {
			return(tableRowsToInstances(t,relationName));
		} else {
			return(tableColsToInstances(t,relationName)); // KJD
		}
	}
	
	
  /****************************************************
  *  Convert a table to a set of instances, with <b>rows</b> representing individual </b>instances</b>
  *  and <b>columns</b> representing <b>attributes</b> 
  */
	public Instances tableRowsToInstances(Table t,String relationName) {	
	  
		System.err.print("Converting table rows to instances...");
	
		// Set up attributes, which for rowInstances will be the colNames...
		FastVector atts = new FastVector();
		ArrayList<Boolean> isNominal = new ArrayList<Boolean>();
		ArrayList<FastVector> allAttVals = new ArrayList<FastVector>(); // Save values for later...				
		for (int c = 0;c < t.numCols;c++) {			
			if (columnIsNumeric(t,c)){
				isNominal.add(false);
				atts.addElement(new Attribute(t.colNames[c]));
				allAttVals.add(null); // No enumeration of attribute values.
			}else{
				// It's nominal... determine the range of values
				isNominal.add(true);
				FastVector attVals = getColValues(t,c);
				atts.addElement(new Attribute(t.colNames[c],attVals));
				// Save it for later
				allAttVals.add(attVals);
			}
		}

		// Create Instances object..
		Instances data = new Instances(relationName,atts,0);
		data.setRelationName(relationName);

		// Fill the instances with data...
		// For each instance...
		for (int r = 0;r < t.numRows;r++) {
			double[] vals = new double[data.numAttributes()];

			// for each attribute
			for (int c = 0;c < t.numCols;c++) {			    
				String val = (String) t.matrix.getQuick(r,c);
				if (val == "?") vals[c] = Instance.missingValue();
				else if (isNominal.get(c)){
					vals[r] = allAttVals.get(c).indexOf(val);
				}else{ 
					vals[r] = Double.parseDouble((String)val);												
				}
			}
			// Add the a newly minted instance with those attribute values...
			data.add(new Instance(1.0,vals));
		}
		
		if (addInstanceNamesAsFeatures){		  		  
			Instances newData = new Instances(data);
	    newData.insertAttributeAt(new Attribute("ID",(FastVector)null),0);	      
	    int attrIdx = newData.attribute("ID").index(); // Paranoid... should be 0

	    // We save the instanceNames in a list because it's handy later on...
	    instanceNames = new ArrayList<String>();

	    for(int r = 0;r < t.rowNames.length;r++){
				instanceNames.add(t.rowNames[r]);
				newData.instance(r).setValue(attrIdx,t.rowNames[r]);
	    }
			data = newData;
	  }		
	
		System.err.println("done.");
		
		return(data);
	}
	
	/****
	* Test if a row is all numeric or if it is a nominal row. 
	*/ 
	public boolean rowIsNumeric(Table t,int row){
		
		// Find first non-missing value in row...
		String testValue ="";
		for(int c = 0;c < t.numCols;c++){
			testValue = (String) t.matrix.getQuick(row,c);
			if (testValue !="?") break; 						
		}

		// See if it's numeric...
		Scanner scanner = new Scanner(testValue);
		if (scanner.hasNextDouble()){
			return(true);
		}else{
			return(false);
		}
	}
	
	/****
	* Test if a column is all numeric or if it is a nominal column
	*/ 
	public boolean columnIsNumeric(Table t,int col){		
		// Find first non-missing value in row...
		String testValue = "";
		for(int r = 0;r < t.numRows;r++){
			testValue = (String) t.matrix.getQuick(r,col);
			if (testValue !="?") break; 						
		}

		// See if it's numeric...
		Scanner scanner = new Scanner(testValue);
		if (scanner.hasNextDouble()){
			return(true);
		}else{
			return(false);
		}
	}
	

  /****************************************************
  *  Convert a table to a set of instances, with <b>columns</b> representing individual </b>instances</b>
  *  and <b>rows</b> representing <b>attributes</b> (e.g. as is common with microarray data)
  */
  public Instances tableColsToInstances(Table t,String relationName) {
       
		System.err.print("Converting table cols to instances...");

  	// Set up attributes, which for colInstances will be the rowNames...
  	FastVector atts = new FastVector();
		ArrayList<Boolean> isNominal = new ArrayList<Boolean>();
		ArrayList<FastVector> allAttVals = new ArrayList<FastVector>(); // Save values for later...
		
		for (int r = 0;r < t.numRows;r++) {						
			if (rowIsNumeric(t,r)){
				isNominal.add(false);
				atts.addElement(new Attribute(t.rowNames[r]));
				allAttVals.add(null); // No enumeration of attribute values.
			}else{
				// It's nominal... determine the range of values and create a nominal attribute...
				isNominal.add(true);
				FastVector attVals = getRowValues(t,r);
				atts.addElement(new Attribute(t.rowNames[r],attVals));
				// Save it for later
				allAttVals.add(attVals);
			}
		}

		// Create Instances object..
		Instances data = new Instances(relationName,atts,0);
		data.setRelationName(relationName);

		/*******  CREATE INSTANCES **************/ 
		// Fill the instances with data...	
		// For each instance...
		for (int c = 0;c < t.numCols;c++) {
			double[] vals = new double[data.numAttributes()];	// Even nominal values are stored as double pointers.

			// For each attribute fill in the numeric or attributeValue index...
			for (int r = 0;r < t.numRows;r++) {			    
				String val = (String) t.matrix.getQuick(r,c);
				if (val == "?") vals[r] = Instance.missingValue();
				else if (isNominal.get(r)){
					vals[r] = allAttVals.get(r).indexOf(val);
				}else{ 
					vals[r] = Double.parseDouble((String)val);												
				}
			}						
			// Add the a newly minted instance with those attribute values...
			data.add(new Instance(1.0,vals));
	}		

   
		/*******  ADD FEATURE NAMES **************/ 
		if (addInstanceNamesAsFeatures){		  		  
		  Instances newData = new Instances(data);
      newData.insertAttributeAt(new Attribute("ID",(FastVector)null),0);	      
      int attrIdx = newData.attribute("ID").index(); // Paranoid... should be 0
      
      // We save the instanceNames in a list because it's handy later on...
      instanceNames = new ArrayList<String>();
      
      for(int c = 0;c < t.colNames.length;c++){
        instanceNames.add(t.colNames[c]);
        newData.instance(c).setValue(attrIdx,t.colNames[c]);
      }            
      data = newData;
  	}		

		System.err.println("done.");
		
  	return(data);
  } 


	/***
	* Create a FastVector containing the set of values found in the given col...
	*/ 
	public FastVector getColValues(Table t,int colIdx){				
		HashSet<String> valSet = new HashSet<String>();
		for(int r = 0;r < t.rows();r++){
			valSet.add((String) t.matrix.getQuick(r,colIdx));
		}
		
		FastVector attVals = new FastVector();		
		for(Object v : valSet){
			attVals.addElement(v);
		}
		return(attVals);
	}


	/***
	* Create a FastVector containing the set of values found in the given row...
	*/ 
	public FastVector getRowValues(Table t,int rowIdx){				
		HashSet<String> valSet = new HashSet<String>();
		for(int c = 0;c < t.cols();c++){
			valSet.add((String) t.matrix.getQuick(rowIdx,c));
		}
		
		FastVector attVals = new FastVector();		
		for(Object v : valSet){
			attVals.addElement(v);
		}
		return(attVals);
	}


	/****************************************************
  *  Convert a table to a set of instances, with <b>columns</b> representing individual </b>instances</b>
  *  and <b>rows</b> representing <b>attributes</b> (e.g. as is common with microarray data)
  */
/*  DEPRECATED 
  public Instances tableColsToInstances(Table t,String relationName) {
       
  	// Set up attributes, which for colInstances will be the rowNames...
  	FastVector atts = new FastVector();

		for (int r = 0;r < t.numRows;r++) {
			atts.addElement(new Attribute(t.rowNames[r]));
		}

		// Create Instances object..
		Instances data = new Instances(relationName,atts,0);
		data.setRelationName(relationName);

		// Fill the instances with data...	
		// For each instance...
		for (int c = 0;c < t.numCols;c++) {
			double[] vals = new double[data.numAttributes()];			

			// For each attribute...
			for (int r = 0;r < t.numRows;r++) {			    
				Object val = t.matrix.getQuick(r,c);
				if (val == null) 	vals[r] = Instance.missingValue();
				else vals[r] = (Double) val;								
			}						
			// Add the a newly minted instance with those attribute values...
			data.add(new Instance(1.0,vals));
		}		

   // System.err.println("******* Before Add Instance Names **********");
    //System.err.println(data);

		if (addInstanceNamesAsFeatures){		  		  
		  Instances newData = new Instances(data);
      newData.insertAttributeAt(new Attribute("ID",(FastVector)null),0);	      
      int attrIdx = newData.attribute("ID").index(); // Paranoid... should be 0
      
      // We save the instanceNames in a list because it's handy later on...
      instanceNames = new ArrayList<String>();
      
      for(int c = 0;c < t.colNames.length;c++){
        instanceNames.add(t.colNames[c]);
        newData.instance(c).setValue(attrIdx,t.colNames[c]);
      }            
      data = newData;
  	}				
  	return(data);
  }
*/
}


/****************************************
*  Performance Notes: 
* 
* 
*/ 






