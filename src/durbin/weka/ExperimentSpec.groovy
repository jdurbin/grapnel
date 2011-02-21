package durbin.weka;

import weka.attributeSelection.*


/**
* Class to hold a single experiment specification. 
* 
* Currently each line is hard coded with classifier, attributeEval, etc. 
* Should replace with a map from these feature names to their values. 
*/ 
class ExperimentSpec{
   def classifier
   def attributeEval
   def attributeSearch
   def numAttributes
   def classAttribute

	 def discretization

   // Just to have something handy to print
   def classifierStr
   def attrEvalStr
   def attrSearchStr
   
    //time estimate:   {so batching can divide it up reasonably}
    
   def ExperimentSpec(line){
    def fields = line.split(",")
    classifierStr = fields[0]

    try{
      if (classifierStr != "None"){      
        classifier = WekaExplorer.classifierFromSpec(classifierStr)
      }else{
        classifier = null;
      }
    }catch(Exception e){
      System.err.println "WekaExplorer.classifierFromSpec failed on:"
      System.err.println classifierStr
      throw(e)
    }
    
    attrEvalStr = fields[1] 
    attributeEval = WekaExplorer.evalFromSpec(attrEvalStr)  
        
    attrSearchStr = fields[2]
    attributeSearch = WekaExplorer.searchFromSpec(attrSearchStr)
    
    numAttributes = (fields[3] as double) as int
    classAttribute = fields[4]     
		discretization = fields[5]
   }   
   
   String toString(){
     def rstr = classifierStr+","+attrEvalStr+","+attrSearchStr
     return(rstr);
   }    
}

//classifier, attributeEval,attributeSearch,numAttributes,classAttribute"
