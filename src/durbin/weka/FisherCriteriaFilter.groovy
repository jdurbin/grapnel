
import hep.aida.bin.DynamicBin1D

import weka.core.*
import weka.filters.unsupervised.attribute.Remove
import weka.filters.*


/***************************************************
*
* Given two classes, denote instances in class1 as X1, those in class2 as X2. 
* 
* xbar_kj is average of jth attribute in Xk. 
* 
* The Fisher score of the jth attribute is given by:
*
* F(j) = (xbar_1j - xbar_2j)^2 - (s_1j^2 + s_2j^2)
* 
* where 
* 
* (s_kj)^2 = sum_xinXk (x_j - xbar_kj)^2
* 
* That is, the difference in the means squared, over the sum of the variances. 
*
* By default we use the sample variance in our calculation, but there is a flag
* to use the plain variance instead. 
*
*/
public class FisherCriteriaFilter{
  
  def data
  def useSampleVariance = true // Use sample variance by default.  
  
  def FisherCriteriaFilter(d){data = d}
  def FisherCriteriaFilter(d,useSampleVariance){this.useSampleVariance = useSampleVariance}
    
  /************************************************
  *  Returns a dataset with the numAttributes attributes 
  *  with highest ranked score by Fisher criteria. 
  */
  def filter(maxAttributes){
    
    // Create objects to compute the quartiles and median cutoffs for each attribute...
  	def bins0 = new DynamicBin1D[data.numAttributes()];
  	def bins1 = new DynamicBin1D[data.numAttributes()];
    
    // Bin all of the attribute values (probably inefficient, but easy)
    createAttributeBins(data,bins0,bins1)
    
    // compute fisher scores for all of the bins...
    def fisherScores = computeFisherScores(bins0,bins1)
    def scoreCutoff = computeCutoff(fisherScores,maxAttributes)    
    
    // Make a list of instances that fall below the cutoff
    def removeList = new ArrayList()
    System.err.println("CUTOFF: $scoreCutoff")
    fisherScores.eachWithIndex{score,i->                  
      def name = data.attribute(i).name()      
      
      if (score <= scoreCutoff){
        if (i != (data.numAttributes()-1)){
          removeList.add(i)
//          System.err.println("\tREMOVE: $name\t$score")
        }
      }else{
//        System.err.println("\tKEEP: $name\t$score")
      }
    }
    
    System.err.print("Removing "+removeList.size()+" attributes with fisher score < "+scoreCutoff+"...")
    
    // Now remove the instances we've identified as falling below the cutoff
    Remove remove = new Remove();
    remove.setAttributeIndicesArray(removeList as int[])   
    remove.setInputFormat(data);
    def dataNew = Filter.useFilter(data, remove);
    
    System.err.println("done.")
    
    return(dataNew)
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
      
      def score = (bin0.mean() - bin1.mean()) / (variance0 + variance1)            
      scores[i] = score.abs()  // only interested in magnitude...
      
      //def name = data.attribute(i).name()  
      //System.err.println "KJD\t${bin0.mean()}\t${bin1.mean()}\t${variance0}\t${variance1}\t$name\t${score.abs()}"      
    }    
    return(scores)
	}
	
	
	/******************************************
	* Find the cutoff such that maxAttributes will have score < cutoff
	*/ 
	def computeCutoff(fisherScores,maxAttributes){
	  def fs = fisherScores as List // Convert to list so we can sort...
	  def sortedScores = (fs.sort()).reverse() // want highest to lowest
	  
	  if (maxAttributes >= sortedScores.size()) maxAttributes = sortedScores.size() -1
	  
    def maxScore = sortedScores[maxAttributes]
	  return(maxScore)
	}
}