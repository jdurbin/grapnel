package durbin.paradigm;

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
    classifier = ParadigmPipeline.classifierFromSpec(classifierStr)
    
    attrEvalStr = fields[1] 
    attributeEval = ParadigmPipeline.evalFromSpec(attrEvalStr)  
    
    
    // KJD Temporary until I look up how to do this with Class.forName()
    attrSearchStr = fields[2]
    attributeSearch = new Ranker()

    //attributeSearch = 
    // Need to convert this to a class...
    
    numAttributes = (fields[3] as double) as int
    classAttribute = fields[4]     
   }    
}

//classifier, attributeEval,attributeSearch,numAttributes,classAttribute"
