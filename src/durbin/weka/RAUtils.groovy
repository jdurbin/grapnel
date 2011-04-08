
import weka.core.*
import weka.filters.*

import weka.classifiers.*
import weka.classifiers.evaluation.*
import weka.classifiers.Evaluation

import weka.classifiers.meta.*
import weka.attributeSelection.*

import weka.core.converters.ConverterUtils.DataSource
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.meta.FilteredClassifier

/***
* Some utilities to help with writing .ra files used by hgClassifiers
* to import classifier results into bioInt.   
* <pre>
* Notes:
* 
* 'type' is a keyword used by loader to determine type of each entry, including:
*    	classifier
*			featureSelection
*			job
*			subgrouping
*			task
* label is the actual database key for each entry 
* name is just descriptive
* </pre>
*/ 
public class RAUtils{
  
  def raRoot;
  def classifiersOut;
  def subgroupsOut;
	def featureSelectionsOut;
  def resultsOut;
  def tasksOut;
  
  def recordedClassifiers = [] as Set
  def recordedSubgroupings = [] as Set
  
  def bFirstTime = false;
  
  /***
  * 
  */
  def RAUtils(raRoot){
    this.raRoot = raRoot
    classifiersOut = new File("${raRoot}.classifiers.ra")
    subgroupsOut = new File("${raRoot}.subgroups.ra")
		featureSelectionsOut = new File("${raRoot}.subgroups.ra")
    resultsOut = new File("${raRoot}.results.ra") // aka jobs    
    tasksOut = new File("${raRoot}.tasks.ra")    
  }

  /***
  *
  */
  def writeFeatureSelection(jobidx){
    
    if (!bFirstTime) return;
    bFirstTime = false;
    
    def outStr = """
name\tfeatureSelection$jobidx
type\tfeatureSelection
label\tNone
parameters\tNA\n"""
    
    featureSelectionsOut<<outStr
  }
  
  /***
  * name	gemcitabineSubgrouping1
  * type	subgrouping
  * label	Gemcitabine response natural split
  * parameters	Split around -logIC50*100 ~ 512
  * subgroup1label	Sensitive
  * subgroup1	Tu8988T,SW1990,Suit2,Panc1,10.05,3.27,2.13,Miapaca2,HupT3,Dan_G,Colo357,CFPac1,
  * subgroup2label	Resistant
  * subgroup2	Tu8988S,Tu8902,6.03,Mpanc96,HPAF_II,HPAC,Capan2,
  */ 
  def writeSubgroup(instances,params,jobidx){
  
    
    def IDAttr = instances.attribute("ID")
  
    def classAttr = instances.classAttribute();
    def className = classAttr.name()
    
    if (recordedSubgroupings.contains(className)) return([null,null]);
    else recordedSubgroupings.add(className)

    def paramStr = "default split"
    if (params.useUpperLowerQuartile){
      paramStr = "Upper and lower quartile split."
    }else if (params.useMedianDiscretization){
      paramStr = "Median split."
    }else if (params.discretizeCutoffs){      
      lowerBound = params.discretizeCutoffs.low
      upperBound = params.discretizeCutoffs.high
      paramsStr = "Split on values low: $lowerBound   high: $upperBound"
    }

    // Create the list of low and high samples...
    def lowSamples = []
    def highSamples = []    
    (0..<instances.numInstances()).each{idx->
      def instance = instances.instance(idx)
      def attValue = instance.stringValue(classAttr)
      def ID = instance.stringValue(IDAttr)
      if (attValue == "high"){
        lowSamples << ID
      }else{
        highSamples << ID
      }
    }
    
    def taskID = "$className$jobidx" as String
    
    // Currently, I'll have one task per subgrouping... I think...
    def outStr = """
name\t$taskID
label\t${className}
type\ttask\n"""

    tasksOut <<outStr
        
    //def subgroup = "${className}" as String
    def subgroupID = "${className}_$jobidx" as String
    
    
    def lowSampOut = lowSamples.join(",")
    def highSampOut = highSamples.join(",")
    
    outStr = """
name\t$subgroupID
type\tsubgrouping
label\t$subgroupID
parameters\t$paramStr
subgroup1label\tlow
subgroup1\t$lowSampOut
subgroup2label\thigh
subgroup2\t$highSampOut\n\n"""    
subgroupsOut << outStr
    
    
    return([taskID,subgroupID])    
  }
  
  /***
  *
  */ 
  def writeClassifier(classifierDescription,jobidx){
    // If we've already seen it, don't write it out...
    if (recordedClassifiers.contains(classifierDescription)) return(null);
    else recordedClassifiers.add(classifierDescription);
    
		def classifierName = getClassifierName(classifierDescription)
		def opt = getClassifierOptions(classifierDescription)

    def classifierID = "${classifierName}${jobidx}".toString()
    //def classifierID = classifierName
    
    def outStr = """
name\t$classifierID
type\tclassifier
label\t$classifierID
parameters\t$opt\n"""
      
    classifiersOut<<outStr;    
    return(classifierID);
  }
  
def getClassifierName(description){
	def options = Utils.splitOptions(description)
  def classifierName = options[0]
	return(classifierName)
}


def getClassifierOptions(description){
	def options = Utils.splitOptions(description)
  def classifierName = options[0]
  classifierName = classifierName.replaceAll(":","")
  options[0] = ""
  def opt = options.join(" ")
  opt = opt.replaceAll("\"","") // Remove pesky quotes...	
	return(opt)
}


  /***
	* a.k.a job
	* I don't know why we have to repeat all the information about classifier 
	* parameters and so on, but what the hell..
	* 
	* 
  * name	gemcitabineJob1
  * type	job
  * task	Gemcitabine Response
  * dataset	collisson2010paradigm_cl
  * subgrouping	Gemcitabine response natural split
  * classifier	Standard UCSC cNMF
  * featureSelection	None
  * accuracyType	Leave one out
  * samples	YAPC,Tu8988T,Tu8988S,Tu8902,T3M4,SW1990,Suit2,Panc1,10.05,8.13,6.03,5.04,3.27,2.13,2.03,Mpanc96,Miapaca2,HupT4,HupT3,Hs766T,HPAF_II,HPAC,HCG25,Dan_G,CFPac1,Capan2,Capan1,BxPC3,ASPC1,Colo357,
  * trainingAccuracies	NULL,1,1,0.448275862068966,NULL,1,1,1,1,NULL,1,NULL,1,1,NULL,1,1,NULL,1,NULL,1,1,NULL,1,1,1,NULL,NULL,NULL,0.896551724137931,
  * testingAccuracies	NULL,1,1,0,NULL,1,1,1,1,NULL,1,NULL,1,1,NULL,0,1,NULL,1,NULL,1,1,NULL,0,0,1,NULL,NULL,NULL,1,  
  */
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

		def opt = getClassifierOptions(classifierDescription)
        
    def resultsStr = 

"""
name\t${task}Job${idx}
type\tjob
task\t${task}
dataset\tdatasetPlaceHolder
subgrouping\t$subgroup
classifier\t$classID
classifierParameters\t$opt
featureSelection\tNone
featureSelectionParameters\tNA
accuracyType\t10x crossvalidation
samples\t$samples
trainingAccuracies\t$training
testingAccuracies\t$testing\n"""

    resultsOut << resultsStr
  }
}

 