
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

public class RAUtils{
  
  def raRoot;
  def classifiersOut;
  def subgroupsOut;
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
    resultsOut = new File("${raRoot}.results.ra")    
    tasksOut = new File("${raRoot}.tasks.ra")    
  }

  /***
  *
  */
  def writeFeatureSelection(){
    
    if (!bFirstTime) return;
    bFirstTime = false;
    
    def outStr = """
      name\tfeatureSelection1\n
      type\tfeatureSelection\n
      label\tNone\n
      parameters\tNA\n\n
    """
    
    classifiersOut<<outStr
  }
  
  /***
  *
  */ 
  def writeSubgroup(instances,params){
  
    
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
    
    
    // Currently, I'll have one task per subgrouping... I think...
    def task = "${className}" as String
    def outStr = """
    name\t$task\n
    label\t${className} gi50 response\n
    type\ttask\n\n"""
    tasksOut <<outStr
        
    def subgroup = "${className}subgroup" as String
    
    def lowSampOut = lowSamples.join(",")
    def highSampOut = highSamples.join(",")
    
    outStr = """
      name\t$subgroup\n
      type\tsubgrouping\n
      label\t${className}\n
      parameters\t$paramStr\n
      subgroup1label\tlow\n
      subgroup1\t    
      lowSampOut\n
      subgroup2label\thigh\n
      subgroup2\t
      highSampOut\n\n"""
    
    subgroupsOut << outStr
    
    return([task,subgroup])    
  }
  
  /***
  *
  */ 
  def writeClassifier(classifierDescription,idx){
    // If we've already seen it, don't write it out...
    if (recordedClassifiers.contains(classifierDescription)) return(null);
    else recordedClassifiers.add(classifierDescription);
    
    def options = Utils.splitOptions(classifierDescription)
    def classifierName = options[0]
    classifierName = classifierName.replaceAll(":","")
    options[0] = ""
    def opt = options.join(" ")
    opt = opt.replaceAll("\"","") // Remove pesky quotes...    

    def classifierID = "${classifierName}${idx}".toString()
    def outStr = """
      name\t$classifierID\n
      type\t$classifierName\n
      label\t$classifierID\n
      parameters\t$opt\n\n"""
      
    classifiersOut<<outStr;    
    return(classifierID);
  }
  
  /***
  *
  */
  def writeResults(results,classID,task,subgroup,idx){
  
    
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
        
    def resultsStr = """
      name\t${task}Job${idx}\n
      type\tjob\n
      task\t${task} gi50 response\n
      dataset\tgrayCellLine\n
      subgrouping\t$subgroup\n
      classifier\t$classID\n
      featureSelection\tNone\n
      accuracyType\tleave one out\n
      samples\t$samples\n
      trainingAccuracies\t$training\n
      testingAccuracies\t$testing\n\n\n"""

    resultsOut << resultsStr
  }
}

 