package durbin.weka;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.filters.*;

import hep.aida.bin.DynamicBin1D;

/****************************************************************************
* An attribute filter that converts a numeric attribute into a two-state nominal 
* attribute split into either upper/lower quartiles or above/below median, or 
* based on one or two predefined splits.  When there are two split points, the 
* instances in the middle are omitted. 
*/ 
public class NumericToQuartileNominal
extends SimpleBatchFilter {

  /** Stores which attributes (cols) to Discretize */
  String defaultAttributes = "first-last";
  protected Range attributesToDiscretize = new Range(defaultAttributes);
  
  boolean useMedian = true;
  boolean useCutoffs = false;  
  
  double cutoff1;
  double cutoff2;

  String nominalValue1 = "low";
  String nominalValue2 = "high";
  
  /******************************************
  * Set the string names to use for nominal values. 
  */ 
  public void setNominalValues(String value1,String value2){
    nominalValue1 = value1;
    nominalValue2 = value2;
  }
  
  public void setCutoffs(double cutoff1,double cutoff2){
    useCutoffs = true;
    this.cutoff1 = cutoff1;
    this.cutoff2 = cutoff2;
  }
  
  public void setCutoff(double cutoff){
    useCutoffs = true;
    this.cutoff1 = cutoff;
    this.cutoff2 = cutoff;
  }
  
  
  /*******************************************
  *  If true, split features into high/low based on 
  *  the median.  If false, 
  */ 
  public void setUseMedian(boolean u){useMedian = u;}
  
  /*******************************************
  *  If true, predefined cutoff values are used. 
  * 
  */ 
  public void setUseCutoffs(boolean u){useCutoffs = u;}
  

  /*******************************************
  * Describe the set of attributes for this filter to work on. 
  */ 
	public void setAttributeIndices(String rangeList) {
		attributesToDiscretize.setRanges(rangeList);
	}
  
  /*********************************************
  *  String describing this filter. Shows up in command line help and
  *  in GUI (class must be in GUI classpath to be picked up)
  */ 
  public String globalInfo() {
    return( "A batch filter that replaces a given numeric attribute with "
      +"a nominal attribute depending on the quartile of that attribute value. "
      +"The nominal value can be either {Upper25%, Lower25%} for single quarltile "
      +"{AboveMedian, BelowMedian} for median filtering.");
  }


  /*********************************************
  * Returns the capabilities of this filter, which are the kinds of data it can 
  * handle. 
  */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.enableAllAttributes();
    result.enableAllClasses();
    result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
    result.enable(Capability.MISSING_VALUES);  //// filter doesn't need class to be set//
    return result;
  }


  /**********************************************
  * Creates an empty set of instances in the proper output format. For some 
  * filters this will be the same as the input format, for some it will
  * involve changing the type of some attributes, or adding or removing 
  * some. In our case, we will be converting the selected numeric attributes 
  * to nominal. 
  */
  protected Instances determineOutputFormat(Instances inputFormat) {

   attributesToDiscretize.setUpper(inputFormat.numAttributes() - 1);

   Instances data = inputFormat; // sugar.    
   FastVector atts = new FastVector();
   for (int i = 0; i < data.numAttributes(); i++) {

      // If it's not part of our set to modify, just add it to the new attribute list. 
     if (!attributesToDiscretize.isInRange(i) || !data.attribute(i).isNumeric()) {
       atts.addElement(data.attribute(i));
       continue;
     }

     // Otherwise, create a new attribute...
     FastVector values = new FastVector();
     values.addElement(nominalValue1);
     values.addElement(nominalValue2);

     // Add an attribute with the same name as the previous numeric attribute
     // but make it nominal with the new values...
     atts.addElement(new Attribute(data.attribute(i).name(), values));
   }

   Instances result = new Instances(inputFormat.relationName(), atts, 0);
   result.setClassIndex(inputFormat.classIndex());
   return(result);
 } 

  /**********************************************
  * Processes all of the instances. 
  */
  protected Instances process(Instances inst) {
    
    // Range has to be told what 'last' means...
    attributesToDiscretize.setUpper(inst.numAttributes() - 1);
    
    // Compute objects to bin selected attributes...  Each DynamicBin1D object
  	// will compute stas for a given attribute..
  	int[] selectedAttributes = attributesToDiscretize.getSelection();		
  	DynamicBin1D[] bins = createAttributeBins(inst,selectedAttributes);
    
    Instances result = null;
    
    // Use explicit cutoffs...
    if (useCutoffs){
        result = cutoffTransformAttributes(inst,cutoff1,cutoff2,selectedAttributes);
    }else{
    
      if (useMedian) {
        double[]  cutoffs = computeCutoffs(bins,selectedAttributes,0.50);    
			  result = medianTransformAttributes(inst,cutoffs,selectedAttributes);			
		  } else {
			  double[]  cutoffsHigh = computeCutoffs(bins,selectedAttributes,0.25);    
			  double[]  cutoffsLow = computeCutoffs(bins,selectedAttributes,0.75);    

			  // Splits into upper and lower quarters, removing instances that fall inbetween. 
			  result = quartileTransformAttributes(inst,cutoffsHigh,cutoffsLow,selectedAttributes);
		  }
		}
    return result;
  }


  /*******************************************
	* Performs work of transforming numeric attributes into 1/0 values depending
	* on whether the value is above or below a fixed cutoff value or values. 
	**/
	public Instances cutoffTransformAttributes(Instances data,double cutoff1,double cutoff2,
	                                      int[] selectedAttributes) {
	                                        	                                        		
		Instances result = new Instances(determineOutputFormat(data), 0);
		
		// Convert the old version of the attributes into the new version.
		for (int i = 0; i < data.numInstances(); i++) {      
		  double[] values = new double[data.numAttributes()];
		  Instance oldInstance = data.instance(i);
		  
		  boolean bSkipInstance = false;
		  
		  // for each attribute, either copy or create a new one...
		  for(int aIdx = 0; aIdx < result.numAttributes();aIdx++){
		    if (!attributesToDiscretize.isInRange(aIdx)){		      
		      // If it's not one of our new attributes, just copy the old value..
		      values[aIdx] = oldInstance.value(aIdx);
		    }else{
		      // Otherwise...
		      
		      // Get the attribute from the new result instances (should be nominal)...
		      Attribute attribute = result.attribute(aIdx);
		      double numericValue = oldInstance.value(aIdx);
		  
		      // Single cutoff case...
          if (cutoff1 == cutoff2){
            if (numericValue <= cutoff1) values[aIdx] = 0.0; // index to nominal value
            else values[aIdx] = 1.0; // inde to nominal value.     			
          }else{
            if (numericValue <= cutoff1) values[aIdx] = 0.0;
            else if (numericValue >= cutoff2) values[aIdx] = 1.0;
            else bSkipInstance = true;  // Going to omit any values that fall in the middle. 
          }   			
		    }
	    }
	    if (!bSkipInstance)	result.add(new Instance(1,values));
	    else bSkipInstance = false;
    }
    return(result);
  }


  /*******************************************
	* Performs work of transforming numeric attributes into 1/0 values depending
	* on whether the value is above or below median.
	**/
	public Instances medianTransformAttributes(Instances data,double[] cutoffs,
	                                      int[] selectedAttributes) {
	                                        	                                        		
		Instances result = new Instances(determineOutputFormat(data), 0);
		
		// Convert the old version of the attributes into the new version.
		for (int i = 0; i < data.numInstances(); i++) {      
		  double[] values = new double[data.numAttributes()];
		  Instance oldInstance = data.instance(i);
		  
		  // for each attribute, either copy or create a new one...
		  for(int aIdx = 0; aIdx < result.numAttributes();aIdx++){
		    if (!attributesToDiscretize.isInRange(aIdx)){		      
		      // If it's not one of our new attributes, just copy the old value..
		      values[aIdx] = oldInstance.value(aIdx);
		    }else{
		      // Otherwise...
		      
		      // Get the attribute from the new result instances (should be nominal)...
		      Attribute attribute = result.attribute(aIdx);
		  
		      // determine where it falls on our cutoff...
          //int attIdx = selectedAttributes[sIdx];
    			double cutoff = cutoffs[aIdx];
          double numericValue = oldInstance.value(aIdx);

    			if (numericValue <= cutoff) values[aIdx] = 0.0; // index to nominal value
    			else values[aIdx] = 1.0; // inde to nominal value.     			
		    }
	    }
	    result.add(new Instance(1,values));
    }
    return(result);
  }
  
  
  /*******************************************
	* Performs work of transforming numeric attributes into 1/0 values depending
	* on whether the value is above 75% quartile or below 25% quartile. Instances  
	* with values in between are deleted. 
	**/
	public Instances quartileTransformAttributes(Instances data,double[] cutoffs1,double[] cutoffs2,
	                                        int[] selectedAttributes) {
	                                        	                                        		
		Instances result = new Instances(determineOutputFormat(data), 0);
		
	
		// Convert the old version of the attributes into the new version.
		for (int i = 0; i < data.numInstances(); i++) {      
		  double[] values = new double[data.numAttributes()];
		  Instance oldInstance = data.instance(i);
		  
		  boolean bSkipInstance = false;
		  
		  // for each attribute, either copy or create a new one...
		  for(int aIdx = 0; aIdx < result.numAttributes();aIdx++){
		    if (!attributesToDiscretize.isInRange(aIdx)){		      
		      // If it's not one of our new attributes, just copy the old value..
		      values[aIdx] = oldInstance.value(aIdx);
		    }else{
		      // Otherwise...
		      System.err.println("quartile1: "+cutoffs1[aIdx]+" quartile2: "+cutoffs2[aIdx]);
		      
		      // Get the attribute from the new result instances (should be nominal)...
		      Attribute attribute = result.attribute(aIdx);
		  
		      // determine where it falls on our cutoff...
          //int attIdx = selectedAttributes[sIdx];
    			double cutoff1 = cutoffs1[aIdx];
    			double cutoff2 = cutoffs2[aIdx];    			
          double numericValue = oldInstance.value(aIdx);

    			if (numericValue <= cutoff1) values[aIdx] = 0.0; // index to nominal value
    			else if (numericValue >= cutoff2) values[aIdx] = 1.0; // inde to nominal value.     			
    			else bSkipInstance = true;
		    }
	    }	    
	    
	    if (!bSkipInstance) result.add(new Instance(1,values));
	    else bSkipInstance = false;
    }
    return(result);
  }

	/*******************************************
	*  For each selected attribute, compute the cutoff given the selected attributes
	*/
	public double[] computeCutoffs(DynamicBin1D[] bins,int[] selectedAttributes,double whichQuartile) {
		double[] cutoffs = new double[bins.length];
		for (int i = 0;i < selectedAttributes.length;i++) {
		  int attIdx = selectedAttributes[i];
			double cutoff = bins[attIdx].quantile(whichQuartile);
			System.err.println("AttributeIdx: "+attIdx+" Cutoff: "+cutoff);
			cutoffs[attIdx] = cutoff;
		}
		return(cutoffs);
	}

	/******************************************
	*  Creates a DynamicBin1D object for each selected attribute in the instances.
	*  From these DynamicBin1D objects, we can compute a variety of attribute
	*  statistics including quantile.
	* 
	*  Note: Missing values are simply skipped. 
	*/
	public DynamicBin1D[] createAttributeBins(Instances inst,int[] selectedAttributes) {
		// Create objects to compute the quartiles and median cutoffs for each attribute...
		DynamicBin1D[] bins = new DynamicBin1D[inst.numAttributes()];
		for (int i = 0;i < bins.length;i++) bins[i] = new DynamicBin1D();

		// Go through the instances and create bins for each selected attribute
		for (int i = 0;i < inst.numInstances();i++) {
			double[] values = inst.instance(i).toDoubleArray();
			for (int sIdx = 0; sIdx < selectedAttributes.length;sIdx++) {
			  int attrIdx = selectedAttributes[sIdx];
			  double value = values[attrIdx];
			  if (value != Instance.missingValue()){
			    bins[attrIdx].add(values[attrIdx]); 
			  }else{
			    System.err.println("Value for instance: "+i+" attribute "+attrIdx+" is missing.");
			  }		  
			}
		}
		return(bins);
	}

  
  public static void main(String[] args) {
    runFilter(new NumericToQuartileNominal(), args);
  }
}