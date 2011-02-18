
package durbin.weka;

import weka.classifiers.evaluation.*
import weka.core.converters.*
import weka.core.*
import weka.attributeSelection.*;

import durbin.util.*;



/***************************
*  Utilities to help with writing summary results from Weka pipeline.
*/
class WekaPipelineOutput{

  /***
  * Returns a heading for the csv file.  MUST match exactly what is output by 
  * getFormattedEvaluationSummary. 
  */ 
  static String getFormattedSummaryHeading(){
    def rval = "jobID,samples,pctCorrect,precision0,recall0,precision1,recall1,tp1,fp1,tn1,fn1,rms,roc"
    
    return(rval)
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
  static String cvFeatureSelections(data,classifiers,maxToReport){  
    
    def attList = [] 
    

    // The classifiers that reach the cross validation code will be 
    // twice wrapped.   Once as an attribute selected classifier, and 
    // again as a filtered classifier (to remove String ID)
    //
    
    def attribute2Score = [:]
    def intersection = [] as Set
     
    def bFirstTime = true; 
    classifiers.each{fc->  // each FilteredClassifier
            
      def asClassifier = fc.getClassifier() // AttributeSelectedClassifier
      def attributeSelection = asClassifier.getAttributeSelection()

      def rankedAttrs = attributeSelection.rankedAttributes()
            
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
        score = score.round(4)
      
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
         score = score.round(4)             
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
  static void appendSummaryLine(jobIdx,data,out,numInstances,experiment,eval){
      // Append a summary line to a file. 
      def summaryLine = getFormattedEvaluationSummary(numInstances,eval)
      out << "$jobIdx,$summaryLine,${experiment.classifierStr},${experiment.attrEvalStr},${experiment.attrSearchStr},${experiment.numAttributes},${experiment.classAttribute},${experiment.discretization}"    
      out<<"\n"      
  } 

 	/****
  * Appends a results summary line to the output stream out, tacking on the top features for classifiers. 
  */ 
  static void appendFeaturesLine(jobIdx,data,out,numInstances,experiment,eval,maxFeaturesToReport){
      // Append a summary line to a file. 
      def summaryLine = getFormattedEvaluationSummary(numInstances,eval)
      out << "$jobIdx,$summaryLine,${experiment.classifierStr},${experiment.attrEvalStr},${experiment.attrSearchStr},${experiment.numAttributes},${experiment.classAttribute},${experiment.discretization}"

      // Figure out the feature selections across cross validation folds...
      if (maxFeaturesToReport != 0){  
        out << ","
        out << cvFeatureSelections(data,eval.getCVClassifiers(),maxFeaturesToReport)
      }      
      out<<"\n"      
  }
  	
 	/****
  * Appends a results summary line to the output stream out, tacking on the top features for classifiers. 

  */ 
  static void appendSamplesLine(jobIdx,data,out,numInstances,experiment,eval,results){
      // Append a summary line to a file. 
      def summaryLine = getFormattedEvaluationSummary(numInstances,eval)
      out << "$jobIdx,$summaryLine,${experiment.classifierStr},${experiment.attrEvalStr},${experiment.attrSearchStr},${experiment.numAttributes},${experiment.classAttribute},${experiment.discretization}"
			
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
				}
			}			
      out<<"\n"      
  }

	/*
	* samples	YAPC,Tu8988T,Tu8988S,Tu8902,T3M4,SW1990,Suit2,Panc1,10.05,8.13,6.03,5.04,3.27,2.13,2.03,Mpanc96,Miapaca2,HupT4,HupT3,Hs766T,HPAF_II,HPAC,HCG25,Dan_G,CFPac1,Capan2,Capan1,BxPC3,ASPC1,Colo357,
  * trainingAccuracies	NULL,1,1,0.448275862068966,NULL,1,1,1,1,NULL,1,NULL,1,1,NULL,1,1,NULL,1,NULL,1,1,NULL,1,1,1,NULL,NULL,NULL,0.896551724137931,
  * testingAccuracies	NULL,1,1,0,NULL,1,1,1,1,NULL,1,NULL,1,1,NULL,0,1,NULL,1,NULL,1,1,NULL,0,0,1,NULL,NULL,NULL,1,  
  
  def writeResults(classifierDescription,results,classID,task,subgroup,idx){
  
    def samplesOut = []
    def trainAcc = []
    def testingAcc = []
    results.each{r->
      samplesOut<<r.instanceID
      trainAcc << "0"
      if (r.isMatch) testingAcc << "1"
      else testingAcc << "0"
    }
    def samples = samplesOut.join(",")
    def training = trainAcc.join(",")
    def testing = testingAcc.join(",")
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