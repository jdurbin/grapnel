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
  
    // Evaluation2 is a modified version of weka Evaluation that saves
    // the classifiers created in a cross-validation experiment for 
    // later access. 
    Evaluation2 eval;

    List<EvaluationResult> crossValidateModel(classifier,data,cvFolds,rng){
            
      // Create a classifier from the name...
      // By using filtered classifer to remove ID, the cross-validation
      // wrapper will keep the original dataset and keep track of the mapping 
      // between the original and the folds (minus ID). 
      def filteredClassifier = new FilteredClassifier()
      def removeTypeFilter = new RemoveType();
			//removeTypeFilter.setOptions("-T string");  
          
      filteredClassifier.setClassifier(classifier)
      filteredClassifier.setFilter(removeTypeFilter)

      def results = new ArrayList<EvaluationResult>()

      // Perform cross-validation of the model..
      eval = new Evaluation2(data)
      def predictions = new StringBuffer()
      eval.crossValidateModel(filteredClassifier,data,cvFolds,rng,predictions,
        new Range("first,last"),false)
        
//      System.err.println "DEBUG PREDICTIONS:"+predictions

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


