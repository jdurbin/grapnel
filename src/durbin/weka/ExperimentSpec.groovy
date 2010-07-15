package durbin.weka;

import weka.attributeSelection.*


/**
* Class to hold a single experiment specification. 
*/ 
class ExperimentSpec{
   def classifier
   def attributeEval
   def attributeSearch
   def numAttributes
   def classAttribute
   
   // Just to have something handy to print
   def classifierStr
   def attrEvalStr
   def attrSearchStr
   
    //time estimate:   {so batching can divide it up reasonably}
    
   def ExperimentSpec(line){
    def fields = line.split(",")
    classifierStr = fields[0]

    try{
      classifier = WekaExperimentPipeline.classifierFromSpec(classifierStr)
    }catch(Exception e){
      System.err.println "WekaExperimentPipeline.classifierFromSpec failed on:"
      System.err.println classifierStr
      throw(e)
    }
    
    attrEvalStr = fields[1] 
    attributeEval = WekaExperimentPipeline.evalFromSpec(attrEvalStr)  
        
    attrSearchStr = fields[2]
    attributeSearch = WekaExperimentPipeline.searchFromSpec(attrSearchStr)
    
    numAttributes = (fields[3] as double) as int
    classAttribute = fields[4]     
   }   
   
   String toString(){
     def rstr = classifierStr+","+attrEvalStr+","+attrSearchStr
     return(rstr);
   }    
}

//classifier, attributeEval,attributeSearch,numAttributes,classAttribute"
