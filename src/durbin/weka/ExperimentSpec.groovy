package durbin.weka;

import weka.attributeSelection.*


/**
* Class to hold a single experiment specification. <br><br>
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

	 def ExperimentSpec(WekaMineResult wmr){
		classifierStr = wmr.classifier
		try{
			if (classifierStr != "None"){
				classifier = WekaMine.classifierFromSpec(classifierStr)
		  }else{
				classifier = null;
			}
			
			attrEvalStr = wmr.attrEval 
		  attributeEval = WekaMine.evalFromSpec(attrEvalStr)  

		  attrSearchStr = wmr.attrSearch
		  attributeSearch = WekaMine.searchFromSpec(attrSearchStr)

		  numAttributes = wmr.numAttrs
		  classAttribute = wmr.classAttr   
			discretization = wmr.discretization
			
		}catch(Exception e){
			System.err.println e
			throw(e)
		}						
	}

    
   def ExperimentSpec(String line){
    def fields = line.split(",")
    classifierStr = fields[0]

    try{
      if (classifierStr != "None"){      
        classifier = WekaMine.classifierFromSpec(classifierStr)
      }else{
        classifier = null;
      }
    }catch(Exception e){
      System.err.println "WekaMine.classifierFromSpec failed on:"
      System.err.println classifierStr
      throw(e)
    }
    
    attrEvalStr = fields[1] 
    attributeEval = WekaMine.evalFromSpec(attrEvalStr)  
        
    attrSearchStr = fields[2]
    attributeSearch = WekaMine.searchFromSpec(attrSearchStr)
    
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
