
package durbin.weka;

import java.util.*;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;

import hep.aida.bin.DynamicBin1D;


/********************************************************************
* An attribute filter that converts a numeric attribute into a
* two-state nominal attribute split into either upper/lower quartiles
* or above/below median.
*
*/
public class QuartileFilter
			extends PotentialClassIgnorer
			implements UnsupervisedFilter, WeightedInstancesHandler {
//			extends Filter {

	boolean useMedian;
	String defaultCols = "first-last";

	/** Stores which columns to Discretize */
	protected Range discretizeCols = new Range();

	public String globalInfo() {
		return( "A batch filter that replaces a given numeric attribute with "
		        +"a nominal attribute depending on the quartile of that attribute value. "
		        +"The nominal value can be either {Upper25%, Lower25%} for single quarltile "
		        +"{AboveMedian, BelowMedian} for median filtering.");
	}

	/** Constructor - initialises the filter */
	public QuartileFilter() {
		defaultCols = "first-last";
		setAttributeIndices("first-last");
	}

	/*********************************************
	* Another constructor, sets the attribute indices immediately
	*
	* @param cols the attribute indices
	*/
	public QuartileFilter(String cols) {
		defaultCols = cols;
		setAttributeIndices(cols);
	}


	public void setAttributeIndices(String rangeList) {
		discretizeCols.setRanges(rangeList);
	}

	/****************************************
	* Gets an enumeration describing the available options.
	*
	* @return an enumeration of all the available options.
	*/
	public Enumeration listOptions() {
		Vector result = new Vector();
		Enumeration enm = super.listOptions();
		while (enm.hasMoreElements())
			result.add(enm.nextElement());

		result.addElement(new Option(
		                    "\tDescretize by median (vs top/bottom 25%) (Default = top/bottom)",
		                    "M", 0, "-M"));
		return result.elements();
	}

	/*********************************************
	* Parses a given list of options.
	*/
	public void setOptions(String[] options) throws Exception {
		super.setOptions(options);
		setUseMedian(Utils.getFlag('M', options));
	}
	public void setUseMedian(boolean newUseMedian) {
		useMedian = newUseMedian;
	}

	public boolean getUseMedian() {
		return(useMedian);
	}

	public String [] getOptions() {
		Vector        result;
		String[]      options;
		int           i;
		result = new Vector();

		options = super.getOptions();
		for (i = 0; i < options.length; i++)
			result.add(options[i]);

		if (getUseMedian()) result.add("-M");
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**********************************************
	* Returns the Capabilities of this filter.
	*
	* @return            the capabilities of this object
	* @see               Capabilities
	*/
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enableAllAttributes();
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enableAllClasses();
		result.enable(Capability.MISSING_CLASS_VALUES);
//		if (!getMakeBinary())
		result.enable(Capability.NO_CLASS);
		return result;
	}

	/************************************
	*
	*/
	public boolean batchFinished() throws Exception {
		if (getInputFormat() == null)
			throw new NullPointerException("No input instance format defined");

		// output format still needs to be set (depends on first batch of data)
		if (!isFirstBatchDone()) {
			Instances outFormat = new Instances(getInputFormat(), 0);
			//	outFormat.insertAttributeAt(new Attribute(
			//	                              "bla-" + getInputFormat().numInstances()), outFormat.numAttributes());
			setOutputFormat(outFormat);
		}

		Instances inst = getInputFormat();
		discretizeCols.setUpper(inst.numAttributes() - 1);
		Instances outFormat = getOutputFormat();

		// Compute objects to bin selected attributes...  Each DynamicBin1D object
		// will compute stas for a given attribute..
		int[] selectedAttributes = discretizeCols.getSelection();		
		DynamicBin1D[] bins = createAttributeBins(inst,selectedAttributes);

		if (useMedian) {
			medianTransformAttributes(inst,outFormat,bins,selectedAttributes);			
		} else {
			System.err.println("Sorry, quartile not yet implemented.");
			System.exit(1);
			//quartileTransformAttributes(inst,outFormat,bins);
		}

		flushInput();
		m_NewBatch = true;
		m_FirstBatchDone = true;

		return (numPendingOutput() != 0);
	}

	/*******************************************
	* Performs work of transforming numeric attributes into 1/0 values depending
	* on whether the value is above or below median.
	**/
	public void medianTransformAttributes(Instances data,Instances outFormat,
	                                      DynamicBin1D[] bins,int[] selectedAttributes) {
	                                        
		//System.err.println("Num selectedAttributes: "+selectedAttributes.length);		
		//for (int i = 0;i < selectedAttributes.length;i++) {
		//	System.err.println("\tselected: "+selectedAttributes[i]);
		//}	                                        
	                                        
		double[]  cutoffs = computeCutoffs(bins,selectedAttributes,0.50);
		
		//System.err.println("Num cutoffs: "+cutoffs.length);

		// Convert the old version of the attributes into the new version.
		// Note:  Currently it's assumed that a second filter will convert the
		// newly coded numeric variable into a nominal variable, though later I might
		// do that here instead.
		for (int i = 0; i < data.numInstances(); i++) {
		  Instance newInstance = (Instance) data.instance(i).copy();
//			double[] newValues = new double[outFormat.numAttributes()];
//			double[] oldValues = data.instance(i).toDoubleArray();
//			System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);

			// Modify just the selected attributes...
			for (int sIdx = 0; sIdx < selectedAttributes.length;sIdx++) {
				int attIdx = selectedAttributes[sIdx];
				double cutoff = cutoffs[attIdx];
//				double numericValue = oldValues[attIdx];			
        double numericValue = newInstance.value(attIdx);
				
				if (numericValue >= cutoff) newInstance.setValue(attIdx,1);
				else newInstance.setValue(attIdx,0);
				
				//if (numericValue > cutoff) newValues[attIdx] = 1;
				//else newValues[attIdx] = 0;
			}
			push(newInstance);
			//push(new Instance(1.0, newValues));
		}
	}

	/*******************************************
	*  For each selected attribute, compute the cutoff given the selected attributes
	*/
	public double[] computeCutoffs(DynamicBin1D[] bins,int[] selectedAttributes,double quartile) {
		double[] cutoffs = new double[bins.length];
		for (int i = 0;i < selectedAttributes.length;i++) {
		  int attIdx = selectedAttributes[i];
			double cutoff = bins[attIdx].quantile(0.50);
			cutoffs[attIdx] = cutoff;
			System.err.println("cutoff "+attIdx+" = "+cutoff);
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
		runFilter(new QuartileFilter(), args);
	}
}