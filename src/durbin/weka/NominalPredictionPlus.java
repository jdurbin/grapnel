
package durbin.weka;

import weka.classifiers.evaluation.NominalPrediction;


public class NominalPredictionPlus extends NominalPrediction{
	String instanceName;
	String actualClassValue = "NA";
	
	public NominalPredictionPlus(double actual, String actualName,double [] distribution,String iName) {		
		super(actual,distribution,1);
		actualClassValue = actualName;
		instanceName = iName;
  }

 	public NominalPredictionPlus(double actual, String actualName,double [] distribution, double weight,String iName) {
		super(actual,distribution,weight);	
		actualClassValue = actualName;
		instanceName = iName;
	}	
	
	// These two constructors are still here for the version of evaluateModelOnceAndRecordPrediction that takes 
	// in a distribution instead of a classifier.  I think it may be unnecessary for my purposes. 
	public NominalPredictionPlus(double actual, double [] distribution,String iName) {
		super(actual,distribution,1);
		instanceName = iName;
  }

 	public NominalPredictionPlus(double actual, double [] distribution, double weight,String iName) {
		super(actual,distribution,weight);	
		instanceName = iName;
	}
}
