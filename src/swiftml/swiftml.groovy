package swiftml

import durbin.weka.*
import weka.clusterers.SimpleKMeans
//import weka.clusterers.HierarchicalClusterer
import weka.classifiers.trees.RandomForest
import weka.attributeSelection.Ranker
import weka.attributeSelection.InfoGainAttributeEval
import weka.classifiers.Evaluation
import weka.filters.*

/***
* Class to encapsulate nicer versions of WekaMine functions. 
* Change WekaMine itself into WekaMinePipeline or some such...
*/
class swiftml{
	
	def swiftml(){
		WekaAdditions.enable()
	}
							
	/*********
	* Reading and writing files...
	*/ 
		
	static def readNumericTab(params=[:]){		
		return(WekaMine.readNumericFromTable(params.file))
	}
	
	static def readTab(params=[:]){		
		return(WekaMine.readFromTable(params.file))
	}
	
	static def makeInstances(data,clin,className){
		return(WekaMine.createInstancesFromDataAndClinical(data,clin,className))
	}	
	
	static def removeID(data){
		return(WekaMine.removeInstanceID(data))
	}
		
	
	/**********
	* CLASSIFIERS 
	*/ 
	static def RandomForest(params=[:]){
		def rf = new RandomForest()
		rf.setOptions(params2Options(params))
		return(rf)
	}
	
	static def BalancedRandomForest(params=[:]){
		def brf = new BalancedRandomForest()
		brf.setOptions(params2Options(params))
		return(brf)
	}
	
	
	/**********
	* Attribute Selection
	*/ 
	static def InformationGain(params=[:]){
		def attrSel = new AttributeSelection()
		attrSel.attributeEval= new InfoGainAttributeEval()
		attrSel.attributeSearch = new Ranker()
		attrSel.attributeSearch.setNumToSelect(params.numAttributes)
		attrSel.numAttributes = params.numAttributes
		return(attrSel)
	}
	
	
	/*
	static def selectAttributes(attributeSelection,withIDInstances){
		def noIDInstances = removeID(withIDInstances)
		def IDs = instances.attributeValues("ID")
		selectedInstances = Filter.useFilter(noIDInstances,attributeSelection)	
		selectedInstances.
	
		wmAttributeSelection removes IDs, performs selection, then adds ID back in...
		This seems dubious... why would removed attribute names be in the given order???	
	}
	*/
	
	
	/***********
	* CLASSIFICATION
	*/ 
	static def buildClassifier(classifier,withIDInstances){
		def noIDInstances = removeID(withIDInstances)
		classifier.buildClassifier(noIDInstances)
	}
	
	static def getClassValues(instances){
		def classValues = []
		def classAttr = instances.classAttribute()
		(0..<classAttr.numValues()).each{i->
			classValues <<classAttr.value(i)
		}
		return(classValues)
	}
	
	static def classify(classifier,withIDInstances){
		def classValues = getClassValues(withIDInstances)
	
		// test to see if it has string attributes.
		def noIDInstances = removeID(withIDInstances)
		def classifications = []	
		noIDInstances.eachWithIndex{instance,i->		
			def classification = new Classification()
			classification.instanceID = withIDInstances.instance(i).stringValue(0) // get the ID
			def distribution = classifier.distributionForInstance(instance)
			distribution.eachWithIndex{p,j->
				classification.values[classValues[j]] = p
			}
			classifications<<classification
		}
		return(classifications)
	}
	
	
	/************
	* Evaluation
	*/ 
	static def crossValidate(classifier,instances,folds){
		Evaluation eval = new Evaluation(instances);
		def noIDInstances = removeID(instances)
		eval.crossValidateModel(classifier,noIDInstances,folds, new Random(1));	
		def experiment = new CVExperiment(eval,instances.classAttribute().name())	
		return(experiment);
	}
	
	static def crossValidate(attrSel,classifier,instances,folds){
		def asClassifier = new AttributeSelectedClassifier2();
		asClassifier.setClassifier(classifier)
		asClassifier.setEvaluator(attrSel.attributeEval)
		asClassifier.setSearch(attrSel.attributeSearch)
				
		Evaluation eval = new Evaluation(instances);
		def noIDInstances = removeID(instances)
		eval.crossValidateModel(classifier,noIDInstances,folds, new Random(1));	
		def experiment = new CVExperiment(eval,instances.classAttribute().name())	
		return(experiment);
	}
	
		
	/*********
	* CLUSTERING
	*/ 	
		
	static def SimpleKMeans(params=[:]){
		def km = new SimpleKMeans()
		km.setOptions(params2Options(params))
		return(km)
	}
	
	/* This doesn't exist in weka-3-6-1
	   KJD However, newer versions do not tolerate duplicate IDs in attributes which happens 
	   briefly in merge data and clinical... so remove this until ID issue is sorted out. 
	static def HierarchicalClusterer(params=[:]){
		def hc = new HierarchicalClusterer()
		hc.setOptions(params2Options(params))
		return(hc)
	}
	*/
			
	
	
	static def params2Options(params){
		def options = []
		params.each{k,v->
			// Flags have value true/false
			if (v == true){
			    options <<"-$k"
			}else{
				options << "-$k"
				options << v
			}
		}
		return(options as String[])
	}
	
	
}