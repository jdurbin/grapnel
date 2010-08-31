
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
    
    classifiersOut<<"name\tfeatureSelection1\n"
    classifiersOut<<"type\tfeatureSelection\n"
    classifiersOut<<"label\tNone\n"
    classifiersOut<<"parameters\tNA\n"
    classifiersOut<<"\n"
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
    tasksOut <<"name\t$task\n"
    tasksOut <<"label\t${className} gi50 response\n"
    tasksOut <<"type\ttask\n"
    tasksOut <<"\n"
    
    def subgroup = "${className}subgroup" as String
    subgroupsOut << "name\t$subgroup\n"
    subgroupsOut << "type\tsubgrouping\n"
    subgroupsOut << "label\t${className}\n"
    subgroupsOut << "parameters\t$paramStr\n"
    subgroupsOut << "subgroup1label\tlow\n"
    subgroupsOut << "subgroup1\t"    
    subgroupsOut << lowSamples.join(",")
    subgroupsOut << "\n"
    subgroupsOut << "subgroup2label\thigh\n"
    subgroupsOut << "subgroup2\t"
    subgroupsOut << highSamples.join(",")
    subgroupsOut << "\n"
    subgroupsOut << "\n"
    
    return([task,subgroup])    
  }
  
  /***
  *
  */ 
  def writeClassifier(classifierDescription,idx){
    // If we've already seen it, don't write it out...
    //if (recordedClassifiers.contains(classifierDescription)) return(null);
    //else recordedClassifiers.add(classifierDescription);
    
    def options = Utils.splitOptions(classifierDescription)
    def classifierName = options[0]
    classifierName = classifierName.replaceAll(":","")
    options[0] = ""
    def opt = options.join(" ")
    opt = opt.replaceAll("\"","") // Remove pesky quotes...    

    def classifierID = "${classifierName}${idx}".toString()
    classifiersOut << "name\t$classifierID\n"
    classifiersOut << "type\t$classifierName\n"
    classifiersOut << "label\t$classifierName\n"
    classifiersOut << "parameters\t$opt\n"
    classifiersOut<<"\n"
    return(classifierID);
  }
  
  /***
  *
  */
  def writeResults(results,classID,task,subgroup,idx){
    
    resultsOut << "name\t${task}Job${idx}\n"
    resultsOut << "type\tjob\n"
    resultsOut << "task\t${task} gi50 response\n"    
    resultsOut << "dataset\tgrayCellLine\n"    
    resultsOut << "subgrouping\t$subgroup\n"
    resultsOut << "classifier\t$classID\n"
    resultsOut << "featureSelection\tNone\n"
    resultsOut << "accuracyType\tleave one out\n"
    
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
    
    resultsOut << "samples\t$samples\n"
    resultsOut << "trainingAccuracies\t$training\n"
    resultsOut << "testingAccuracies\t$testing\n" 
    resultsOut << "\n"       
    resultsOut << "\n"           
  }
}

 