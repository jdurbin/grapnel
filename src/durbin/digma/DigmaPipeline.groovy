#!/usr/bin/env groovy 

package durbin.digma;

import durbin.weka.InstanceUtils as IU
import durbin.util.*
import durbin.weka.*

import weka.core.*
import weka.filters.*
import weka.filters.unsupervised.attribute.Remove
import weka.filters.unsupervised.instance.RemoveWithValues
import weka.filters.unsupervised.attribute.RemoveType

import weka.classifiers.*
import weka.classifiers.functions.*
import weka.classifiers.Evaluation;


/****************************************************************************
*  Class to implement top-level steps of the Digma classifier pipeline.  The 
*  goal is to make the pipeline steps more readable and to share a single 
*  debugged instance of these steps.  Not all of these steps will be used
*  in any given pipeline.  This is rather a collection of pipeline steps 
*  that are often used.  
*/ 
class DigmaPipeline{
  def err = System.err
  
  String    className;

  def DigmaPipeline(className){
    this.className = className
    WekaAdditions.enable()
  }

  /******************************************
  *  Creae a classifier from the command-line classifier specification
  *  string.  For example:
  * 
  *  weka.classifiers.functions.SMO -C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -M
  */
  def classifierFromSpec(classifierSpec){
    // Create a classifier from the name...
    def options = Utils.splitOptions(classifierSpec)
    def classifierName = options[0]
    options[0] = ""
    def descString = "$classifierName $options" as String  
    def classifier = Classifier.forName(classifierName,options) 
    return(classifier)
  }

  /*****************************************
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  Instances readFromTable(dataFileName){
    // Read data in from table.  Handle missing values as we go.
    err.print "Loading $dataFileName..."
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)
    Instances data = loader.read(dataFileName,"\t"){
      if ((it == "") || (it == null)){
        return (Instance.missingValue())
      }else{
        return(it as Double)
      }
    }
    err.println "${data.numInstances()} x ${data.numAttributes()} done."
    return(data)
  }

  /*****************************************
  * Remove the ID attribute.  Note, in general this should be done 
  * via a FilteredClassifier in order to preserve ID mapping to predictions, 
  * but sometimes if we don't need predictions this is OK. 
  */ 
  Instances removeID(Instances data){
    // Remove ID attribute, since classifiers can't handle string...
    err.print "Removing string attribute..."
    def remove = new RemoveType();
    remove.setInputFormat(data);
    def idFreeData = Filter.useFilter(data,remove)
    err.println "done."
    def classIdx = idFreeData.setClassName(className)
    return(idFreeData)
  }  

  /******************************************
  *  Removes instances whose class value is missing.  Should probably
  *  replace with more generic version below...
  */ 
  Instances removeInstancesWithMissingClassValues(Instances data){
    // Remove any instances whose class value is missingValue(). This
    // will include attributes for which -1 is a bogus value and has been marked as such.  
    err.print "Removing instances with missing class value. Before: ${data.numInstances()}..."
    def classIdx = data.setClassName(className)
    def remove = new RemoveWithValues()
    remove.setAttributeIndex("${classIdx+1}".toString())
    remove.setMatchMissingValues(true) 
    remove.setInputFormat(data)
    def cleanedData = Filter.useFilter(data,remove)
    err.println "done.  After: ${cleanedData.numInstances()}"
    classIdx = cleanedData.setClassName(className)
    return(cleanedData)
  }


  /******************************************
  *  Removes instances whose value of attribute attrIdx is missing. 
  */ 
  Instances removeInstancesWithMissingValues(Instances data,attrName){
    
    def attrIdx = -1;
    (0..<data.numAttributes()).each{i->
      Attribute attribute = data.attribute(i);
      def name = attribute.name()
      if (name == attrName) attrIdx = i
    }
    
    // Remove any instances whose class value is missingValue(). This
    // will include attributes for which -1 is a bogus value and has been marked as such.  
    err.print "Removing instances with missing value. Before: ${data.numInstances()}..."
    def remove = new RemoveWithValues()
    remove.setAttributeIndex("${attrIdx+1}".toString())
    remove.setMatchMissingValues(true) 
    remove.setInputFormat(data)
    def cleanedData = Filter.useFilter(data,remove)
    err.println "done.  After: ${cleanedData.numInstances()}"
    return(cleanedData)
  }

  /******************************************
  *  Converts the numeric class attribute into 
  */
  Instances classToNominalFromCutoffs(data,cutoffLow,cutoffHigh,lowString,highString){
    def classIdx = data.setClassName(className) // Necessary to get idx, since idx changes 
     
    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setCutoffs(cutoffLow,cutoffHigh)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(className)
    err.println "done."  // KJD report how many high/low as a sanity check. 
    return(discretizedData)
  }

  Instances removeAttributesNotSelected(data,selectedAttributes){
    // Remove all attributes that aren't selected 
    // Must preserve ID, however!!!
    selectedAttributes.add("ID")
    def attrIndicesStr = IU.attributeNames2Indices(data,selectedAttributes)
    //err.println "Removing attributes not in set: "+attrIndicesStr
    def remove = new Remove();
    remove.setAttributeIndices(attrIndicesStr);
    remove.setInvertSelection(true); // Remove everything not in this list. 
    remove.setInputFormat(data);
    def selectedAttributeData = Filter.useFilter(data, remove);
    selectedAttributeData.setClassName(className)
    return(selectedAttributeData)
  }

  /************************************************
  * Marks all negative instances of the given attribute as missing value. 
  * WARNING: Unlike most of these steps, this does not return a copy. 
  * Hmmm... perhaps I should...
  */ 
  Instances markNegativeValuesAsMissing(data,attributeName){
    Attribute attribute = data.attribute(attributeName)
    (0..<data.numInstances()).each{instIdx->
      def instance = data.instance(instIdx)
      def value = instance.value(attribute)
      if (value < 0) instance.setValue(attribute,Instance.missingValue())
    }
    return(data)
  }


  /****************************************************************************
  *                     SPECIAL/INFREQUENTLY USED                             *
  *****************************************************************************/


  /*********************************************
  * Creates a dataset with one attribute, attrIdx, and the class value. 
  * 
  */ 
  Instances removeAllAttributesExceptClassAnd(data,attrIdx){
    def classIdx = data.setClassName(className) 

    err.print "Removing all other attributes..."
    def remove = new Remove()
    def removeStr = "${attrIdx+1},${classIdx+1}".toString()
    remove.setAttributeIndices(removeStr);
    remove.setInvertSelection(true);
    remove.setInputFormat(data);
    def singleAttributeData = Filter.useFilter(data, remove); 
    singleAttributeData.setClassName(className)
    err.print "done...."
    return(singleAttributeData)
  }
  
  

}