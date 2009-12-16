
package durbin.weka;

import durbin.util.*;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

import groovy.lang.Closure;


/***************************************************************
*  My own version of CSVLoader.  It loads a delimiter separated file
*  into a Table object than converts that to a set of instances. CSV 
*  loader is OK, but balks at transposed tables, and I also have 
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
	
	/*****************************************************************************/
	/***************   Merge versions of read methods...  ************************/
	/*****************************************************************************/
	
	/* WARNING WARNING WARNING *********************
	 WARNING:  I THINK THIS MAY BE BROKEN.  Use InstanceUtils.mergeNamedInstances instead. 
	 *************/
	
	public Instances mergeRead(String fileName,String delimiter,Closure c) throws Exception{
    Instances data = mergeRead(fileName,fileName,delimiter,c);
  	return(data);
  }

  public Instances mergeRead(String fileName,String relationName,String delimiter,Closure c) throws Exception{
    return(mergeRead(fileName,relationName,delimiter,false,c));
  }

  public Instances mergeRead(String fileName,String relationName,String delimiter,boolean rowsAreInstances,
  Closure c) throws Exception{
    System.err.print(" Read table "+fileName+"...");
    Table t = new Table(fileName,delimiter,c);
    System.err.println("done.");
    System.err.print("Merge table to instances...");
    Instances data = mergeTableToInstances(t,relationName,rowsAreInstances);
    System.err.println("done.");
    return(data);
  }
  
  public Instances mergeTableToInstances(Table t,String relationName) throws Exception{
    return(mergeTableToInstances(t,relationName,false));
  }
  
  public Instances mergeTableToInstances(Table t,String relationName,boolean rowsAreInstances) throws Exception{
    if (rowsAreInstances) {
      //	return(mergeTableRowsToInstances(t,relationName));
      return(null);
  	} else {
  		return(mergeTableColsToInstances(t,relationName));
  	}
  }
  
    //public Instances mergeTableRowsToInstances(Table t,String relationName) {}// Implement later
  //  public Instances mergeTableColsToInstances(Table t,String relationName) {}


  
  
  /**********************************************
  * Merges attributes from a table into an existing set of instances.  If 
  * attributesToMerge is set then only those attributes will be merged, otherwise
  * all of the attributes in Table t will be merged.  The instances are matched
  * by name, so no assumption is made about the order of instances in the original
  * instances and in the Table t. <p>
  * 
  * <b>NOTE:</b> <b>REQUIRES</b> that setMergeData() be called first with the set 
  * of Instances to merge <b>AND</b> that the mergeData instances contain an attribute 
  * with the label <b>"ID"</b> that gies a unique name for each instance. 
  * 
  */
	public Instances mergeTableColsToInstances(Table t,String relationName) throws Exception {

    // quick sanity test to ensure that attributes desired actually exist...
	  Set<String> attributeSet = new HashSet<String>();
	  for(int i = 0;i < t.numRows;i++){
	    attributeSet.add(t.rowNames[i]);
	  }
	  	  
	  // If no merge list is given, use all the attributes...
	  if (attributesToMerge == null) attributesToMerge = attributeSet;
	  else{
	    for(String attribute: attributesToMerge){
	      if (!attributeSet.contains(attribute)){
	        throw new RuntimeException(attribute+" in attributesToMerge does not match any known attribute.");
	      }
	    }	    	    
	  }
	  	  	  
	  // Add each attribute to merge as an attribute in the dataset... all values initially 
	  // set to missing value
	  Instances newData = new Instances(mergeData); // KJD potential speed optimization... copy???
	  	  
	  for(String attributeName : attributesToMerge){
      // Add the selected attributes from t
      Add filter = new Add();
      filter.setAttributeName(attributeName);
      filter.setInputFormat(newData);
      newData = Filter.useFilter(newData, filter);
	  }
	  
    // Make a map between instance names and instance indices...
    // (Maybe I'm missing where this is part of the API already?)        
    Map<String,Integer> instanceNameMap =  InstanceUtils.createInstanceNameMap(newData);
    
    System.err.println("matrix rows: "+t.matrix.rows()+" cols: "+t.matrix.columns());  
        
	  // for each instance in the new table... skipping first col that is the row title    
	  for(int col = 0;col < t.colNames.length;col++){
	    // find the corresponding instance in mergeData (do nothing if no match)	    
	    String instanceName = t.colNames[col]; 
	    if (instanceNameMap.containsKey(instanceName)){
	      int instanceIdx = instanceNameMap.get(instanceName);
  	    System.err.println("col: "+col+" instanceName: "+instanceName+" instanceIdx: "+instanceIdx);
	      Instance instance = newData.instance(instanceIdx);

        // For each new attribute...
	      for(String attributeName : attributesToMerge){
	        int attIdx = newData.attribute(attributeName).index();
	        int row = t.getRowIdx(attributeName);
	        
	        //System.err.println("aName: "+attributeName+" aidx: "+attIdx+
	        //" row: "+row+" col: "+col);
	        	        	        
	        Double value = (Double) t.matrix.getQuick(row,col);
	        if ((value == null) || (Double.isNaN(value))){	        
	          //System.err.println("DEBUG:  missing value found "+
	          //instanceName+" "+attributeName+" row: "+row+" col: "+col+" instanceIdx: "+
	          //instanceIdx);
	          instance.setValue(attIdx,Instance.missingValue());
	        }else instance.setValue(attIdx,value);	       	        
    	  }
      }else{
        System.out.println("Hey, map doesn't contain key: "+instanceName);
        for(String key: instanceNameMap.keySet()){
          System.out.println("\t"+key+"\t"+instanceNameMap.get(key));
        }
      }
    } 
    
    newData.setRelationName(relationName);
    return(newData);
  } 
}


/****************************************
*  Performance Notes: 
* 
* 
*/ 






