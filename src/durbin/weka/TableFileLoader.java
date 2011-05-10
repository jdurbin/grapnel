
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
			return(tableColsToInstances(t,relationName));
		}
	}
	
	/***
	*  Examines a delimited file to determine if columns are numeric or nominal. 
	*/
	public boolean[] isNumericColumns(String fileName,String regex) throws Exception {

		// Read the col headings and figure out the number of columns in the table..
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = reader.readLine();

		 // Not quite right, because includes spurious 0,0 column. 
		String[] fields = line.split(regex,-1); // -1 to include empty cols.
		int numCols = fields.length -1;
		
		// We'll assume that each column is all of a type so we only need to check the 
		// first row to see what type the column is, nominal or numeric. 
		boolean [] isNumeric = new boolean[numCols];
		line = reader.readLine();					
		Scanner scanner = new Scanner(line).useDelimiter("\t");
		scanner.next(); // Consume row name						
		int colIdx = 0;
		while(scanner.hasNext()){
			if (scanner.hasNextDouble()) {  
				isNumeric[colIdx] = true;
			}else{
				isNumeric[colIdx] = false;
			}
			colIdx++;								
		}
		return(isNumeric);
	}
	
	
	

  /****************************************************
  *  Convert a table to a set of instances, with <b>rows</b> representing individual </b>instances</b>
  *  and <b>columns</b> representing <b>attributes</b> 
  */
	public Instances tableRowsToInstances(Table t,String relationName) {	
	  
	  if (addInstanceNamesAsFeatures){	
	    System.err.println("addInstanceNamesAsFeatures not currently supported with RowInstances.");
	    System.exit(1);
	  }
	  
		// Set up attributes, which for rowInstances will be the colNames...
		FastVector atts = new FastVector();
		for (int c = 0;c < t.numCols;c++) {
			atts.addElement(new Attribute(t.colNames[c]));
		}

		// Create Instances object..
		Instances data = new Instances(relationName,atts,0);

		// Fill the instances with data...
		// For each instance...
		for (int r = 0;r < t.numRows;r++) {
			double[] vals = new double[data.numAttributes()];

			// For each attribute...
			for (int c = 0;c < t.numCols;c++) {
				Object val = t.matrix.getQuick(r,c);
				if (val == null) 	vals[c] = Instance.missingValue();
				else vals[c] = (Double) val;
			}
			data.add(new Instance(1.0,vals));
		}
		return(data);
	}
	
	/***
	* Create a FastVector containing the set of values found in the given row...
	*/ 
	/*
	public FastVector getRowValues(Table t,int rowIdx){				
		HashSet<String> valSet = new HashSet<String>();
		for(int c = 0;c < t.cols();c++){
			valSet.add(t.matrix.getQuick(rowIdx,c));
		}
		
		attVals = new FastVector();		
		for(Object v : valSet){
			attVals.addElement(v);
		}
		return(attVals);
	}
	*/

  /****************************************************
  *  Convert a table to a set of instances, with <b>columns</b> representing individual </b>instances</b>
  *  and <b>rows</b> representing <b>attributes</b> (e.g. as is common with microarray data)
  */
/*
  public Instances tableColsToInstances2(Table t,String relationName) {
       
  	// Set up attributes, which for colInstances will be the rowNames...
  	FastVector atts = new FastVector();
		for (int r = 0;r < t.numRows;r++) {
			
			// We're going to assume that the first value is like all of the values 
			// and choose our type from that...
			String testValue = t.matrix.getQuick(r,0) 			
			Scanner scanner = new Scanner(testValue)
			if (scanner.hasNextDouble()){
				// It's numeric...
				atts.addElement(new Attribute(t.rowNames[r]));
			}else{
				// It's nominal... determine the range of values
				FastVector attVals = getRowValues(t,r)
				atts.addElement(new Attribute(t.rowNames[r],attVals));
			}
		}
		
		KJD need to save the numeric/nominal determination to be used during stuffing below...

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

	/****************************************************
  *  Convert a table to a set of instances, with <b>columns</b> representing individual </b>instances</b>
  *  and <b>rows</b> representing <b>attributes</b> (e.g. as is common with microarray data)
  */
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
}


/****************************************
*  Performance Notes: 
* 
* 
*/ 






