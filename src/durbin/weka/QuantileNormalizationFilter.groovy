package durbin.weka;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;

import durbin.stat.QuantileNormalization;

/****
* Wraps QuantileNormalization in boilerplate to be used as a Weka filter. 
* 
*/ 
public class QuantileNormalizationFilter extends SimpleBatchFilter {
	static{WekaAdditions.enable();}
	static err = System.err
	
	QuantileNormalization qn = new QuantileNormalization()
	
	def setTrainingDistribution(trainingData){
		def allvalues = getAllValues(trainingData)
		qn.compressDistribution(allvalues)
	}
	
	def setNumQuantiles(numQuantiles){
		qn.setNumQuantiles(numQuantiles)
	}
	
	protected Instances process(Instances instances) throws Exception {	
		Instances result = new Instances(determineOutputFormat(instances), 0);

		instances.each{instance->
			newValues = qn.transform(instance.toDoubleArray());
			result.add(new Instance(1, newValues));			
		}
		return result;
	}

	/**
	* Retrieves all of the attribute values across all instances. 
	*/ 
	def getAllValues(instances){
		def allvalues = []
		instances.each{instance->
			allvalues.addAll(instance.toDoubleArray())
		}
		return(allvalues)			
	}

	public String globalInfo() {
		return   "Replaces  "
		+ "containing the index of the processed instance.";
	}	
		
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
//		result.enableAllAttributes();
		result.disableAllAttributes();
		result.enableAllClasses();
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
		result.enable(Capability.MISSING_CLASS_VALUES); // doesn't even need a class value
		result.enable(Capability.MISSING_VALUES);
		return result;
	}

	protected Instances determineOutputFormat(Instances inputFormat) {
		Instances result = new Instances(inputFormat, 0);
		return result;
	}

	
	double countNAN(double[] vals){
		int count = 0;
		for(int i = 0;i < vals.length;i++){
			double val = vals[i];
			if (val == Double.NaN) count++;
		}
		return(count);
	}		
}
