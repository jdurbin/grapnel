package swiftml

import durbin.weka.*
import weka.clusterers.SimpleKMeans
//import weka.clusterers.HierarchicalClusterer
import weka.classifiers.trees.RandomForest
import weka.classifiers.functions.SMO
import weka.classifiers.functions.SimpleLogistic
import weka.classifiers.functions.supportVector.RBFKernel
import weka.attributeSelection.Ranker
import weka.attributeSelection.InfoGainAttributeEval
import weka.classifiers.Evaluation
import weka.filters.*
import weka.filters.supervised.attribute.AttributeSelection

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
	
	static def svm_rbf(params=[:]){
		def svm = new SMO()
		def kernel = new RBFKernel()
		//kernel.setGamma()
		svm.setKernel(kernel)
		svm.setOptions(params2Options(params))
		return(svm)
	}	
	
	static def svm(params=[:]){
		def svm = new SMO()
		svm.setOptions(params2Options(params))
		return(svm)
	}
	
	static def logistic(params=[:]){
		def log = new SimpleLogistic()
		log.setOptions(params2Options(params))
		return(log)
	}	
	
	/**********
	* Attribute Selection
	*/ 
	static def InformationGain(params=[:]){
		def attrSel = new AttributeSelectionWrapper()
		attrSel.attributeEval= new InfoGainAttributeEval()
		attrSel.attributeSearch = new Ranker()
		attrSel.attributeSearch.setNumToSelect(params.numAttributes)
		attrSel.numAttributes = params.numAttributes
		return(attrSel)
	}
		
	static def selectAttributes(attributeSelection,withIDInstances){
		AttributeSelection atSel = new AttributeSelection();
		atSel.setEvaluator(attributeSelection.attributeEval)
		atSel.setSearch(attributeSelection.attributeSearch)		
		def IDs = withIDInstances.attributeValues("ID")
		def noIDInstances = removeID(withIDInstances)
		atSel.setInputFormat(noIDInstances)
		def selectedInstances = Filter.useFilter(noIDInstances,atSel)	
		def newWithIDInstances = WekaMine.addID(selectedInstances,IDs)
		return(newWithIDInstances)
	}
		
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