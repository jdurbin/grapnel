
package durbin.weka;

import hep.aida.bin.DynamicBin1D

import weka.core.*
import weka.filters.unsupervised.attribute.Remove
import weka.filters.*
import weka.core.Capabilities
import weka.core.Capabilities.Capability


import weka.attributeSelection.*

import java.util.Enumeration;
import java.util.Vector;

/*
* Ranks attributes based on Fisher's linear discriminant. <br>
* 
* Given two classes, denote instances in class1 as X1, those in class2 as X2. <br><br>
* 
* xbar_kj is average of jth attribute in Xk. <br><br>
* 
* The Fisher score of the jth attribute is given by:<br><br>
*
* F(j) = (xbar_1j - xbar_2j)^2 - (s_1j^2 + s_2j^2)<br><br>
* 
* where <br><br>
* 
* (s_kj)^2 = sum_xinXk (x_j - xbar_kj)^2<br><br>
* 
* That is, the difference in the means squared, over the sum of the variances. <br><br>
*
* By default we use the sample variance in our calculation, but there is a flag <br><br>
* to use the plain variance instead. <br><br>
* 
* AttributeSelectedClassifier can use an ASEvaluation to do the right thing when 
* Eval.crossValidateModel() is used.  ASC first performs the selection in buildClassifier()
* and saves the selection for subsequent use on calls to distributionForInstances(), so that
* it can reduce the dimensionality of each test instance as it's evaluated.
*/

class FisherLDEval
  extends ASEvaluation
  implements AttributeEvaluator{
    
  def fisherScores  
  def mdata
  boolean useSampleVariance = true;
        
  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  String globalInfo() {
    return "FisherLDEval :\n\nEvaluates the worth of an attribute "
        +"by measuring the Fisher linear discriminant between two classes for this attribute.\n\n"
  }
  
  def FisherLDEval() {
    fisherScores = null;
    mdata = null;
    //resetOptions();
  }
  
  /**
   * Returns the capabilities of this evaluator.
   *
   * @return            the capabilities of this evaluator
   * @see               Capabilities
   */
  Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.enableAllAttributes();
    result.enableAllClasses();
     //result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
    return result;
    
    // attributes
    //result.enable(Capability.NOMINAL_ATTRIBUTES);
    //result.enable(Capability.NUMERIC_ATTRIBUTES);
    //result.enable(Capability.DATE_ATTRIBUTES);
    //result.enable(Capability.MISSING_VALUES);
    
    // class
    //result.enable(Capability.NOMINAL_CLASS);
    //result.enable(Capability.MISSING_CLASS_VALUES);
  }
  
  
  
  /**
   * Initializes a fisher LDA attribute evaluator.
   *
   * @param data set of instances serving as training data 
   * @throws Exception if the evaluator has not been 
   * generated successfully
   */
  void buildEvaluator(Instances data){
    
    mdata = data
    
    // can evaluator handle data?
    getCapabilities().testWithFail(data);
    
    // Create objects to compute the quartiles and median cutoffs for each attribute...
    def bins0 = new DynamicBin1D[data.numAttributes()];
    def bins1 = new DynamicBin1D[data.numAttributes()];
    
    // Bin all of the attribute values (probably inefficient, but easy)
    createAttributeBins(data,bins0,bins1)
        
    // compute fisher scores for all of the bins (attributes)...
    fisherScores = computeFisherScores(bins0,bins1)
  }
  
  /**
  * Get score for this attribute. We've already computed fisher scores in bulk during
  * buildEvaluator, so nothing to do here except return the value. 
  */ 
  public double evaluateAttribute (int attributeIdx){
    if (attributeIdx >= fisherScores.length) 
      throw new IllegalStateException("evaluateAttribute: attributeIdx out of bounds.")
      
    return(fisherScores[attributeIdx])
  }
  
  
  /******************************************
 	*  Creates a DynamicBin1D object for each of two classes for each attribute 
 	*  in the instances. From these DynamicBin1D objects, we can compute a 
 	*  variety of attribute statistics. 
 	* 
 	*  Note: Missing values are simply skipped. 
 	*/
 	def createAttributeBins(Instances data,bins0,bins1) {

 		for (int i = 0;i < bins0.length;i++) bins0[i] = new DynamicBin1D();
 		for (int i = 0;i < bins1.length;i++) bins1[i] = new DynamicBin1D();

 		// Go through the instances and create bins for each attribute
 		for (int i = 0;i < data.numInstances();i++) {
 			double[] values = data.instance(i).toDoubleArray()
 			for (int attrIdx = 0; attrIdx < data.numAttributes();attrIdx++){
 			  double value = values[attrIdx];
 			  if (value != Instance.missingValue()){
 			    if (data.instance(i).classValue() == 0){			    
 			      bins0[attrIdx].add(values[attrIdx]); 
 		      }else{
 		        bins1[attrIdx].add(values[attrIdx]); 
 		      }
 			  }else{
 			    //System.err.println("Value for instance: "+i+" attribute "+attrIdx+" is missing.");
 			  }		  
 			}
 		}
 	}

 	/******************************************
 	*  Compute the fisher score for each attribute in bins0,bins1
 	*  Return an array of these scores. 
 	*/
 	def computeFisherScores(bins0,bins1){
     def scores = new double[bins0.length]
//     System.err.println "KJD computeFisherScores"

     (0..<scores.length).each{i->
       def bin0 = bins0[i]
       def bin1 = bins1[i]
       def variance0 = bin0.variance()
       def variance1 = bin1.variance()

       // Mainly to be comparable to other code that uses the variance rather 
       // than sample variance.  X/(n-1) * ((n-1)/n) = X/n 
       if (!useSampleVariance){
         double n0 = bin0.size() as double
         double n1 = bin1.size() as double
         variance0 = variance0*((n0-1.0)/n0)
         variance1 = variance1*((n1-1.0)/n1)
       }

       // Put a minimum on the variance for stability. 
       if (variance0 < 0.1) variance0 = 0.1
       if (variance1 < 0.1) variance1 = 0.1             

       def meanDiff = (bin0.mean() - bin1.mean())
       def score =  (meanDiff*meanDiff)/ (variance0 + variance1)            
       //scores[i] = score.abs  // only interested in magnitude...
       scores[i] = score  

       //def name = mdata.attribute(i).name()
       //System.err.println "KJD\t${bin0.mean()}\t${bin1.mean()}\t${variance0}\t${variance1}\t$name\t${score.abs()}"
     }    
     return(scores)
 	}
  
}