#!/usr/bin/env groovy 

package durbin.weka;

import durbin.weka.InstanceUtils as IU
import durbin.util.*
import durbin.weka.*

import weka.core.*
import weka.filters.*
import weka.filters.unsupervised.attribute.Remove
import weka.filters.unsupervised.instance.RemoveWithValues
import weka.filters.unsupervised.attribute.RemoveType
import weka.filters.unsupervised.attribute.RemoveUseless


import weka.classifiers.*
import weka.classifiers.functions.*
import weka.classifiers.Evaluation
import weka.classifiers.trees.*
import weka.classifiers.misc.*
import weka.classifiers.rules.*
import weka.classifiers.mi.*
import weka.classifiers.lazy.*
import weka.classifiers.functions.suportVector.*
import weka.classifiers.meta.FilteredClassifier;

import weka.attributeSelection.*

/**
*  
*/ 
class WekaMine{
	
	def exp
	def params
	def data
	def clinical
	
	def results
	def eval
		
	//def instances
	static def err = System.err // sugar
	
		
	def WekaMine(data,clinical,exp,params){
		this.data = data // The CNV,EXP, or whatever data
		this.clinical = clinical // Instances that will be used as class attributes. 
		this.exp = exp // A single experiment specification
		this.params = params // Params don't vary across experiments (yet.. plan to replace exp list with a map, and to pack all params in each exp)

		// Add some sugar methods to weka.Instances with Groovy metaclass (e.g. access class by name)
		WekaAdditions.enable()
	}	
	
	/********************************
	* Merges data and single clinical class attribute into a single set of 
	* instances. 
	*/ 
	def createInstancesFromDataAndClinical(data,clinical,classAttribute){
				
 		// Remove all clinical attributes except for the current class...
  	def selectedAttribute = []
  	selectedAttribute.add(classAttribute)

  	def singleClinicalInstances = removeAttributesNotSelected(clinical,selectedAttribute)  

  	// Merge data and clinical files (i.e. instances contained in both, omitting rest)		
  	def merged = IU.mergeNamedInstances(data,singleClinicalInstances)		

		return(merged)
	}
	
	
	/*********************************
	* Cleans up the attributes and instances by removing degenerate cases
	*/ 
	def cleanUpInstances(instances){	
		// Remove any attributes that don't vary at all..
  	instances = removeUselessAttributes(instances)
    
  	// For many class-attributes, negative values are invalid.  
  	if (!params.allowNegativeClass){    
    	instances = removeInstancesWithNegativeClassValues(instances)
  	}

  	// Remove any instances whose class value is missingValue(). 
		// KJD.. use of this method hasn't been tested...
		err.print "Removing instances with missing class value. Before: ${instances.numInstances()}..."
  	instances.deleteWithMissingClass();
		err.println "done.  After: ${instances.numInstances()}"

		return(instances)
	}
	
	/**********************************
	* discretizes the class attribute according to the discretization given in 
	* the experiment specification.
	*/ 
	def discretizeClassAttribute(instances){
	 // Discretize class 
		if (exp.discretization == 'median'){
			err.println "median discretization"		
	    instances = classToMedian(instances,"low","high")        
	  }else if (exp.discretization == 'quartile'){
			err.println "quartile discretization"		
	    instances = classToNominalTopBottomQuartile(instances,"low","high")        
	  }else if (exp.discretization.contains(";")){
			fields = exp.discretization.split(";")
	    lowerBound = fields[0] as double
	    upperBound = fields[1] as double
	    instances = classToNominalFromCutoffs(instances,lowerBound,upperBound,"low","high")
	  }else{
			err.println "UNKNOWN discretization: "+exp.discretization
			return;
		}
		return(instances)
	}
	
	/**************************
	* Create an attribute selected classifier. 
	*/ 
	def createAttributeSelectedClassifier(){

	  def asClassifier
	  if (exp.attributeEval == null) asClassifier = exp.classifier
	  else{

	    // Wrap classifier in an AttributeSelectedClassifier2 (modified from weka to 
	    // provide API to expose the actual attributes selected). 
	    asClassifier = new AttributeSelectedClassifier2();

	    def search = exp.attributeSearch
	    // Only Ranker (? right??) allows an overt numAttributes cutoff. 
	    // At least, BestFirst search does not. 
	    if (search.class == Ranker){
	      search.setNumToSelect(exp.numAttributes);
	    }

	    asClassifier.setClassifier(exp.classifier);
	    asClassifier.setEvaluator(exp.attributeEval);
	    asClassifier.setSearch(exp.attributeSearch);                
	  }
		return(asClassifier)
	}
	
	def appendSummary(out,idx,instances){		
	  WekaPipelineOutput.appendSummaryLine(idx,instances,out,exp,eval)
	}
	
	def appendFeatures(out,idx,instances,maxFeaturesOut){
  	WekaPipelineOutput.appendFeaturesLine(idx,instances,out,exp,eval,maxFeaturesOut)
	}
	
	def appendSamples(out,idx,instances){
  	WekaPipelineOutput.appendSamplesLine(idx,instances,out,exp,eval,results)			                                      
	}
	
		
	//=================================================================================
	// *********                  Lower level functions.                    ***********
	//=================================================================================

	def crossValidateModel(classifier,instances,folds,randomSeed){
		def cvu = new CVUtils()
		// Results contain per-sample information... 
		// eval is a summary description of the results. 
	  results = cvu.crossValidateModel(classifier,instances,folds,randomSeed)
	  eval = cvu.eval
		return(eval)
	}

  /**
  *  Converts the numeric class attribute into 
  */
  Instances classToNominalTopBottomQuartile(data,lowString,highString){
    def classIdx = data.setClassName(exp.classAttribute) // Necessary to get idx, since idx changes 
     
    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setUseMedian(false)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(exp.classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 
    return(discretizedData)
  }

	/**
  *  Converts the numeric class attribute into 
  */
  Instances classToNominalFromCutoffs(data,cutoffLow,cutoffHigh,lowString,highString){
    def classIdx = data.setClassName(exp.classAttribute) // Necessary to get idx, since idx changes 

    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setCutoffs(cutoffLow,cutoffHigh)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(exp.classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 
    return(discretizedData)
  }

  /**
  *  Converts the numeric class attribute into 
  */
  Instances classToMedian(data,lowString,highString){
    def classIdx = data.setClassName(exp.classAttribute) // Necessary to get idx, since idx changes 

    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setUseMedian(true)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(exp.classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 
    return(discretizedData)
  }


	/**
  *  Remove attributes that don't vary. 
  */ 
  Instances removeUselessAttributes(data){
    err.print "Removing useless attributes. Before: ${data.numAttributes()}..."
  
    def remove = new RemoveUseless(); 
    def classIdx = data.setClassName(exp.classAttribute)
    remove.setInputFormat(data)
    def cleanedData = Filter.useFilter(data,remove)
    err.println "done.  After: ${cleanedData.numAttributes()}"
    return(cleanedData)
  }

	 /**
   *  Removes instances whose class value is negative.  
   */ 
   Instances removeInstancesWithNegativeClassValues(Instances data){
     // Remove any instances whose class value is missingValue(). This
     // will include attributes for which -1 is a bogus value and has been marked as such.  
     err.print "Removing instances with negative class value. Before: ${data.numInstances()}..."
     def classIdx = data.setClassName(exp.classAttribute)
     def remove = new RemoveWithValues()
     remove.setAttributeIndex("${classIdx+1}".toString())
     remove.setMatchMissingValues(false)
     remove.setSplitPoint(0.0 as double) // Values smaller than this will be matched.  
     remove.setInputFormat(data)
     def cleanedData = Filter.useFilter(data,remove)
     err.println "done.  After: ${cleanedData.numInstances()}"
     classIdx = cleanedData.setClassName(exp.classAttribute)
     return(cleanedData)
   }

	
	/**
  * Remove all the attributes from data except those named in selectedAttributes
  */ 
  Instances removeAttributesNotSelected(data,selectedAttributes){

    //err.println "SelectedAttributes: $selectedAttributes"

    // Remove all attributes that aren't selected 
    // Must preserve ID, however!!!  So explicitly add it to selected attributes...
    selectedAttributes.add("ID")
    def attrIndicesStr = IU.attributeNames2Indices(data,selectedAttributes)
    err.println "Removing attributes except current class: "+attrIndicesStr
    def remove = new Remove();
    remove.setAttributeIndices(attrIndicesStr);
    remove.setInvertSelection(true); // Remove everything not in this list. 
    remove.setInputFormat(data);
    def selectedAttributeData = Filter.useFilter(data, remove);
    selectedAttributeData.setClassName(exp.classAttribute)
    return(selectedAttributeData)
  }
	
	
  /**
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  static Instances readNumericFromTable(dataFileName){
    // Read data in from table.  Handle missing values as we go.
    err.print "Loading numeric data from $dataFileName..."
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)
    Instances data = loader.read(dataFileName,"\t"){  

      // Lots of different missing value notations...
      if ((it == "") || (it == null) || (it == "NA") || 
         (it == "null") || (it == 'NULL') || 
         (it == "?")){
        return (Instance.missingValue())
      }else{
        return(it as Double)
      }
    }
    //err.println "${data.numInstances()} x ${data.numAttributes()} done."
    return(data)
  }
			
}