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
import weka.filters.supervised.instance.*

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
import weka.filters.unsupervised.instance.*;
import weka.filters.unsupervised.attribute.NumericToNominal

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
*	pipeline.APPENDsAMPLEs(sout, jobIdx,instances)
*	
*}
*</pre>
*/ 
class WekaMine{
	
	def exp
	def params
	def data
	def clinical
	
	def dataName
	
	// No longer parsing results from forPredictionsPrinting
	//List<EvaluationResult> testResults
	//List<EvaluationResult> trainResults
		
	// Evaluation2 is a modified version of weka Evaluation that saves
	// the features selected in a cross-validation experiment for 
	// later access. 
	Evaluation2 eval;
	
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

	static addID(instances,instanceNames){
		instances.insertAttributeAt(new Attribute("ID",(FastVector)null),0)
		int attrIdx = instances.attribute("ID").index(); // Paranoid... should be 0
		
		instanceNames.eachWithIndex{name,i->
			instances.instance(i).setValue(attrIdx,name)
		}
		return(instances);
	}
	
	
	/*
	* KJD:  This doesn't seem to be used anywhere.  Also, it only works for numeric
	* attributes.... need something different for nominal attributes. 
	*/ 
	static addAttributeAt(instances,attributeValues,attributeName){
		instances.insertAttributeAt(new Attribute(attributeName,(FastVector)null),0)
		int attrIdx = instances.attribute(attributeName).index(); // Paranoid... should be 0
		
		attributeValues.eachWithIndex{value,i->
			instances.instance(i).setValue(attrIdx,value)
		}
		return(instances);
	}

	/***
	* takes a set of instances and creates a tab file from them...
	*/ 
	static saveDataFromInstances(fileName,instances){
		
		err.print "Saving data to $fileName..."
				
		// See if this speeds things up at all..
		def st = new SaveTab();
		st.saveDataFromInstances(fileName,instances) 
		err.println "done."
	}
	
	
	/***
	* takes a set of instances and creates a tab file from them...
	*
	* deprecated for faster version in SaveTab.java
	*/ 
	static saveDataFromInstances2(fileName,instances){
		
		err.print "Saving data to $fileName..."
					
		new File(fileName).withWriter(){w->
		
			def instNames = instances.attributeValues("ID")
				
			def line = "Features\t${instNames.join('\t')}"
			w.write(line)
		 	w << "\n"

			def className
			if (instances.classIndex() < 0) className = "none"
			else className = instances.className()						
								
			def attrNames = instances.attributeNames()
			attrNames.each{attName->
				if ((attName != "ID") && (attName != className)){
					def atvalues = instances.attributeValues(attName)
					def atvalueStrings = atvalues.collect{"$it"} // convert them to strings...	
					line = "$attName\t${atvalueStrings.join('\t')}\n"
				 	w << line
				}
			}
		}
		err.println "done."
	}
		
	def createInstancesFromDataAndClinical(data,clinical){
		return(createInstancesFromDataAndClinical(data,clinical,exp.classAttribute))
	}
	
	
	
	/***
	* Merges data and single clinical class attribute into a single set of 
	* instances. 
	*/ 
	static def createInstancesFromDataAndClinical(data,clinical,classAttribute){		
			
		err.print "Creating instances from data and clinical..."
				
 		// Remove all clinical attributes except for the current class...
  	def selectedAttributes = []
  	selectedAttributes.add(classAttribute)

		selectedAttributes.add("ID") // Want to preserve the ID along with classAttribute  KJD
  	def singleClinicalInstances = subsetAttributes(clinical,selectedAttributes)  
		singleClinicalInstances.setClassName(classAttribute)
		
		//err.println "DEBUG singleClinicalInstances.size() = "+singleClinicalInstances.numInstances()
		//err.println "DEBUG  data.size() = "+data.numInstances()		
		
  	// Merge data and clinical files (i.e. instances contained in both, omitting rest)		
  	def merged = IU.mergeNamedInstances(data,singleClinicalInstances)		

    //err.println "DEBUG merged: "+merged.numInstances()

		def classIdx = merged.setClassName(classAttribute)

		err.println "done."

		return(merged)
	}
	
	/***
	* Creates a class attribute where all the values are Instance.missingValue()
	* This gives an appropriate model for classifiers to use (namespace, etc.)
	*/ 
	static def createEmptyClassAttribute(data,attributeName,attributeValues){
		def newData = new Instances(data);
		def values = new FastVector()
		attributeValues.each{values.addElement(it)}
		newData.insertAttributeAt(new Attribute(attributeName, values), newData.numAttributes());
		for (int i = 0; i < newData.numInstances(); i++) {
			newData.instance(i).setValue(newData.numAttributes() - 1,Instance.missingValue());
		}	
		return(newData);
	}
	
	
	/***
	* Removes and/or adds attributes to make the resulting set of instances
	* match those specified in the attribute list (e.g. the attributes retrieved
	* from a trained model).  In some cases rawdata has an ID, in some cases 
	* not, have to handle that as a special case...
	*/ 
	static def createInstancesToMatchAttributeList(rawdata,modelAttributes){

		err.print "Make data match model attributes...Initial attributes: ${rawdata.numAttributes()}..."

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

				//println "\nOLD INSTANCE ELAC1: ${instance['ELAC1']} NUP205: ${instance['NUP205']}"					
				def newInstance = new Instance(1.0,vals)
				data.add(newInstance);
		}		

		//data.each{newInstance->
		//	println "NEW INSTANCE ELAC1: ${newInstance['ELAC1']} NUP205: ${newInstance['NUP205']}}"	
		//}
		
		
		/*
		// If we were given an ID, put it back...
		def attrNames = rawdata.attributeNames() as Set
		if (attrNames.contains("ID")){
			def instanceNames = rawdata.attributeValues("ID")
			addID(data,instanceNames)
		}	
		*/			
		
		err.println "done. Final attributes: ${data.numAttributes()}"
		
		return(data)
	}
	
	/***
	* Cleans up the attributes and instances by removing degenerate cases
	*/ 
	static def cleanUpInstances(instances){	
		// Remove any attributes that don't vary at all..
  	instances = removeUselessAttributes(instances)
    
  	// Remove any instances whose class value is missingValue(). 
		// KJD.. use of this method hasn't been tested...
		err.print "Removing instances with missing class value. Before: ${instances.numInstances()}..."
  	instances.deleteWithMissingClass();
		err.println "done.  After: ${instances.numInstances()}"

		return(instances)
	}
	
	
	/***
	* Write out a list of lines, one for each cluster job, ready for Parasol or gsub or whatever.
	*/ 
	static def writeClusterJobListStdout(args,numExperiments,experimentsPerJob,scriptFile,outRoot){
		
		
		// Remove the -k option, keep all the rest...
		def newargs = []
		def bRemoveNext = false;
		args.each{arg->
			if ((!arg.contains("-k")) && !bRemoveNext){
				newargs << arg
			}else{
				bRemoveNext = true;
			}
		}
					
		def rootCmd = "${scriptFile} ${newargs.join(" ")}"
		def expPerJob = Double.parseDouble(experimentsPerJob) // convert string value to double
		def numJobs = (double)numExperiments/(double)expPerJob				
		//System.err.println("numExperiments: $numExperiments  numJobs: $numJobs")
				
		int jobStart = 0
		int jobEnd = expPerJob -1;
		(0..<numJobs).each{
			//System.err.println "0..<$numJobs: jobStart: $jobStart\tjobEnd: $jobEnd"
			def outName = "${outRoot}_${jobStart}_${jobEnd}"
			def cmdOut = "${rootCmd} -r $jobStart,$jobEnd > ${outName}"
		  println cmdOut
		  jobStart += expPerJob
		  jobEnd += expPerJob				
		}

		// Handle remainder experiments..

		def lastExperiment = (jobEnd-expPerJob)
		
		//System.err.println "lastExperiment: jobstart: $jobStart, jobend: $jobEnd, $lastExperiment"
		//System.err.println "lastExperiment $lastExperiment < numExperiments: $numExperiments"
		if (lastExperiment < numExperiments-1){
		  jobStart = lastExperiment+1
		  jobEnd = numExperiments-1
		
			if (jobEnd < jobStart) jobEnd = jobStart
			//System.err.println "last loop: jobStart: $jobStart, jobEnd: $jobEnd"
			
			def outName = "${outRoot}_${jobStart}_${jobEnd}"
			def cmdOut = "${rootCmd} -r $jobStart,$jobEnd  >  ${outName}"
			println cmdOut
		}		
	}
	
	
	
	/***
	* Write out a list of lines, one for each cluster job, ready for Parasol or gsub or whatever.
	*/ 
	static def writeClusterJobList(args,numExperiments,experimentsPerJob,scriptFile){
		
		// Remove the -k option, keep all the rest...
		def newargs = []
		def bRemoveNext = false;
		args.each{arg->
			if ((!arg.contains("-k")) && !bRemoveNext){
				newargs << arg
			}else{
				bRemoveNext = true;
			}
		}
					
		def rootCmd = "${scriptFile} ${newargs.join(" ")}"
		def expPerJob = Double.parseDouble(experimentsPerJob) // convert string value to double
		def numJobs = (double)numExperiments/(double)expPerJob				
		//System.err.println("numExperiments: $numExperiments  numJobs: $numJobs")
				
		int jobStart = 0
		int jobEnd = expPerJob -1;
		(0..<numJobs).each{
			//System.err.println "0..<$numJobs: jobStart: $jobStart\tjobEnd: $jobEnd"
		  def cmdOut = "${rootCmd} -r $jobStart,$jobEnd"
		  println cmdOut
		  jobStart += expPerJob
		  jobEnd += expPerJob				
		}

		// Handle remainder experiments..

		def lastExperiment = (jobEnd-expPerJob)
		
		//System.err.println "lastExperiment: jobstart: $jobStart, jobend: $jobEnd, $lastExperiment"
		//System.err.println "lastExperiment $lastExperiment < numExperiments: $numExperiments"
		if (lastExperiment < numExperiments-1){
		  jobStart = lastExperiment+1
		  jobEnd = numExperiments-1
		
			if (jobEnd < jobStart) jobEnd = jobStart
			//System.err.println "last loop: jobStart: $jobStart, jobEnd: $jobEnd"
			
		
			def cmdOut = "${rootCmd} -r $jobStart,$jobEnd"
		  println cmdOut
		}		
	}
	
	
	
	/****
	*	Removes censored samples that, due to being censored, can't be placed cleanly in one
	* class or another...
	*/ 
	def removeUnclassifiableCensoredSamples(instances,clinical,censoredAttribute){
		
			err.print "Removing unclassifiable censored instances.. before: ${instances.numInstances()} instances ..."

			def classIdx = instances.classAttribute().index()
			//err.println "classIdx: $classIdx"
		
			def name2IDMap = [:]
			clinical.eachWithIndex{clinInstance,idx->
				name2IDMap[clinInstance["ID"]] = idx
			}
		
			// if sample is censored and time > upper cutoff, leave it
			// otherwise, remove it.
			def useInstances = [] 		
			instances.each{instance->
				
				def instanceName = instance["ID"]
				
				// look up corresponding clinical instance censored value							
				def clinInstanceIdx = name2IDMap[instanceName]
				//err.println "clinInstanceIdx: $clinInstanceIdx"
				def censored = clinical[clinInstanceIdx][censoredAttribute]				
				//err.println "censored: $censored"
				//err.println "instanceName: $instanceName"
				//err.println "instance.numAttributes: "+instance.numAttributes()
				
				// KJD: Need to fix.. the valance can go either way depending on how censored is defined
				// 
				if (censored >= 1){
					// KJD binary assumption
					if (instance[classIdx] == "high"){
						//err.println "Save instance $instanceName"
						useInstances << instanceName
					}else{
						err.println "Removing instance $instanceName.  value = ${instance[classIdx]} $censoredAttribute = $censored"
					}
				}else{
					//err.println "Save instance $instanceName"
					useInstances << instanceName
				}
			}
			
			//err.println "About to filter instances..."
			def filteredInstances = subsetInstances(instances,useInstances)
			//err.println "done filtering instances."
			
			err.println "done.  After ${filteredInstances.numInstances()}"
			return(filteredInstances);
	}
	
	
	// TODO KJD: This should just be discretizeAttribute where attribute is passed in...
	// not specific to class...
	def discretizeClassAttribute(instances){
		return(discretizeClassAttribute(instances,exp.discretization,exp.classAttribute))
	}

	
	/***
	* discretizes the class attribute according to the discretization given in 
	* the experiment specification.
	*/ 
	static def discretizeClassAttribute(instances,discretization,classAttribute){
		
		def cutoffString = ""
		
	 	// Discretize numeric class 
		if (discretization == 'median'){
			err.println "median discretization"		
	    (instances,cutoffString) = classToMedian(instances,"low","high",classAttribute)        
	  }else if (discretization == 'quartile'){
			err.println "quartile discretization"		
	    (instances,cutoffString) = classToNominalTopBottomQuartile(instances,"low","high",classAttribute)        
	  }else if (discretization.contains(";")){
			def fields = discretization.split(";")
	    def lowerBound = fields[0] as double
	    def upperBound = fields[1] as double
			err.println "lower/upper cutoff discretization [$lowerBound,$upperBound]"
	    (instances,cutoffString) = classToNominalFromCutoffs(instances,lowerBound,upperBound,"low","high",classAttribute)
	  }else if (discretization.toLowerCase() == 'none'){
			err.println "NO discretization"
			cutoffString = "none"
		}else if (discretization == 'nominal'){
			err.print "Creating nominal attributes from coded numeric values for class $classAttribute"
			cutoffString = 'nominal'
			(instances,cutoffString) = classToNominal(instances,classAttribute)
			err.println "done."
		}else if (discretization == 'dichotomize'){
			err.println "dichotomize"
		}else{
			err.println "UNKNOWN discretization: "+discretization
		}
		return([instances,cutoffString])
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
	  WekaMineResults.appendSummaryLine(idx,instances,out,exp,eval,dataName)
	}
	
	def appendFeatures(out,idx,instances,maxFeaturesOut){
  	WekaMineResults.appendFeaturesLine(idx,instances,out,exp,eval,maxFeaturesOut,dataName)
	}
	
	def appendTestSamples(out,idx,instances){
  	WekaMineResults.appendSamplesLine(idx,instances,out,exp,eval,dataName)			                                      
	}

	def appendTrainingSamples(out,idx,instances){
  	WekaMineResults.appendSamplesLine(idx,instances,out,exp,eval.trainingEval,dataName)			                                      
	}
	
		
	//=================================================================================
	// *********                  Lower level functions.                    ***********
	//=================================================================================

	def getFilteredClassifier(classifier){
			// Create a classifier from the name...
			// By using filtered classifer to remove ID, the cross-validation
			// wrapper will keep the original dataset and keep track of the mapping 
			// between the original and the folds (minus ID). 
			def filteredClassifier = new FilteredClassifier()
			def removeTypeFilter = new RemoveType();
			//removeTypeFilter.setOptions("-T string");  

			filteredClassifier.setClassifier(classifier)
			filteredClassifier.setFilter(removeTypeFilter)
		
			return(filteredClassifier)
	}
	
	/**
	* Remove string attributes.  Specifically the string attribute that contains 
	* the instance ID.  This is done because most attribute evaluators and classifiers
	* can not handle string attributes. 
	*/ 
	static Instances removeInstanceID(instances){
		def filter = new RemoveType()
		filter.setInputFormat(instances);
		def instances_notype = Filter.useFilter(instances,filter)
		return(instances_notype)
	}

	def removeID(instances){
		def filter = new RemoveType();
		filter.setInputFormat(instances);
		def newinstances = Filter.useFilter(instances,filter)
		return(newinstances);
	}

	
	/***
	* cross validate model with folds generated in advance. 
	*/ 
	def crossValidateModelWithGivenFolds(classifier,data,foldSets,evalTraining){				
		def filteredClassifier = getFilteredClassifier(classifier);		

		// Perform cross-validation of the model..
		eval = new Evaluation2(data)
		
		def predictions = new StringBuffer()
		def trainPredictions = null;
		if (evalTraining) trainPredictions = new StringBuffer()		
		eval.crossValidateModelWithGivenFolds(filteredClassifier,data,(FoldSets) foldSets,predictions,new Range("first,last"),false,trainPredictions)
		//testResults = CVUtils.parsePredictions(predictions)

		//if (trainPredictions != null) trainResults = CVUtils.parsePredictions(trainPredictions)
		//else trainResults = null
						
		return(eval);		
	}
	
	/***
	* cross validate model with folds generated in advance. 
	*/ 
	def crossValidateModelWithPoolsAndFolds(classifier,trainData,testData,trainFoldSets,testFoldSets,evalTraining){				
		def filteredClassifier = getFilteredClassifier(classifier);		

		// Perform cross-validation of the model..
		// Evaluation2 only uses data to look at number of class values and create confusion and cost matrix.
		// So it doesn't matter which we pass to it as both should have clinical instances in them. 
		eval = new Evaluation2(trainData)
		
		def predictions = new StringBuffer()
		def trainPredictions = null;
		if (evalTraining) trainPredictions = new StringBuffer()		
		eval.crossValidateModelWithPoolsAndFolds(filteredClassifier,trainData,testData,(FoldSets) trainFoldSets,
		(FoldSets) testFoldSets,predictions,new Range("first,last"),false,trainPredictions)
		//testResults = CVUtils.parsePredictions(predictions)

		//if (trainPredictions != null) trainResults = CVUtils.parsePredictions(trainPredictions)
		//else trainResults = null
						
		return(eval);		
	}
	

	/***
	* cross validate model with generated folds. 
	*/ 
	def crossValidateModel(classifier,data,cvFolds,rng,evalTraining){
		
		def filteredClassifier = getFilteredClassifier(classifier);	

		// Perform cross-validation of the model..
		eval = new Evaluation2(data)
		def predictions = new StringBuffer()
		
		def trainPredictions = null;
		if (evalTraining) trainPredictions = new StringBuffer()
	
		err.println "CHECK1"
		//eval.crossValidateModel(filteredClassifier,data,cvFolds,rng,predictions)
		
		// Apparently I haven't written this yet.... looks like I intended the functionality to match 
		// cross-validate with folds... 
		eval.crossValidateModel(filteredClassifier,data,cvFolds,rng,predictions,new Range("first,last"),false,trainPredictions)
		//testResults = CVUtils.parsePredictions(predictions)
				
		//if (trainPredictions != null) trainResults = CVUtils.parsePredictions(trainPredictions)
		//else trainResults = null
		
		return(eval)		
	}
	
	
	
	static def classToNominal(data,classAttribute){
		def classIdx = data.setClassName(classAttribute) 
		def numericToNominal = new NumericToNominal()
		numericToNominal.setAttributeIndices("${classIdx+1}".toString())
		numericToNominal.setInputFormat(data)
		def filteredData = Filter.useFilter(data,numericToNominal)
		return([filteredData,'nominal'])
	}
	

  /***
  *  Converts the numeric class attribute into 
  */
  static def classToNominalTopBottomQuartile(data,lowString,highString,classAttribute){
    def classIdx = data.setClassName(classAttribute) // Necessary to get idx, since idx changes 
     
    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "DISCRETIZE: Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setUseMedian(false)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 

		// Hacky way to save the cutoffs for use later...
		def newName = "${discretizedData.relationName()} cutoffs:\t${quartile.cutoff1};${quartile.cutoff2}"
		discretizedData.setRelationName(newName)
		def cutoffString = "${quartile.cutoff1};${quartile.cutoff2}"				
	  return([discretizedData,cutoffString])
  }

	/**
  *  Converts the numeric class attribute into 
  */
  static def classToNominalFromCutoffs(data,cutoffLow,cutoffHigh,lowString,highString,classAttribute){
    def classIdx = data.setClassName(classAttribute) // Necessary to get idx, since idx changes 

    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "DISCRETIZE: Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setCutoffs(cutoffLow,cutoffHigh)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 
		def newName = "${discretizedData.relationName()} cutoffs:\t${quartile.cutoff1};${quartile.cutoff2}"
		discretizedData.setRelationName(newName)
		def cutoffString = "${quartile.cutoff1};${quartile.cutoff2}"				
	  return([discretizedData,cutoffString])
  }

  /**
  *  Converts the numeric class attribute into 
  */
  static def classToMedian(data,lowString,highString,classAttribute){
    def classIdx = data.setClassName(classAttribute) // Necessary to get idx, since idx changes 

    // Discretize class (select whether to discretize median, quartile, or with chosen value)
    err.print "DISCRETIZE: Discretizing class attribute..."
    def quartile = new NumericToQuartileNominal()
    quartile.setAttributeIndices("${classIdx+1}".toString())
    quartile.setUseMedian(true)
    quartile.setInputFormat(data)
    quartile.setNominalValues(lowString,highString)
    def discretizedData = Filter.useFilter(data,quartile)
    classIdx = discretizedData.setClassName(classAttribute)
    err.println "done."  // KJD report how many high/low as a sanity check. 
		def newName = "${discretizedData.relationName()} cutoffs:\t${quartile.cutoff1};${quartile.cutoff2}"
		discretizedData.setRelationName(newName)
		def cutoffString = "${quartile.cutoff1};${quartile.cutoff2}"				
    return([discretizedData,cutoffString])
  }

	def applyUnsupervisedFilter(instances){
		return(applyUnsupervisedFilter(instances,exp.filter))
	}

	/****
	* 
	*/ 
	static Instances applyUnsupervisedFilter(instances,Filter filter){		

		if (filter == null){ 
			err.println "FILTER: Apply unsupervised filter: None"
			return(instances)
		}				
				
		err.print "Applying unsupervised filter ${filter.toString()}..."	
								
		// If there is an ID attribute, remove it before filtering since attribute 
		// evaluators and classifiers choke on it. If no ID, just filter...
		def filteredInstances
		def nameSet = instances.attributeNames() as Set
		if (nameSet.contains("ID")){	
			def instNames = instances.attributeValues("ID")
			def noIDinstances = WekaMine.removeInstanceID(instances)

			// Apply the filter to instances...
			//noIDinstances = WekaMine.applyUnsupervisedFilter(noIDinstances,options.filter)
			filter.setInputFormat(noIDinstances);	
			noIDinstances = Filter.useFilter(noIDinstances,filter)

			// Put the ID back into the instances...
			// KJD: Note that addID below assumes same number of instances in same order. 
			// KJD: This assumption may not always hold for filters... need to think of how to handle this.
			filteredInstances = WekaMine.addID(noIDinstances,instNames)
		}else{
			err.println "WARNING: this code branch is associated with some coding issues.  Please proceed with caution."
			filter.setInputFormat(instances);				
			filteredInstances = Filter.useFilter(instances,filter)
		}
		
		err.println("done.")
		
		return(filteredInstances)
	}
	
	
	static Instances applyUnsupervisedFilterFromName(instances,String filterName){
		err.print("Apply unsupervised filter $filterName...")
		def filter = filterFromSpec(filterName)
		filter.setInputFormat(instances);	
		def filteredInstances = Filter.useFilter(instances,filter)
		err.println("done.")
		return(filteredInstances)
	}

	/**
	* Apply an attribute selection algorithm (attributeEvaluator + attributeSearch) to the
	* instances and return the reduced set of attributes. 
	*/ 
	Instances selectAttributes(instances){
		err.print("ATTRIBUTE SELECTION: Select attributes with ${exp.attrEvalStr}.  Before: ${instances.numAttributes()} ...")
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
  static Instances removeUselessAttributes(data){
    err.print "REMOVE: Removing useless attributes. Before: ${data.numAttributes()}..."
  
    def remove = new RemoveUseless(); 
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
     err.print "REMOVE: Removing instances with negative class value. Before: ${data.numInstances()}..."
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
    def attrIndicesStr = IU.attributeNames2Indices(data,selectedAttributes)
    def remove = new Remove();
    remove.setAttributeIndices(attrIndicesStr);
    remove.setInvertSelection(true); // Remove everything not in this list. 
    remove.setInputFormat(data);
    def selectedAttributeData = Filter.useFilter(data, remove);
    return(selectedAttributeData)
  }

	/***
	* Will return the first holdoutFraction instances as a set of instances. 
	* Will remove those instances from data. 
	*/ 
	static Instances holdOutInstances(Instances data,double holdoutFraction){
		def metaFolds = (1.0/holdoutFraction) as int
		
		def filter = new StratifiedRemoveFolds()		
		filter.setInputFormat(data);
		filter.setNumFolds(metaFolds)
		filter.setFold(1)
		filter.setInvertSelection(false)
		def holdout = Filter.useFilter(data,filter)
		return(holdout);
	}

	/***
	* Will return the first remaining instances in data after holdOutInstances
	* called on data. 
	*/ 
	static Instances remainingInstances(Instances data,double holdoutFraction){
		def metaFolds = (1.0/holdoutFraction)	as int
		def filter = new StratifiedRemoveFolds()
		filter.setInputFormat(data);
		filter.setNumFolds(metaFolds)
		filter.setFold(1)
		filter.setInvertSelection(true)
		def remaining = Filter.useFilter(data,filter)
		return(remaining);
	}
	
	
	/***
	* Build a model for instances and evaluate them on holdoutInstances
	*/ 
	def buildAndEvaluateOnHoldout(instances,holdoutInstances){
		// Create a classifier from all of the non-validation samples...
		// Discretize the class attribute...
		def cutoffs
		(instances,cutoffs) = discretizeClassAttribute(instances)
		
		// Remove ID attribute, since attribute evaluators and classifiers choke on it...
		instances = removeInstanceID(instances)
		
		// Apply the attribute selection algorithm to instances...
		instances = selectAttributes(instances)
		err.print("Build evaluation classifier ${exp.classifierStr}...")
		exp.classifier.buildClassifier(instances);
		err.println("done.")
		
		def model = new WekaMineModel(instances,exp.classifier,cutoffs)	
		// The model has no ID, so we save the ID for later reporting...
		instanceIDs = holdoutInstances.attributeValues("ID") as ArrayList
		
		def results = model.classify(holdoutInstances)
		def tp,tn,fp,fn;
		(tp,tn,fp,fn) = model.accuracy(results,clinical)
		return([tp,tn,fp,fn])
	}
	
	/***
	* Remove all the instances from data except those named in selectedInstances
	* 
	* InstanceUtils.removeNamedInstances does the opposite of this.  Should move 
	* this to InstanceUtils and make the names match.  
	*/ 
	static Instances subsetInstances(Instances data,List selectedInstances){
		WekaAdditions.enable() // Just in case
		
		def remove = new RemoveRange()
		
		// KJD selectedInstances should be a Set, because we don't care about order and 
		// can't have repetitions...
		def idxList = data.nameSetToIndexList(selectedInstances)	
		idxList = idxList.collect{it+1} // convert to one based	
		def rangeStr = idxList.join(",")
		//System.err.println "selectedInstances: "+selectedInstances
		//System.err.println "rangeStr: "+rangeStr
		remove.setInstancesIndices(rangeStr)
		remove.setInvertSelection(true) // Remove everything not in this list
		remove.setInputFormat(data)
		def selectedInstanceData = Filter.useFilter(data,remove)
		return(selectedInstanceData)
	}
	
	/***
	* Remove the named instances from data. 
	* InstanceUtils.removeNamedInstances duplicates this functionality...
	*/ 
	static Instances removeInstances(Instances data,selectedInstances){
		WekaAdditions.enable() // Just in case
		
		def remove = new RemoveRange()
		
		def idxList = data.nameSetToIndexList(selectedInstances)	
		idxList = idxList.collect{it+1} // convert to one based	
		def rangeStr = idxList.join(",")
		remove.setInstancesIndices(rangeStr)
		remove.setInvertSelection(false) // Remove everything in this list
		remove.setInputFormat(data)
		def remainingInstanceData = Filter.useFilter(data,remove)
		return(remainingInstanceData)
	}
		
	/**
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  static Instances readFromTable(dataFileName){
		def instancesInRows = false // Default 
		readFromTable(dataFileName,instancesInRows)
	}
	
	/**
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  static Instances readNumericFromTable(dataFileName){
		
    // Read data in from table.  Handle missing values as we go.
    err.print "Loading data from $dataFileName..."
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)
		def relationName = dataFileName;
		
		Instances data = loader.readNumeric(dataFileName,relationName,"\t");		
		err.println "done."
    return(data)
  }
 	
	/**
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  static Instances readNominalFromTable(dataFileName,instancesInRows){
    // Read data in from table.  Handle missing values as we go.
    err.print "Loading data from $dataFileName..."
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)

		def relationName = dataFileName;
    Instances data = loader.readNominal(dataFileName,relationName,"\t",instancesInRows){  

      // Lots of different missing value notations...
      if ((it == "") || (it == null) || (it == "NA") || 
         (it == "null") || (it == 'NULL') || 
         (it == "?")){
        return ("?")
      }else{
        //return(it as Double)  //KJD 5/16
			  return(it)
      }
    }
    //err.println "DEBUG ${data.numInstances()} x ${data.numAttributes()} done."
    return(data)
  }

	
  /**
  * Read instances from a table, filling in missing value tokens as we go. 
  */ 
  static Instances readFromTable(dataFileName,instancesInRows){
    // Read data in from table.  Handle missing values as we go.
    err.print "Loading data from $dataFileName..."
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)

		def relationName = dataFileName;
    Instances data = loader.read(dataFileName,relationName,"\t",instancesInRows){  

      // Lots of different missing value notations...
      if ((it == "") || (it == null) || (it == "NA") || 
         (it == "null") || (it == 'NULL') || 
         (it == "?")){
        return ("?")
      }else{
        //return(it as Double)  //KJD 5/16
			  return(it)
      }
    }
    //err.println "DEBUG ${data.numInstances()} x ${data.numAttributes()} done."
    return(data)
  }

	/**
  *  Creae a classifier from the command-line classifier specification
  *  string.  For example:<br><br>
  * 
  *  weka.classifiers.functions.SMO -C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -M
  */
  static def classifierFromSpec(classifierSpec){
		if (classifierSpec.toLowerCase() == 'none') return('none');
	
    // Create a classifier from the name...
    def options = Utils.splitOptions(classifierSpec)
    def classifierName = options[0]
    options[0] = ""

    //System.err.println "classifierName: $classifierName"
    //System.err.println "options: $options"

    def classifier = Classifier.forName(classifierName,options) 
    return(classifier)
  }

	static def filterFromSpec(filterSpec){
		 // Create a filter from the name...
	    def options = Utils.splitOptions(filterSpec)
	    def filterName = options[0]
	    options[0] = ""
		
		  def filter = (Filter) Class.forName(filterName).newInstance();
		  if (filter instanceof OptionHandler){
		    ((OptionHandler) filter).setOptions(options);
		  }
			return(filter)		
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

		
		/*	
		// Wrapping as AS classifier now is pro forma because Evaluation2 expects it to be this way...
		def asClassifier = new AttributeSelectedClassifier2();
		def search = new Ranker();
		search.setNumToSelect(-1); // save all attributes, not really doing attribute selection. 
		asClassifier.setClassifier(exp.classifier);
		asClassifier.setEvaluator(new InfoGainAttributeEval()); // placeholder. 
		asClassifier.setSearch(search);
		*/

/*
static String getFormattedEvaluationSummary(numInstances,eval){
   def samples = numInstances
   def pctCorrect = eval.pctCorrect() 
   def precision1 = eval.precision(1)
   def recall1 = eval.recall(1)    
   def precision0 = eval.precision(0)
   def recall0 = eval.recall(0)
   def tp1 = eval.numTruePositives(1) as Integer
   def fp1 = eval.numFalsePositives(1) as Integer
   def tn1 = eval.numTrueNegatives(1) as Integer
   def fn1 = eval.numFalseNegatives(1) as Integer
   def rms = eval.rootMeanSquaredError()
   def roc = eval.weightedAreaUnderROC()    
   def rval = sprintf("%d,%.4g,%.4g,%.4g,%.4g,%.4g,%d,%d,%d,%d,%.4g,%.4g",
             samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc)
 
   return(rval)    
 }
*/
