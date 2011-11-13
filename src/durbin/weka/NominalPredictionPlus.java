
package durbin.weka;

import weka.classifiers.evaluation.NominalPrediction;


public class NominalPredictionPlus extends NominalPrediction{
	String instanceName;

	
	public NominalPredictionPlus(double actual, double [] distribution,String iName) {
		super(actual,distribution,1);
		instanceName = iName;
  }

 	public NominalPredictionPlus(double actual, double [] distribution, double weight,String iName) {
		super(actual,distribution,weight);	
		instanceName = iName;
	}
}
