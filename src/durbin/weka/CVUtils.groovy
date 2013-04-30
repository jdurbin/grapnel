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

	/***
	* Generate cross validation test set from a saved set of folds...
	*/ 
	static Instances testCV(Instances data,FoldSet foldset,int fold){
		def testSampleNames = foldset.getTestSamples(fold)		
		// Look up the instance indexes for this list of names...
		def testSampleIdxs = data.nameListToIndexList(testSampleNames)					
		// Copy each instance to the test set...
		Instances test = new Instances(data,testSampleNames.size())
		testSampleIdxs.each{idx->
			//err.println "idx: $idx \ttest: ${test.numInstances()}"
			if (idx != -1) data.copyInstances(idx,test,1);	// -1 means no instance with that name\n			
		}		
		return(test)
	}

	/****
	* Generate a cross validation training set from a saved set of folds. 
	*/ 
	static Instances trainCV(Instances data,FoldSet foldset,int fold){
		def trainSampleNames = foldset.getTrainSamples(fold)					

		// Look up the instance indexes for this list of names...
		def trainSampleIdxs = data.nameListToIndexList(trainSampleNames)		

		// Copy each instance to the test set...
		Instances train = new Instances(data,trainSampleNames.size())

		trainSampleIdxs.each{idx->
			if (idx != -1) data.copyInstances(idx,train,1);	// -1 means no instance with that name
		}		
		return(train)
	}
	

	/***
	* Parse the predictions out of the results text. 
	*/ 
	static def parsePredictions(predictions){	
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

			//err.println "KJD DEBUG prediction line: $line"

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


