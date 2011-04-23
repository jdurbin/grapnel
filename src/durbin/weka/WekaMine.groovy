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
import weka.filters.supervised.attribute.*

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
* wekaMine is a collection of scripts, java library functions, and modifications to weka to 
* enable large scale exploration of machine learning algorithms and their parameters.   While 
* written for working with cancer genomics datasets, wekaMine is not specific to cancer or genomics.    
* 
* Most of the methods in the WekaMine class are just sugar for weka methods.  The major goal of the class is to 
* standardize basic processing steps in a machine learning pipeline. 
* 
* Here is a simplified example of a basic pipeline implemented with WekaMine:
* 
* <pre>
* experiments = new WekaMineConfig(configName)
*
* experiments.each{exp-> 
*	// Creates a wekaMine pipeline...
*	pipeline = new WekaMine(data,clinical,exp,experiments.params)		
*	
*	// Combines data and single class attribute from clinical into one set of instances...
*	instances = pipeline.createInstancesFromDataAndClinical(data,clinical,exp.classAttribute)	
*	
*	// Clean up instances:
*	// * remove useless attributes
*	// * if not allowed, remove instances with negative class values
*	// * remove instances with missing class values	
*	instances = pipeline.cleanUpInstances(instances)
*
*	// Discretize the class attribute...
*	instances = pipeline.discretizeClassAttribute(instances)	
*
*	// Create an attribute selected classifier from the given experiment description..
*	asClassifier = pipeline.createAttributeSelectedClassifier()
*	
*	// Perform the cross validation
*	def eval = pipeline.crossValidateModel(asClassifier,instances,experiments.params.cvFolds,new Random(experiments.params.cvSeed))
*	
*	pipeline.appendSummary(out,jobIdx,instances)
*	pipeline.appendFeatures(fout,jobIdx,instances,options.maxFeaturesOut as Integer)
*	pipeline.appendSamples(sout, jobIdx,instances)
*	
*}
*</pre>
*/ 
class WekaMine{
	
	def exp
	def params
	def data
	def clinical
	
	def results
	def eval
	
	static{
		WekaAdditions.enable()
	}
	
		
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
	
	//=================================================================================
	// *********                  Pipeline Steps.                          ***********
	//=================================================================================

	
	/***
	* Merges data and single clinical class attribute into a single set of 
	* instances. 
	*/ 
	static def createInstancesFromDataAndClinical(data,clinical,classAttribute){
				
 		// Remove all clinical attributes except for the current class...
  	def selectedAttribute = []
  	selectedAttribute.add(classAttribute)

  	def singleClinicalInstances = subsetAttributes(clinical,selectedAttribute)  
		singleClinicalInstances.setClassName(classAttribute)
		
  	// Merge data and clinical files (i.e. instances contained in both, omitting rest)		
  	def merged = IU.mergeNamedInstances(data,singleClinicalInstances)		

		return(merged)
	}
	
	/***
	* Removes and/or adds attributes to make the resulting set of instances
	* match those specified in the attribute list (e.g. the attributes retrieved
	* from a trained model).  
	*/ 
	static def createInstancesToMatchAttributeList(rawdata,modelAttributes){

		def rawAttributeNames = rawdata.attributeNames()
		def rawAttributeNamesSet = rawAttributeNames as Set
		def rawName2Idx = [:]
		rawAttributeNames.eachWithIndex{n,i->rawName2Idx[n] = i}


		// Create a set of instances reflecting the model data...
		def atts = new FastVector()
		modelAttributes.each{attrName-> 
			atts.addElement(new Attribute(attrName))
		}
		def data = new Instances("NewInstances",atts,0)


		def numInstances = rawdata.numInstances()
		for(int i = 0;i < numInstances;i++){

				// Get the values from the new data...
				def instance = rawdata.instance(i)
				double[] rawVals = instance.toDoubleArray()

				// Create space for the new values...
				double[] vals = new double[data.numAttributes()];

				// For each attribute...
				modelAttributes.eachWithIndex{modelAttrName,aIdx->
					if (rawAttributeNamesSet.contains(modelAttrName)){
						def rawIdx = rawName2Idx[modelAttrName]
						vals[aIdx] = rawVals[rawIdx]
					}else{
						vals[aIdx] = Instance.missingValue();
					}
				}
				data.add(new Instance(1.0,vals));
		}
		return(data)
	}
	
	
	/***
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
	
	/***
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
	
	/***
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
	    asClassifier.setSearch(search);                
	  }
		return(asClassifier)
	}
	
	def appendSummary(out,idx,instances){		
	  WekaMineResults.appendSummaryLine(idx,instances,out,exp,eval)
	}
	
	def appendFeatures(out,idx,instances,maxFeaturesOut){
  	WekaMineResults.appendFeaturesLine(idx,instances,out,exp,eval,maxFeaturesOut)
	}
	
	def appendSamples(out,idx,instances){
  	WekaMineResults.appendSamplesLine(idx,instances,out,exp,eval,results)			                                      
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

  /***
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
	* Remove string attributes.  Specifically the string attribute that contains 
	* the instance ID.  This is done because most attribute evaluators and classifiers
	* can not handle string attributes. 
	*/ 
	Instances removeInstanceID(instances){
		def filter = new RemoveType()
		filter.setInputFormat(instances);
		def instances_notype = Filter.useFilter(instances,filter)
		return(instances_notype)
	}

	/**
	* Apply an attribute selection algorithm (attributeEvaluator + attributeSearch) to the
	* instances and return the reduced set of attributes. 
	*/ 
	Instances selectAttributes(instances){
		err.print("Select attributes with ${exp.attrEvalStr}.  Before: ${instances.numAttributes()} ...")
		AttributeSelection atSel = new weka.filters.supervised.attribute.AttributeSelection(); 
		atSel.setEvaluator(exp.attributeEval);
		def search = exp.attributeSearch
		// Only Ranker (? right??) allows an overt numAttributes cutoff. 
		// At least, BestFirst search does not. 
		if (search.class == Ranker){
			search.setNumToSelect(exp.numAttributes);
		}				
		atSel.setSearch(search);
		atSel.setInputFormat(instances);
		
		// generate new data
	 	def instances_atsel = Filter.useFilter(instances, atSel);
		err.println("done. After: ${instances_atsel.numAttributes()}")

		return(instances_atsel)
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
 	static Instances subsetAttributes(Instances data,ArrayList selectedAttributes){

    //err.println "SelectedAttributes: $selectedAttributes"

    // Remove all attributes that aren't selected 
    // Must preserve ID, however!!!  So explicitly add it to selected attributes...
    selectedAttributes.add("ID")
    def attrIndicesStr = IU.attributeNames2Indices(data,selectedAttributes)
    def remove = new Remove();
    remove.setAttributeIndices(attrIndicesStr);
    remove.setInvertSelection(true); // Remove everything not in this list. 
    remove.setInputFormat(data);
    def selectedAttributeData = Filter.useFilter(data, remove);
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

	/**
  *  Creae a classifier from the command-line classifier specification
  *  string.  For example:<br><br>
  * 
  *  weka.classifiers.functions.SMO -C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -M
  */
  static def classifierFromSpec(classifierSpec){
    // Create a classifier from the name...
    def options = Utils.splitOptions(classifierSpec)
    def classifierName = options[0]
    options[0] = ""

    //System.err.println "classifierName: $classifierName"
    //System.err.println "options: $options"

    def classifier = Classifier.forName(classifierName,options) 
    return(classifier)
  }

	static def filteredClassifierFromClassifier(classifier){
	 // Create a classifier from the name...
    // By using filtered classifer to remove ID, the cross-validation
    // wrapper will keep the original dataset and keep track of the mapping 
    // between the original and the folds (minus ID). 
    def filteredClassifier = new FilteredClassifier()
    def removeTypeFilter = new RemoveType();  

    filteredClassifier.setClassifier(classifier)
    filteredClassifier.setFilter(removeTypeFilter)
		return(filteredClassifier)
	}

  /**
  *  Create a attribute evaluation from the command-line evaluation specification
  *  string.  For example:<br><br>
  * 
  *  weka.attributeSelection.InfoGainAttributeEval
  */
  static def evalFromSpec(attributeEvalSpec){    
    // Create a classifier from the name...
    def options = Utils.splitOptions(attributeEvalSpec)
    def evalName = options[0]
    options[0] = ""
    def classifier = ASEvaluation.forName(evalName,options) 
    return(classifier)
  }
 
	/**
   *  Creae a attribute evaluation from the command-line evaluation specification
   *  string.  For example:<br><br>
   * 
   *  weka.attributeSelection.InfoGainAttributeEval
   */
   static def searchFromSpec(attributeSearchSpec){   
     //System.err.println("attributeSearchSpec: $attributeSearchSpec") 
     // Create a classifier from the name...
     def options = Utils.splitOptions(attributeSearchSpec)
     def searchName = options[0]
     options[0] = ""
     //System.err.println("searchName: $searchName")
     //System.err.println("options: $options")
     def search = ASSearch.forName(searchName,options) 
     return(search)
   }

			
}