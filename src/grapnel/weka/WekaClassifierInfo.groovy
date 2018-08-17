package grapnel.weka;

import grapnel.weka.*
import grapnel.util.*
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.*

class WekaClassifierInfo{

	static def getSummary(classifier){
		def summary		
		// Base classifier might be wrapped in a filtered or attribute selected classifier
		def baseClassifier
		if (classifier instanceof weka.classifiers.meta.FilteredClassifier){
			baseClassifier = classifier.getClassifier()
			if (baseClassifier instanceof grapnel.weka.AttributeSelectedClassifier2){
				baseClassifier = baseClassifier.getClassifier()
			}						
		}else if (classifier instanceof grapnel.weka.AttributeSelectedClassifier2){
			baseClassifier = classifier.getClassifier()
		}else{	
			baseClassifier = classifier
		}
		
		switch(baseClassifier){
			//case {it instanceof grapnel.weka.BalancedRandomForest}:
			//	def trees = baseClassifier.m_bagger.m_Classifiers
			//	def numTrees = trees.size()
			//	return("Balanced Random Forest.  NumTrees: $numTrees")
			case {it instanceof weka.classifiers.trees.RandomForest}:
			// KJD TODO need to fix weka 3.8 refactor. 
				//def trees = baseClassifier.m_Classifiers
				//def numTrees = trees.size()
				//return("Random Forest.  NumTrees: $numTrees")		
				return("Random Forest.");
			break;
			
			case {it instanceof weka.classifiers.functions.SimpleLogistic}:
			return("Logistic Regression")
			break;
			
			case {it instanceof weka.classifiers.functions.SMO}:
			if (baseClassifier.m_KernelIsLinear){
				return("SVM linear kernel.")
			}else{
				return("SVM Non-linear kernel.")
			}
			break;

			default:
				return("${baseClassifier.class}")
			break;
		}
	}

	/**
	* Returns a map from features to weights for the features used by this classifier. 
	* Some classifiers use all the features, some use only a subset.  Some have obvious 
	* weights, some less so. 
	*/ 
	static HashMap getFeatures(classifier){
		
		System.err.println "GET FEATURES"
		
		// Base classifier might be wrapped in a filtered or attribute selected classifier
		def baseClassifier
		if (classifier instanceof weka.classifiers.meta.FilteredClassifier){
			baseClassifier = classifier.getClassifier()
			if (baseClassifier instanceof grapnel.weka.AttributeSelectedClassifier2){
				baseClassifier = baseClassifier.getClassifier()
			}						
		}else if (classifier instanceof grapnel.weka.AttributeSelectedClassifier2){
			baseClassifier = classifier.getClassifier()
		}else{	
			baseClassifier = classifier
		}
		
		switch(baseClassifier){
			//case {it instanceof grapnel.weka.BalancedRandomForest}:
			case {it instanceof weka.classifiers.trees.RandomForest}:
				// Need to refactor for weka3.8 changes. 
				def features2weights = getRFFeatures(baseClassifier)
				return(features2weights)
			break;
			
			case {it instanceof weka.classifiers.functions.SimpleLogistic}:
			def features2weights = getLogisticFeatures(baseClassifier)
			return(features2weights)
			break;
			
			case {it instanceof weka.classifiers.functions.SMO}:
			if (baseClassifier.m_KernelIsLinear){
				def features2weights = getSMOFeatures(baseClassifier);
				return(features2weights);
			}else{
				//System.err.println "\tWARNING: Unsupported non-linear kernel for WekaClassifierInfo.getFeatures"
			}
				return(null);
			break;

			default:
			System.err.println "ERROR: Unsupported classifier type in WekaClassifierInfo"+baseClassifier.class
				return(null);
			break;
		}
		def features2weights = ["NA":0]
		return(features2weights)
	}
			
	/***
	* Performs depth first search through tree to collect feature names. 
	*/ 
	static def dfsCollectFeatures(tree,info,features){
		if (tree.m_Attribute == -1) {
			// Binary assumption
			int sum = tree.m_ClassDistribution[0]+tree.m_ClassDistribution[1]
			return(sum)
		}	
		int childInstanceCount = 0	
		tree.m_Successors.eachWithIndex{child,i->
			def featureName = info.attribute(tree.m_Attribute).name()
			//System.err.println "Child# $i DFS featureName: "+featureName+"\t"+child.m_Prop
			//System.err.println "\tClass distribution: "+child.m_ClassDistribution		
			int instanceCounts = dfsCollectFeatures(child,info,features)
			childInstanceCount+=instanceCounts
			if (features.keySet().contains(featureName)){
				features[featureName] = features[featureName] + instanceCounts
			}else{
				features[featureName] = instanceCounts
			}
		}
		return(childInstanceCount)							
	}

	/**
	* Goes through each tree of the random forest and collects all of the features
	* in that tree. 
	*/ 	
	static def getRFFeatures(classifier){
	
		System.err.println "getRFFeatures"
		def feature2weights = [:]
		def feature2counts =[:]

		// classifier is instance of Bagging
		def trees = classifier.m_Classifiers
		int totalInstances = 0;	
		trees.each{randomTree->
			def tree = randomTree.m_Tree
			int treeInstances = dfsCollectFeatures(tree,randomTree.m_Info,feature2counts)
			totalInstances+= treeInstances
		}

		feature2counts.each{k,v->
			double fraction = (double)v/(double)totalInstances
			feature2weights[k] = fraction
		}		
		
		// Sort them by weight to make life easy on recipient
		feature2weights = feature2weights.sort{-it.value}
		
		return(feature2weights);
	}
	
	
	static def getLogisticFeatures(classifier){
		def logBase = classifier.m_boostedModel
		int[][] attributes = logBase.getUsedAttributes()
		// First dimension is attributes for class 0 or class 1 (etc.), which are 
		// the same for us...		
		def attributeNames = attributes[0].collect{attr->
			logBase.m_numericDataHeader.attribute(attr).name()
		}
		
		double[][] coefficients = logBase.getCoefficients();
		def attributeWeights = attributes[0].collect{attr->
			coefficients[0][attr+1]
		}
		def constant = coefficients[0][0]  // Not using right now, but might in the future. 
		
		def names2weights = [:]
		attributeNames.eachWithIndex{n,i->
			names2weights[n] = attributeWeights[i]
		}				
					
		names2weights = names2weights.sort{-Math.abs(it.value)}
		return(names2weights)
	}	
	
	static def getSMOFeatures(classifier){
		// if there are only two classes... 
		def c = classifier.m_classifiers[0][1]

		//if (classifier.m_filterType == classifier.FILTER_NORMALIZE) println "Normalized values."

		def name2weight = [:]

		c.m_sparseWeights.eachWithIndex{w,i->
		  if (c.m_sparseIndices[i] != (int)classifier.m_classIndex) {
			  def name = c.m_data.attribute(c.m_sparseIndices[i]).name()
			  def value = c.m_sparseWeights[i]
			  name2weight[name] = value 
			  //print Utils.doubleToString(c.m_sparseWeights[i], 12, 4)+"\t"
			  //println c.m_data.attribute(c.m_sparseIndices[i]).name()
		  }
		}
		//println c.m_b

		name2weight = name2weight.sort{-Math.abs(it.value)}
		return(name2weight)
	}		
}