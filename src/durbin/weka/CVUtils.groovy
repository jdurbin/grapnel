#!/usr/bin/env groovy 

package durbin.weka;

import weka.core.*
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.evaluation.*;
import weka.classifiers.meta.FilteredClassifier;
import java.util.Random


/***
* A single evaluation returned from a cross-validation experiment. (i.e. the output of CVUtils)
*/ 
public class EvaluationResult{

	def EvaluationResult(a,p,s,m,pr){
		actual = a
		predicted = p
		instanceID = s
		isMatch = m
		probability = pr
	}

	String toString(){
		def rval = instanceID+"\t"+actual+"\t"+predicted+"\t"+isMatch+"\t"+probability;
		return(rval)
	}

	def actual;
	def predicted;
	def instanceID;
	def probability
	boolean isMatch;
}

/***
* A class to encapsulate the basics of running a cross validation
* experiment and make the interface and code simpler. 
*/ 
public class CVUtils{

	static err = System.err

	// Evaluation2 is a modified version of weka Evaluation that saves
	// the classifiers created in a cross-validation experiment for 
	// later access. 
	Evaluation2 eval;



	/***
	* Generate cross validation test set from a saved set of folds...
	*/ 
	static Instances testCV(Instances data,cvAssignments,fold){
		Instances test = new Instances(data,data.numInstances())
		for(int i = 0;i<cvAssignments.size();i++){
			if (cvAssignments[i] == fold){
				data.copyInstances(i,test,1)
			}
		}	
		return(test)
	}

	/****
	* Generate a cross validation training set from a saved set of folds. 
	*/ 
	static Instances trainCV(Instances data,cvAssignments,fold){
		Instances train = new Instances(data,data.numInstances())
		
		//err.println "CHECK data.size="+data.numInstances()
		//err.println "CHECK trainCV: "+train.numInstances()
		//err.println "CHECK cvAssignments.size="+cvAssignments.size()
		
		for(int i = 0;i<cvAssignments.size();i++){
			if (cvAssignments[i] != fold){
				data.copyInstances(i,train,1)
			}
		}	
		
		//err.println "CHECK trainCV after copy: "+train.numInstances()
		
		return(train)
	}
	
	
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
	
	/***
	* cross validate model with folds generated in advance. 
	*/ 
	List<EvaluationResult> crossValidateModelWithGivenFolds(classifier,data,foldSets){
				
		def filteredClassifier = getFilteredClassifier(classifier);		

		// Perform cross-validation of the model..
		eval = new Evaluation2(data)
		
		def predictions = new StringBuffer()
		eval.crossValidateModelWithGivenFolds(filteredClassifier,data,(FoldSets) foldSets,predictions,new Range("first,last"),false)
		def results = parsePredictions(predictions)
		return(results);		
	}

	/***
	* cross validate model with generated folds. 
	*/ 
	List<EvaluationResult> crossValidateModel(classifier,data,cvFolds,rng){
		
		def filteredClassifier = getFilteredClassifier(classifier);	

		// Perform cross-validation of the model..
		eval = new Evaluation2(data)
		def predictions = new StringBuffer()
		eval.crossValidateModel(filteredClassifier,data,cvFolds,rng,predictions,new Range("first,last"),false)
		
		def results = parsePredictions(predictions)
		return(results)		
	}
	

	/***
	* Parse the predictions out of the results text. 
	*/ 
	def parsePredictions(predictions){	
		def results = new ArrayList<EvaluationResult>()
		def lines = predictions.toString().split("\n")

		// KJD TODO: Now that I've broken down and made a fork, Evaluation2, I 
		// could just add a more direct API into Evaluation2 for this...

		// Probably should, in fact, since I'm now running up against having + as a nominal 
		// class attribute, screwing up the regex below...

		// Output of predictions looks like:  
		// inst#     actual  predicted error prediction (ID)
		//     1      1:low      1:low       1 (P1)
		//     2     2:high      1:low   +   0.5 (P6)
		//     3     2:high     2:high       1 (P0)
		lines[1..-1].each{line->    

			// Parse out fields we're interested in..
			//def m = line =~ /\d:(\w+).*\d:(\w+).*\((.+)\)/    
			//def m = line =~ /\d:(.+).*\d:(\w+).*\((.+)\)/         
			def m = line =~ /\d:(.+).*\d:(\w+)(\s+.*\s+)(\d.*)\s.*\((.+)\)/   
			def actual = m[0][1]
			actual = actual.trim() // remove leading/trailing whitespace
			def predicted = m[0][2]
			//def spaces = m[0][3] // or + 
			def probability = m[0][4]
			def instanceID = m[0][5]
			//println "$actual $predicted [$spaces] [$probability] $sample"

			def result = new EvaluationResult(actual,predicted,instanceID,
				!line.contains("+"),probability)
				results.add(result);
		}
		return(results);
	}
	
}

	// TODO:
	// stop when badResults == 0 or data.size == 0
	// 
	// 
	//
	// badResults.size=1	data.size=32


