
package durbin.weka;

import weka.classifiers.evaluation.*
import weka.core.converters.*
import weka.core.*
import weka.attributeSelection.*;

import durbin.util.*;

class WekaMineResult{
	
	static err = System.err
	
	int jobID
	int	samples
	float pctCorrect
	float precision0
	float recall0
	float precision1
	float recall1
	int tp1
	int fp1
	int tn1
	int fn1
	float rms
	float roc
	String classifier
	String attrEval
	String attrSearch
	int numAttrs
	String classAttr
	String discretization
	String dataFile
	
	static def headingStr = "attrEval,attrSearch,numAttrs,classifier,classAttr,discretization,dataFile,Break,jobID,samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc"    		
	static def expHeadingStr = "attrEval,attrSearch,numAttrs,classifier,classAttr,discretization"
	
	
	String toString(){
		def sb = []
		sb << attrEval
		sb << attrSearch
		sb << numAttrs
		sb << classifier
		sb << classAttr
		sb << discretization
		sb << dataFile
		sb << "*"
		sb << jobID
		sb << samples
		sb << pctCorrect
		sb << precision0
		sb << recall0 
		sb << precision1
		sb << recall1
		sb << tp1
		sb << fp1
		sb << tn1
		sb << fn1
		sb << rms
		sb << roc
		
		def rval = sb.join(",")
		return(rval)    		
	}
	
	/*** 
	* If we're not given a heading map, we can assume it comes from the default...
	*/ 
	static def defaultHeadingMap(){
		def headings = headingStr.split(",")
		
		//err.println "DEBUG: headings:"+headings
		
		def headings2Cols = [:]
		headings.eachWithIndex{h,i-> headings2Cols[h] = i}		
		return(headings2Cols)
	}
		
	/***
	* If we're given a heading, use that...
	*/ 
	def WekaMineResult(String line,headings2Cols){
		parse(line,headings2Cols)		
	}
	
	def parse(line,headings2Cols){
		def fields = line.split(",")				
						
		//err.println "DEBUG fields: "+fields
		//err.println "DEBUG headings2Cols"+headings2Cols				
						
		attrEval = fields[headings2Cols['attrEval']] as String
		attrSearch = fields[headings2Cols['attrSearch']] as String
		numAttrs = fields[headings2Cols['numAttrs']] as int
		
		dataFile = fields[headings2Cols['dataFile']] as String
		
		jobID = fields[headings2Cols['jobID']] as int
		samples = fields[headings2Cols['samples']] as int
		pctCorrect = fields[headings2Cols['pctCorrect']] as float
		precision0 = fields[headings2Cols['precision0']] as float
		recall0	= fields[headings2Cols['recall0']] as float
		precision1 = fields[headings2Cols['precision1']] as float
		recall1 = fields[headings2Cols['recall1']] as float
		tp1 = fields[headings2Cols['tp1']] as int
		fp1 = fields[headings2Cols['fp1']] as int
		tn1 = fields[headings2Cols['tn1']] as int
		fn1 = fields[headings2Cols['fn1']] as int
		rms = fields[headings2Cols['rms']] as float
		roc = fields[headings2Cols['roc']] as float
		
		classifier = fields[headings2Cols['classifier']] as String 		
		classAttr = fields[headings2Cols['classAttr']] as String
		discretization = fields[headings2Cols['discretization']] as String
	}
	
	def toExperiment(){
		return(new ExperimentSpec(this))
	}	
}


/***
*  Utilities to help with writing summary results from Weka pipeline.
*/
class WekaMineResults extends ArrayList<WekaMineResult>{

	static err = System.err

	/***
	* WARMING: MUST match summary heading in wekaMineResult
	* "attrEval,attrSearch,numAttrs,clsssifier,classAttr,discretization,jobID,samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc"    				
	*/
	static String getFullSummaryLine(jobIdx,data,experiment,eval,dataName){
		def summaryLine = getFormattedEvaluationSummary(data.numInstances(),eval)
    def fullSummaryLine="${experiment.attrEvalStr},${experiment.attrSearchStr},${experiment.numAttributes},${experiment.classifierStr},${experiment.classAttribute},${experiment.discretization},${dataName},*,$jobIdx,$summaryLine"		
		return(fullSummaryLine)
	}


	/***
	* Read results from a results summary file...
	*/ 
	def WekaMineResults(resultsFile){
		new File(resultsFile).withReader{r->
			def headings = r.readLine().split(",")						
			def heading2Col = [:]
			headings.eachWithIndex{h,i-> heading2Col[h] = i}
			
			r.eachLine{line->
				def wmr = new WekaMineResult(line,heading2Col)
				add(wmr)
			}			
		}		
	}
	
	
	/***
	* Convert results into a list of experiments
	*/ 
	def toExperiments(){
		def rlist = []
		this.each{result->			
			def experiment = result.toExperiment()
			rlist.add(experiment)
		}
		return(rlist)
	}
	


  /***
  * Returns a heading for the csv file.  MUST match exactly what is output by 
  * getFormattedEvaluationSummary. 
  */ 
  static String getFormattedSummaryHeading(){
    return(WekaMineResult.headingStr)
  }
  
  static String getAttributesHeading(dataFile,maxFeatures){
	
		// KJD TODO:  actually fill in real headings, eh? 
		def numFeatures =  FileUtils.fastCountLines(dataFile) -1; // Minus 1 for heading...
	
		System.err.println "$numFeatures attributes in data.  reporting a maximum of $maxFeatures"
	
    def list = []
    if (maxFeatures > 0){
      (0..<numFeatures).each{i->
        list << "Attribute$i" as String
      }
    }
    def rval = list.join(",")
    return(rval)    
  }



  /****
  * Returns a formatted comma separated string of values from an evaluation. 
  * The format of the returned string is: <br><br>
  * 
  * prefix,samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc 
  *
  */ 
  static String getFormattedEvaluationSummary(numInstances,eval){
    def samples = numInstances
    def pctCorrect = eval.pctCorrect() 
    def precision1 = eval.precision(1)
    def recall1 = eval.recall(1)    
    def precision0 = eval.precision(0)
    def recall0 = eval.recall(0)
    def tp1 = eval.numTruePositives(1) as Integer
    def fp1 = eval.numFalsePositives(1) as Integer
    def tn1 = eval.numTrueNegatives(1) as Integer
    def fn1 = eval.numFalseNegatives(1) as Integer
    def rms = eval.rootMeanSquaredError()
    def roc = eval.weightedAreaUnderROC()    
    def rval = sprintf("%d,%.4g,%.4g,%.4g,%.4g,%.4g,%d,%d,%d,%d,%.4g,%.4g",
              samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc)
  
    return(rval)    
  }
  
  
  /***
  * When no classification is performed, only attribute selection, these fields will have no meaning. 
  * They are reported pro forma so that the same code can handle the resulting csv file. 
  */ 
  static String getFeatureOnlyEvaluationSummary(numInstances){
    int samples = numInstances
    def pctCorrect = 0 as double
    def precision1 = 0 as double
    def recall1 = 0 as double
    def precision0 = 0 as double
    def recall0 = 0 as double
    def tp1 = 0 as int
    def fp1 = 0 as int
    def tn1 = 0 as int
    def fn1 = 0 as int
    def rms = 0 as double
    def roc = 0 as double
    def rval = sprintf("%d,%.4g,%.4g,%.4g,%.4g,%.4g,%d,%d,%d,%d,%.4g,%.4g",
              samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc)
  
    return(rval)    
  }


	/***
  * Returns a string containing the ordered and selected features and their 
  * feature selection scores.  The output will be like: 
  * 
  * 68_IHH N/PTCH1~0.6930,105_BMP2-4/CHRDL1~0.64456,132_CELL_MIGRATION~0.4630,...
  * 
  * 
  */ 
  static String cvFeatureSelections(data,attributeSelections,maxToReport){  
    
    def attList = [] 
    

//		err.println "DEBUG: cvFeatureSelections"

    // The classifiers that reach the cross validation code will be 
    // twice wrapped.   Once as an attribute selected classifier, and 
    // again as a filtered classifier (to remove String ID)
    //
    
    def attribute2Score = [:]
    def intersection = [] as Set
     
    def bFirstTime = true; 
		
		attributeSelections.each{thinAttributes->                 
            
			//def rankedAttrs = attributeSelection.rankedAttributes() // this is a double[][]
			def rankedAttrs = thinAttributes.getAttributes()  // Returns the double[][] we tucked away in this thin wrapper. 

      def reportCount = 0;
      
      def keySet = [] as Set
      rankedAttrs.each{attrRank->
        
        // Only record maxToReport then skip over rest...
				//System.err.println "maxToReport: $maxToReport\treportCount: $reportCount"
        if (reportCount >= maxToReport) return;
        reportCount++
        
        // +1 because the CV was done on Filtered instances that have removed 
        // the 0th (ID) attribute.  KJD: While in practice I have always been 
        // making the ID the first (0) attribute, I worry slightly that assuming 
        // so is risky... but what a mess if ID is 5th attribute and you remove it...
        def attIdx = (attrRank[0] as int)+1        
        def attName = data.attribute(attIdx).name()
        def score = attrRank[1] as Double
        //score = score.round(4)
      
        // Save every pair attributes and scores... save only the max score 
        // for each attribute...
        if (attribute2Score.keySet().contains(attName)){
          def oldScore = attribute2Score[attName]
          if (score > oldScore) {          
            attribute2Score[attName] = score;
          }
        }else{
          attribute2Score.put(attName,score)
        }
        
        // keySet for just this one cv fold...        
        keySet << attName        
      }
      
      if (bFirstTime){
        intersection = keySet
        bFirstTime=false;
      }else{      
        intersection = intersection.intersect(keySet)
      }      
    }


    // Scores 
    def valueSortedKeys = attribute2Score.keySet().sort{-attribute2Score[it]}

    def pairs = []
    valueSortedKeys.eachWithIndex{attr,i -> 
      if (i < maxToReport){
        pairs << "$attr~${attribute2Score[attr]}" 
      }
    }    
    return(pairs.join(","))  
  }
  
  /****
  * Appends a results summary line for AttributeSelection ONLY experiments 
  * to the output stream out. 
  *
  * KJD TODO: Need to add jobID... both job# and cfgID
  */ 
   static void appendAttributeSelectionSummaryLine(jobIdx,data,out,
     numInstances,experiment,
     attributeSelection,maxFeaturesToReport){
       
     // Append a summary line to a file. 
     def summaryLine = getFeatureOnlyEvaluationSummary(numInstances) // Basically a dummy record.
     def lineOut = "$jobIdx,$summaryLine,${experiment.classifierStr},${experiment.attrEvalStr},${experiment.attrSearchStr},${experiment.numAttributes},${experiment.classAttribute}" as String
     
     if (maxFeaturesToReport != 0){                       
       def rankedAttrs = attributeSelection.rankedAttributes()
       def reportCount = 0;
       
       def attrList = []
       def keySet = [] as Set
       rankedAttrs.each{attrRank->
        // Only record maxToReport then skip over rest...
        if (reportCount >= maxFeaturesToReport) return;
        reportCount++

         // +1 because the CV was done on Filtered instances that have removed 
         // the 0th (ID) attribute.  KJD: While in practice I have always been 
         // making the ID the first (0) attribute, I worry slightly that assuming 
         // so is risky... but what a mess if ID is 5th attribute and you remove it...
         def attIdx = (attrRank[0] as int)+1        
         def attName = data.attribute(attIdx).name()
         def score = attrRank[1] as Double
         //score = score.round(4)             
         attrList << "$attName~$score" as String
       }
       lineOut = lineOut + ","
       lineOut = lineOut + attrList.join(",")
      }
      lineOut = lineOut +"\n" as String    
      out << lineOut            // Build the entire string so that write is atomic...
    }


  /****
  * Appends a results summary line to the output stream out
  *
  */ 
  static void appendSummaryLine(jobIdx,data,out,experiment,eval,dataName){
      // Append a summary line to a file. 
			out << getFullSummaryLine(jobIdx,data,experiment,eval,dataName)
      out<<"\n"      
  } 

 	/****
  * Appends a results summary line to the output stream out
  *
  */ 
  static void appendSummaryLineWithHoldout(jobIdx,data,out,experiment,eval,dataName,holdoutAcc){
	
			holdoutAcc = holdoutAcc.round(4)
	
      // Append a summary line to a file. 
			out << getFullSummaryLine(jobIdx,data,experiment,eval,dataName)
			out <<","
			out << holdoutAcc
      out<<"\n"      
  }


	/****
  * Appends a results summary line to the output stream out, tacking on the top features for classifiers. 
  */ 
  static void appendFeaturesLine(jobIdx,data,out,experiment,eval,maxFeaturesToReport,dataName){
      // Append a summary line to a file. 
			def summaryLine = getFullSummaryLine(jobIdx,data,experiment,eval,dataName)
			out << summaryLine			
			
			//err.println "DEBUG: maxFeaturesToReport: "+maxFeaturesToReport
			
      // Figure out the feature selections across cross validation folds...
      if (maxFeaturesToReport != 0){  		
        out << ","
        out << cvFeatureSelections(data,eval.getCVAttributeSelections(),maxFeaturesToReport)
      }      
      out<<"\n"      
  }
  	
 	/****
  * Appends a results summary line to the output stream out, tacking on the top features for classifiers. 
  */ 
  static void appendSamplesLine(jobIdx,data,out,experiment,Evaluation2 eval2,dataName){
//																List<EvaluationResult> results,dataName){
																																		
      // Append a summary line to a file. 
      def summaryLine = getFormattedEvaluationSummary(data.numInstances(),eval2)
			def classAttribute = data.classAttribute()
			def  predictions = eval2.m_Predictions
			
			//err.println "predictions.size: "+predictions.size()
			//err.println "results.size: "+results.size()
			
			out << getFullSummaryLine(jobIdx,data,experiment,eval2,dataName)
			for(i in 0..<predictions.size()){
				out<<","
				def id = data.attribute("ID")
				def p = (NominalPredictionPlus) predictions.elementAt(i)
				def sampleName = p.instanceName
				def actual = classAttribute.value((int)p.actual())
				def predicted = classAttribute.value((int)p.predicted())	
				def dist = p.distribution() as ArrayList
				def distStr = dist.join(":")
				def margin = p.margin()
				out<< "$sampleName~$actual;$predicted;$distStr"
			}
			out<<"\n"
			
			/* Old way of getting these values...
			if (results.size() >0){						
				results.each{r->
					out<<","
					def sampleID = r.instanceID
					def actual = r.actual
					def predicted = r.predicted
					//err.println "ACTUAL{$actual} PREDICTED{$predicted}"
					def probability = r.probability
					def resultStr = "$sampleID~$actual;$predicted;$probability"
					out<<resultStr
					err.println "resultStr: $resultStr"
				}
			}			
      out<<"\n"      
			*/ 
  }

	/**
	* Saves the ROC curve from a cross validation experiment. 
	*/
  static void saveROC(eval,rocDir,className,numFeatures){
   // Save the ROC values for later plotting. 
    ThresholdCurve tc = new ThresholdCurve();
    Instances curve = tc.getCurve(eval.predictions());
    
    ArffSaver saver = new ArffSaver();
    saver.setInstances(curve);
    def curveFile = new File("${rocDir}${className}.${numFeatures}.arff".toString())
    saver.setFile(curveFile);
    saver.writeBatch();
  }
}