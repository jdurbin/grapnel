
package durbin.weka;

import hep.aida.bin.DynamicBin1D

import weka.core.*
import weka.filters.unsupervised.attribute.Remove
import weka.filters.*
import weka.core.Capabilities.*;

/**
*
*             **************** DEPRECATED ******************************
*             **************** DEPRECATED ******************************
*             **************** DEPRECATED ******************************
*             **************** DEPRECATED ******************************
*             **************** DEPRECATED ******************************
* 
* Given two classes, denote instances in class1 as X1, those in class2 as X2.<br><br> 
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
* to use the plain variance instead. 
*
* DEPRECATED:  Use FisherLDEval and AttributeSelectedClassifier instead. <br>
* I mistakenly went down the path of implementing this as a filter rather than as an 
* ASEvaluation. There is no good way to use this class with Evaluate.crossValidateModel()
* because that crossValidateModel ultimately calls the provided classifier on each 
* instance indivually, with no way to transfer train set filtering to test set. 
* AttributeSelectedClassifier, in contrast, first performs the selection in buildClassifier()
* and saves the selection for subsequent use on calls to distributionForInstances(), so that
* it can reduce the dimensionality of each test instance as it's evaluated.  
*
*/
public class FisherCriteriaFilter extends SimpleBatchFilter{
  
  def data
  def useSampleVariance = true // Use sample variance by default.  
  def maxAttributes = 200
  
  def className
  
  def FisherCriteriaFilter(){}
  
  def FisherCriteriaFilter(d){data = d}
  def FisherCriteriaFilter(d,useSampleVariance){this.useSampleVariance = useSampleVariance}
    
  String globalInfo() {
       return   "A filter that chooses the top n attributes based on Fisher's linear discriminant score"
  }
  
  Capabilities getCapabilities() {
       Capabilities result = super.getCapabilities();
       result.enableAllAttributes();
       result.enableAllClasses();
       //result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
       return result;
  }  
  
  /*
  public static Instances useFilter(Instances data,
				    Filter filter) throws Exception {
  
    for (int i = 0; i < data.numInstances(); i++) {
      filter.input(data.instance(i));
    }
    filter.batchFinished();
    Instances newData = filter.getOutputFormat();
    Instance processed;
    while ((processed = filter.output()) != null) {
      newData.add(processed);
    }
    return newData;
  }
  */
  
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null)
      throw new IllegalStateException("No input instance format defined");
    
    if (m_NewBatch) {
      System.err.println "\tinput: NewBatch!"
      resetQueue();
      m_NewBatch = false;
    }

    bufferInput(instance);
    
    // Never happens, because I do everything in one batch...
    //if (isFirstBatchDone()) {
    //  Instances inst = new Instances(getInputFormat());
    //  inst = process(inst);
    //  for (int i = 0; i < inst.numInstances(); i++)
	  //    push(inst.instance(i));
    //  flushInput();
    //}
    return m_FirstBatchDone;
  }
  
  /***************************************
  *  Normally one can simply override process and determineOutputFormat
  *  but I didn't exactly follow or trust what weka was doing there, so 
  *  I have overridden batchFinished as well as input. 
  */   
  boolean batchFinished() throws Exception {
     int         i;
     Instances   inst;

     if (getInputFormat() == null)
       throw new IllegalStateException("No input instance format defined");

     // get data
     inst = new Instances(getInputFormat());
     
     System.err.println "\tbatchFinished getInputFormat returns ${inst.numInstances()} instances."

     // don't do anything in case there are no instances pending.
     // in case of second batch, they may have already been processed
     // directly by the input method and added to the output queue
     if (inst.numInstances() > 0) {
       // process data
       def filteredInst = process(inst);

        // if output format hasn't been set yet, do it now
        //if (!hasImmediateOutputFormat() && !isFirstBatchDone())
       setOutputFormat(new Instances(filteredInst, 0));       

       // clear input queue
       flushInput();

       // move it to the output
       for (i = 0; i < filteredInst.numInstances(); i++){
         push(filteredInst.instance(i));
       }
     }else{
       System.err.println "\tbatchFinished no more instances."
     }

     m_NewBatch       = true;
     m_FirstBatchDone = true;

     System.err.println "\tbatchFinished numPending: ${numPendingOutput()}"

     return (numPendingOutput() != 0);
   }
  
  Instances determineOutputFormat(Instances inst){
    return(getOutputFormat())    
  }
  
  
  

  Instances process(Instances inst){
    data=inst;
    System.err.println "\tprocess attrs: ${inst.numAttributes()} inst: ${inst.numInstances()}..."
    def filteredData = filter(maxAttributes)
    System.err.println "\tafter filter: ${filteredData.numAttributes()} inst: ${filteredData.numInstances()}"
    return(filteredData)
  }
  

  /************************************************
  *  Returns a dataset with the numAttributes attributes 
  *  with highest ranked score by Fisher criteria. 
  */
  def Instances filter(maxAttributes){
    
    // Look up class index... can't filter out the class...
    def classIdx = data.classIndex()
    
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
    System.err.println("\tFisher CUTOFF: $scoreCutoff")
    fisherScores.eachWithIndex{score,i->                  
      def name = data.attribute(i).name()         
      
      if (score <= scoreCutoff){
        if (i != (data.numAttributes()-1)){
          if (i != classIdx) removeList.add(i) // Must preserve class attribute.
        }
      }
    }
    
    System.err.print("\tRemoving "+removeList.size()+" attributes with fisher score < "+scoreCutoff+"...")
    
    // Now remove the attributes we've identified as falling below the cutoff
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
      
      def name = data.attribute(i).name()  
      //System.out.println "KJD\t${bin0.mean()}\t${bin1.mean()}\t${variance0}\t${variance1}\t$name\t${score.abs()}"
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