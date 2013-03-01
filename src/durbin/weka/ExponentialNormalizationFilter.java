package durbin.weka;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;

import org.apache.commons.math.stat.ranking.*;
import org.apache.commons.math.distribution.*;

public class ExponentialNormalizationFilter extends SimpleBatchFilter {

	static{WekaAdditions.enable();}
	//static err = System.err

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
	
	/**
	* 
	* rankNA <- function(col)
	* { col[!is.na(col)]<-(rank(col[!is.na(col)])/sum(!is.na(col)))-(1/sum(!is.na(col)))
	* 	col
	* }
	* b<-apply(data,2,rankNA)
	* c<-apply(b, c(1,2),qexp)
	*/ 
	protected Instances process(Instances instances) throws Exception {
	
		Instances result = new Instances(determineOutputFormat(instances), 0);
		ExponentialDistributionImpl exp = new ExponentialDistributionImpl(1.0);
		
		// Save rank lists for each attribute...
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.FIXED,TiesStrategy.MAXIMUM);						

		for(int i = 0;i < instances.numInstances();i++){
		//for(i in 0..< instances.numInstances()){			
			double[] newValues = new double[instances.numAttributes()]; // create space for new values
			Instance inst = instances.instance(i);
			
			// rank the attribute values...
			double[] attrvals = inst.toDoubleArray(); 											
			double [] attrranks = ranking.rank(attrvals);
			double attsum = attrranks.length - countNAN(attrvals);
			double invattsum = (double)(1.0/attsum);
			
			for (int a = 0; a < instances.numAttributes(); a++){
				double oldval = attrvals[a];
				double rank = attrranks[a];
				double scaled = rank*invattsum - invattsum;
				double newVal = Math.abs(exp.inverseCumulativeProbability(Math.abs(scaled)));
				newValues[a] = newVal;
			}
			result.add(new Instance(1, newValues));
		}
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