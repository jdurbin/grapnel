package durbin.weka;

import weka.classifiers.meta.*;
import weka.attributeSelection.*;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.SingleClassifierEnhancer;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
* Extends the default weka AttributeSelectedClassifier in order to expose, what 
* else?, the AttributeSelection.  Why the weka people didn't do this already
* is a mystery.  KJD: Can I do this with an expando meta-class?  
*/
public class AttributeSelectedClassifier2 extends AttributeSelectedClassifier{
    
  //String m_SpecialExperiment = null;	
	
  /***  
  */ 
  public AttributeSelection getAttributeSelection(){
    return(m_AttributeSelection);
  }
  

  /**
   * Build the classifier on the dimensionally reduced data.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {
	 //System.err.println("\t\t\t\t\t\tAttribute selected classifier.");
	  
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }

    if (m_Evaluator == null) {
      throw new Exception("No attribute evaluator has been set!");
    }

    if (m_Search == null) {
      throw new Exception("No search method has been set!");
    }
   
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

//	System.err.print("\t\t\t\t\tRemove with missing class...");
    // remove instances with missing class
    Instances newData = new Instances(data);
//	System.err.print("\t\t\t\t\tcopy done");
    newData.deleteWithMissingClass();
//	System.err.println("\t\t\t\t\t delete done.");
    
    if (newData.numInstances() == 0) {
      m_Classifier.buildClassifier(newData);
      return;
    }
    if (newData.classAttribute().isNominal()) {
      m_numClasses = newData.classAttribute().numValues();
    } else {
      m_numClasses = 1;
    }

    Instances resampledData = null;
    // check to see if training data has all equal weights
    double weight = newData.instance(0).weight();
    boolean ok = false;
    for (int i = 1; i < newData.numInstances(); i++) {
      if (newData.instance(i).weight() != weight) {
        ok = true;
        break;
      }
    }
    
    if (ok) {
      if (!(m_Evaluator instanceof WeightedInstancesHandler) || 
          !(m_Classifier instanceof WeightedInstancesHandler)) {
        Random r = new Random(1);
        for (int i = 0; i < 10; i++) {
          r.nextDouble();
        }
        resampledData = newData.resampleWithWeights(r);
      }
    } else {
      // all equal weights in the training data so just use as is
      resampledData = newData;
    }
	
	// SINGLE CELL SIMULATION 
	// Handle special case for simulation. 
	//if (m_SpecialExperiment != null){
	//	mAttributeSelection = new SingleCellSimulationSelection(m_SpecialExperiment);				
	//}else{
	//	m_AttributeSelection = new AttributeSelection();
	//}
	
	m_AttributeSelection = new AttributeSelection();
    m_AttributeSelection.setEvaluator(m_Evaluator);
    m_AttributeSelection.setSearch(m_Search);
//	System.err.println("\t\t\t\t\tSelect attributes with "+m_Evaluator.toString());
    long start = System.currentTimeMillis();
    m_AttributeSelection.
      SelectAttributes((m_Evaluator instanceof WeightedInstancesHandler) 
                       ? newData
                       : resampledData);
    long end = System.currentTimeMillis();
//	System.err.println("\t\t\t\t\t done Attribute selection.");
    if (m_Classifier instanceof WeightedInstancesHandler) {
      newData = m_AttributeSelection.reduceDimensionality(newData);
      m_Classifier.buildClassifier(newData);
    } else {
      resampledData = m_AttributeSelection.reduceDimensionality(resampledData);
      m_Classifier.buildClassifier(resampledData);
    }

    long end2 = System.currentTimeMillis();
    m_numAttributesSelected = m_AttributeSelection.numberAttributesSelected();
    m_ReducedHeader = 
      new Instances((m_Classifier instanceof WeightedInstancesHandler) ?
                    newData
                    : resampledData, 0);
    m_selectionTime = (double)(end - start);
    m_totalTime = (double)(end2 - start);
//	System.err.println("\t\t\t\t\t completely done Attribute selection.");
  }
}