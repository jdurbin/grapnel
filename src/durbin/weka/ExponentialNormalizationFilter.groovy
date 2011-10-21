package durbin.weka;

import durbin.stat.BimodalMixtureModel as BMM

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability
import weka.filters.*;
import jMEF.*;

import org.apache.commons.math.stat.ranking.*
import org.apache.commons.math.distribution.*

//***********************************************************************
//***************  NOT FUNCTIONING YET... BEING WRITTEN *****************
//***********************************************************************

public class ExponentialNormalizationFilter extends SimpleBatchFilter {

	double[] attr2bmi;
	double 	 bimodalityThreshold = 0;

	static{WekaAdditions.enable()}
	static err = System.err

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
		result.enable(Capability.MISSING_VALUES);
		return result;
	}

	Instances determineOutputFormat(Instances inputFormat) {
		Instances result = new Instances(inputFormat, 0);
		return result;
	}
	
	/**
	* 
	* rankNA <- function(col)
	* { col[!is.na(col)]<-(rank(col[!is.na(col)])/sum(!is.na(col)))-(1/sum(!is.na(col)))
	* 	col
	* }
	* b<-apply(data,2,rankNA)
	* c<-apply(b, c(1,2),qexp)
	*/ 
	Instances process(Instances instances) {
	
		Instances result = new Instances(determineOutputFormat(instances), 0);
		def exp = new ExponentialDistributionImpl(1.0);
		
		// Save rank lists for each attribute...
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.FIXED,TiesStrategy.MAXIMUM);						

		for(i in 0..< instances.numInstances()){			
			double[] newValues = new double[instances.numAttributes()]; // create space for new values
			Instance inst = instances.instance(i)
			
			// rank the attribute values...
			double[] attrvals = inst.toDoubleArray() 											
			double [] attrranks = ranking.rank(attrvals)
			
			for (int a = 0; a < instances.numAttributes(); a++){
				double oldval = attrvals[a]
				def rank = attrranks[a]
				def attsum = attrranks.length - countNAN(attrranks)					
				double scaled = (rank/attsum) - (1/(attsum))	
				newValues[a] = exp.inverseCumulativeProbability(scaled)
			}
			result.add(new Instance(1, newValues));
		}
		return result;
	}
	
	def countNAN(vals){
		def count = 0;
		vals.each{
			if (it == Double.NaN) count++
		}
		return(count)
	}
	
		
}