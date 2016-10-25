package grapnel.weka;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;

import grapnel.stat.QuantileNormalization;
import hep.aida.bin.QuantileBin1D;

import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.apache.commons.math3.stat.ranking.*

/****
* Wraps QuantileNormalization in boilerplate to be used as a Weka filter. 
* 
*/ 
public class QuantileNormalizationFilter extends SimpleBatchFilter {
	static{WekaAdditions.enable();}
	static err = System.err
	
	def quantiles = null;
	
	//QuantileNormalization qn = new QuantileNormalization()
	//def qn = new QuantileBin1D(0.002)
	
	def initializeTrainingDistribution(trainingData){
		computeQuantiles(trainingData)
	}
	
	def setQuantiles(thequantiles){
		quantiles = thequantiles
	}
	
	// withClassInstances may or may not contain class attribute
	protected Instances process(Instances withClassInstances) throws Exception {	
		
		// If a class attribute is set, remove it before filtering....
		AttributeUtils au = new AttributeUtils();
		Instances instances = au.removeClassAttribute(withClassInstances);
		
		Instances result = new Instances(determineOutputFormat(instances), 0);
			
		// If quantiles aren't defined, generate from these instances...
		if (quantiles == null){			
			def names = instances.attributeNames() as Set	
			if (names.contains("ID")){
				def instNames = instances.attributeValues("ID")
				noIDinstances = AttributeUtils.removeInstanceID(instances)
				computeQuantiles(instances)	
				instances = WekaMine.addID(noIDinstances,instNames)			
			}else{
				computeQuantiles(instances)			
			}
		}
		
		instances.eachWithIndex{instance,i->
			//def newValues = qn.transform(instance.toDoubleArray());
			def newValues = transform(instance)
			err.println "Adding ${newValues.size()} values for instance $i"			
			result.add(new DenseInstance(1, newValues));					
		}			
		// If we saved a class attribute, restore it. 
		result = au.restoreClassAttribute(result);			
		return result;		
	}
	
	def computeQuantiles(instances){
		
		// *KJDKJDKJDKJDKJKDKJDKJDKJKDJKJDKJKDJKJDKJD
		// commented out
		// kjd -1 is to remove ID attribute... should test for ID attribute
		// and remove it in advance and replace, or something more overt than
		// this
		//quantiles = new double[instances.numAttributes()-1]
		quantiles = new double[instances.numAttributes()]
		instances.eachWithIndex{instance,i->
			def vals = instance.toDoubleArray()
			//vals = vals[1..-1] as double[]
			Arrays.sort(vals) 
			vals.eachWithIndex{v,j->
				quantiles[j]+=v
			}
		}
		quantiles = quantiles.collect{it/(double)instances.numInstances()}
		return(quantiles)
	}
	
	
	def transform(instance){	
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL,TiesStrategy.MINIMUM);
		
		def vals = instance.toDoubleArray()			
		int[] ranks = ranking.rank(vals) as int[]
		def newvals = new double[vals.size()]
		// ranking returns 1-based ranks, so -1
		ranks.eachWithIndex{sortedIdx,i->
			newvals[i] = quantiles[sortedIdx-1]
		}				
		return(newvals)
	}
	
	/*
	def transform(instance){
		def vals = instance.toDoubleArray()
		def testQuantileBin = new QuantileBin1D(0.002) 
		vals.each{value->
			testQuantileBin.add(value)
		}
				
		def newvals = new double[vals.length]
		vals.eachWithIndex{v,i->
			def testquant = testQuantileBin.quantileInverse(v)
			newvals[i] = qn.quantile(testquant)
		}
		return(newvals)
	}
	*/

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
