package durbin.weka;

import weka.attributeSelection.*

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


/**
* Class to hold a single experiment specification. <br><br>
* 
* Currently each line is hard coded with classifier, attributeEval, etc. 
* Should replace with a map from these feature names to their values. 
*/ 
class ExperimentSpec{
   def filter          // unsupervised filter...
   def classifier
   def attributeEval
   def attributeSearch
   def numAttributes
   def classAttribute

	 def discretization
	
	 def dataFile

   // Just to have something handy to print
	 def filterStr
   def classifierStr
   def attrEvalStr
   def attrSearchStr
   
    //time estimate:   {so batching can divide it up reasonably}


	 def ExperimentSpec(WekaMineResult wmr){
		classifierStr = wmr.classifier
		try{
			if (classifierStr.toLowerCase() != "none"){
				classifier = classifierFromSpec(classifierStr)
		  }else{
				classifier = null;
			}
			
			filterStr = wmr.filter
			if (filterStr.toLowerCase() != "none"){
				filter = filterFromSpec(filterStr)  // Why is this in WekaMine and not here? 
			}else{
				filter = null
			}
			
			attrEvalStr = wmr.attrEval 
		  attributeEval = evalFromSpec(attrEvalStr)  

		  attrSearchStr = wmr.attrSearch
		  attributeSearch = searchFromSpec(attrSearchStr)

		  numAttributes = wmr.numAttrs
		  classAttribute = wmr.classAttr   
			discretization = wmr.discretization
			
			dataFile = wmr.dataFile
			
		}catch(Exception e){
			System.err.println e
			throw(e)
		}						
	}

    
	def ExperimentSpec(String line){
		ExperimentSpec(line,WekaMineResult.defaultHeadingMap())
	}
	
		/***
		* Takes a comma separated string and creates an experiment from it. 
		* headings2Cols gives the index of the column where the given field is derived from
		* this can come from an output heading, or from WekaMineResult.defaultHeadingMap
		* The idea is for all knowledge of the heading order to be confined to WekaMineResult
		* so that we don't have to worry about things getting out of sync. 
		*/ 
   def ExperimentSpec(String line,headings2Cols){
    def fields = line.split("\t")

	//System.err.println "DEBUG fields: "+fields
	//System.err.println "DEBUG headings2cols: "+headings2Cols
    classifierStr = fields[headings2Cols['classifier']]

	// add some code to take 
	// SMO -M -C 3.0 -L 0.0001 -P 1.0E-12 -N 0 -V -1 -W 1 -K "PolyKernel -C 250007 -E 1"
	// And transform it into: 
	// weka.classifiers.functions.SMO -M -C 3.0 -L 0.0001 -P 1.0E-12 -N 0 -V -1 -W 1 -K "weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1
	
	//smor = /^(?!\.)SMO/
	//classifierStr.replaceFirst(smor,"weka.classifiers.functions.SMO")

    try{
      if (classifierStr.toLowerCase() != "none"){      
        classifier = classifierFromSpec(classifierStr)
      }else{
        classifier = null;
      }
    }catch(Exception e){
      System.err.println "WekaMine.classifierFromSpec failed on:"
      System.err.println classifierStr
      throw(e)
    }
    
		filterStr = fields[headings2Cols['filter']]
		if (filterStr.toLowerCase() != "none"){
			// Why is this in WekaMine and not here? 
			filter = filterFromSpec(filterStr)  
		}else{
			filter = null
		}

    attrEvalStr = fields[headings2Cols['attrEval']] 
    attributeEval = evalFromSpec(attrEvalStr)  
        
    attrSearchStr = fields[headings2Cols['attrSearch']]
    attributeSearch = searchFromSpec(attrSearchStr)
    
    numAttributes = (fields[headings2Cols['numAttrs']] as double) as int
    classAttribute = fields[headings2Cols['classAttr']]     
		discretization = fields[headings2Cols['discretization']]
   }   
   
   String toString(){
     def rstr = filterStr+"\t"+attrEvalStr+"\t"+attrSearchStr+"\t"+numAttributes+"\t"+classifierStr+"\t"+classAttribute+"\t"+discretization+"\t"+dataFile
     return(rstr);
   } 

	 String toOutputString(){
     def rstr = filterStr+"\t"+attrEvalStr+"\t"+attrSearchStr+"\t"+numAttributes+"\t"+classifierStr+"\t"+classAttribute+"\t"+discretization
     return(rstr);
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


   def filteredClassifierFromClassifier(classifier){
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
   def evalFromSpec(attributeEvalSpec){    
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
   def searchFromSpec(attributeSearchSpec){   
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
