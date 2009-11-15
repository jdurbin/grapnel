#!/usr/bin/env groovy 

package durbin.weka;

import weka.core.*
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.evaluation.*;
import java.util.Random


public class EvaluationResult{
  
  def EvaluationResult(a,p,s,m,pr){
    actual = a
    predicted = p
    sample = s
    isMatch = m
    probability = pr
  }
  
  String toString(){
    def rval = actual+"\t"+predicted+"\t"+sample+"\t"+isMatch+"\t"+probability;
    return(rval)
  }
  
  def actual;
  def predicted;
  def sample;
  def probability
  boolean isMatch;
}

/*********************************************************
* A class to encapsulate the basics of running a cross validation
* experiment. 
*/ 
public class CVUtils{
  

    List<EvaluationResult> crossValidateModel(classifier,data,cvFolds,rng){
      
      def results = new ArrayList<EvaluationResult>()

      // Perform cross-validation of the model..
      def eval = new Evaluation(data)
      def predictions = new StringBuffer()
      eval.crossValidateModel(classifier,data,cvFolds,rng,predictions,
        new Range("first,last"),false)

      def lines = predictions.toString().split("\n")

      //System.err.println predictions

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
        def predicted = m[0][2]
        //def spaces = m[0][3] // or + 
        def probability = m[0][4]
        def sample = m[0][5]
        //println "$actual $predicted [$spaces] [$probability] $sample"
                
        def result = new EvaluationResult(actual,predicted,sample,
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


