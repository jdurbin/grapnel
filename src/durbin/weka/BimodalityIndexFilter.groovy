package durbin.weka;

import durbin.stat.BimodalMixtureModel as BMM

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability
import weka.filters.*;
import jMEF.*;

public class BimodalityIndexFilter extends SimpleBatchFilter {

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
		return result;
	}


	Instances determineOutputFormat(Instances inputFormat) {
		Instances result = new Instances(inputFormat, 0);
		return result;
	}


	Instances process(Instances inst) {
		Instances result = new Instances(determineOutputFormat(inst), 0);

		// Compute the bimodality index for each attribute...
		double[] mean1 = new double[inst.numAttributes()];
		double[] mean2 = new double[inst.numAttributes()];
		attr2bmi = new double[inst.numAttributes()];
		for (int attrIdx = 0; attrIdx < inst.numAttributes(); attrIdx++){
			double[] attrvals = inst.attributeToDoubleArray(attrIdx) 

			MixtureModel mm = BMM.estimateMixtureParameters(attrvals)	
			def m1 = mm.param[0].array[0]
			mean1[attrIdx] = m1
			def m2 = mm.param[1].array[0]
			mean2[attrIdx] = m2			

			double bmi = BMM.bimodalityIndex(mm,attrvals.length)			
			attr2bmi[attrIdx] = bmi;
		}

		for (int i = 0; i < inst.numInstances(); i++) {
			double[] values = new double[result.numAttributes()];

			for (int n = 0; n < inst.numAttributes(); n++){
				def m1 = mean1[n]
				def m2 = mean2[n]

				double y = inst.instance(i).value(n);
				double scaledy = (2*(y-m1))/(m2-m1-1);					
				values[n] = scaledy
			}
			result.add(new Instance(1, values));
		}
		return result;
	}
}