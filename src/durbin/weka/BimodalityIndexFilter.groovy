package durbin.weka;

import durbin.stat.BimodalMixtureModel as BMM

import durbin.weka.*;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability
import weka.filters.*;
import jMEF.*;

// KJD would be nice to have option to filter but not replace values with BMI. 
public class BimodalityIndexFilter extends SimpleBatchFilter {

	double[] attr2bmi;
	double 	 bimodalityThreshold = 1.0;
	
	double[] mean1;
	double[] mean2;
	def selectedAttributes
	
	def outputFormatInst = null;

	static{WekaAdditions.enable()}
	static err = System.err

	public String globalInfo() {
		return   "Replaces  "
		+ "containing the index of the processed instance.";
	}
	
	String getRevision(){return("1.0");}
	
	/**
   * Parses a given list of options. <p/>
   */
  public void setOptions(String[] options) throws Exception{
    String	tmpStr;

		tmpStr = Utils.getOption('B', options);
    if (tmpStr.length() != 0) {
      bimodalityThreshold = Double.parseDouble(tmpStr);
    } else {
      bimodalityThreshold = 1.0;
    }
  }

	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
//		result.enableAllAttributes();
		result.disableAllAttributes();
		result.enableAllClasses();
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
		return result;
	}


	Instances determineOutputFormat(Instances inst) {
		
		// First time, we compute the output format... subsequent calls to determineOutputFormat
		// will simply return precomputed format...
		if (outputFormatInst != null) {
			System.err.println("output format already determined, returning saved format")
			return(outputFormatInst);
		}
		
		//Instances result = new Instances(inst, 0);
		//System.err.println "in determineOutputFormat  inst.numInstances:"+inst.numInstances();
		selectedAttributes = []
		
		// Compute the bimodality index for each attribute...
		mean1 = new double[inst.numAttributes()];
		mean2 = new double[inst.numAttributes()];
		attr2bmi = new double[inst.numAttributes()];

		//System.err.println "numAttributes: "+inst.numAttributes();

		for (int attrIdx = 0; attrIdx < inst.numAttributes(); attrIdx++){
			
			//System.err.println "attrIdx:"+attrIdx			
			double[] attrvals = inst.attributeToDoubleArray(attrIdx) 

			//System.err.println "attrvals obtained."
			MixtureModel mm = BMM.estimateMixtureParameters(attrvals)	
			
			//System.err.println "mm:"+mm			
			def m1 = mm.param[0].array[0]
			mean1[attrIdx] = m1
			def m2 = mm.param[1].array[0]
			mean2[attrIdx] = m2			

			double bmi = BMM.bimodalityIndex(mm,attrvals.length)			
			attr2bmi[attrIdx] = bmi;

			if (bmi > bimodalityThreshold){
				Attribute attribute = inst.attribute(attrIdx)
				def attrName = attribute.name()
				//System.err.println "save "+attrName
				selectedAttributes << attrName
			}			
		}
		
		System.err.println "\nselectedAttributes: "+selectedAttributes.size()		
				
		// Remove the attributes that don't meet the bimodality threshold...
		def result;
		result = WekaMine.subsetAttributes(inst,selectedAttributes)
		outputFormatInst = result;		
		return result;
	}

	Instances process(Instances inst) {
		
		// Result will only contain the bimodality index selected attributes...
		// determineOutputFormat will compute bimodality index for every attribute 
		// and save the mixture model parameters in mean1/mean2
		// KJD why is process calling determineOutputFormat???
		//Instances result = new Instances(determineOutputFormat(inst), 0);		
		
		// Scaled will contain all of the attributes... We first created a 
		// scaled copy with all the attributes because mean1,mean2 are defined
		// over all the attributes and it's a pain to map the indices post-attribute
		// removal (could do it by name... smaller, different, pain).  
		// Once have scaled all the attributes, will subset them to only include
		// those with high enough bimodality index. 
		Instances scaled = new Instances(inst, 0);	
		
		//System.err.println "CHECK1 inst.numAttributes(): "+inst.numAttributes()	
		//System.err.println "CHECK1 inst.numInstances(): "+inst.numInstances()	
	
		// Scale each attribute value by the mixture model parameters...
		for (int i = 0; i < inst.numInstances(); i++) {
			double[] values = new double[scaled.numAttributes()];

			for (int n = 0; n < inst.numAttributes(); n++){
				def m1 = mean1[n]
				def m2 = mean2[n]

				double y = inst.instance(i).value(n);
				double scaledy = (2*(y-m1))/(m2-m1-1);					
				values[n] = scaledy
			}
			scaled.add(new Instance(1, values));
		}
		
		//System.err.println "CHECK2"	
				
		// Remove the attributes that don't meet the bimodality threshold...
		def result;
		result = WekaMine.subsetAttributes(scaled,selectedAttributes)
				
		//System.err.println "CHECK3"			
				
		return result;
	}
	
	/**
   * Signify that this batch of input to the filter is finished. If
   * the filter requires all instances prior to filtering, output()
   * may now be called to retrieve the filtered instances. Any
   * subsequent instances filtered should be filtered based on setting
   * obtained from the first batch (unless the setInputFormat has been
   * re-assigned or new options have been set). Sets m_FirstBatchDone
   * and m_NewBatch to true.
   *
   * @return 		true if there are instances pending output
   * @throws IllegalStateException 	if no input format has been set. 
   * @throws Exception	if something goes wrong
   * @see    		#m_NewBatch
   * @see    		#m_FirstBatchDone 
   */
 public boolean batchFinished() throws Exception {
   int         i;
   Instances   inst;

   if (getInputFormat() == null)
     throw new IllegalStateException("No input instance format defined");

   // get data
   inst = new Instances(getInputFormat());

   // if output format hasn't been set yet, do it now
   if (!hasImmediateOutputFormat() && !isFirstBatchDone())
     //setOutputFormat(determineOutputFormat(new Instances(inst, 0)));
		System.err.println "calling determineOutputFormat"
		setOutputFormat(determineOutputFormat(inst));

   // don't do anything in case there are no instances pending.
   // in case of second batch, they may have already been processed
   // directly by the input method and added to the output queue
   if (inst.numInstances() > 0) {
     // process data
     inst = process(inst);

     // clear input queue
     flushInput();

     // move it to the output
     for (i = 0; i < inst.numInstances(); i++)
       push(inst.instance(i));
   }

   m_NewBatch       = true;
   m_FirstBatchDone = true;

   return (numPendingOutput() != 0);
 }
	
	
}