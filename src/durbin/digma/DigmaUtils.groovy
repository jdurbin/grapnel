
import durbin.util.*
import groovy.lang.Closure;

/************************************************************
* Some utilities to help process inputs and outputs from/to
* Digma (a bayesian factor graph for integrating genomic data
* into biological pathway models).
* 
*/
class DigmaUtils{

  /**************************************
  * Sometimes I get clinical features as a flat list.  Format:
  *
  * name  sample_id value
  * 
  * return: map from sample ID's to clinical value. 
  */ 
  static Map readClinicalFromList(clinicalFile,featureName){  
    // Read in the clinical feature we're interested in...
    //$clinicalName.... 
    System.err.print "Reading attribute $featureName from file $clinicalFile..."
    def clinicalVal = [:]
    new File(clinicalFile).withReader{r->
      // name    sample_id       val
      def header = r.readLine();
      r.splitEachLine(/\s/){fields->
        if (fields[0] == featureName){
          //id = "sample_"+fields[1]
          def id = fields[1]
          clinicalVal[id] = fields[2] as Double
        }
      }  
    }
    System.err.println "done."
    return(clinicalVal)
  }  
  
  /*****************************************
  * Sometimes I get clinical features as a table.
  * 
  * rows are features, columns are patient IDs
  * 
  */
  static Map readClinicalFromTable(clinicalFile,featureName){
    def clinicalVal = [:]
    def clinicalTable = new Table(clinicalFile);
    def attributeRow = clinicalTable.getRowIdx(featureName)
    if (attributeRow == null){
      System.err.println "ERROR, no attribute named $featureName in table. "
      System.err.println clinicalTable.rowNames()
    }
        
    (0..<clinicalTable.numCols).each{col->
      def val = clinicalTable[attributeRow][col]
      def id = clinicalTable.colNames[col]
      
      // Check for missing clinical values... just skip those, what else can you do?
      if ((val != "") && (val != null)){
        clinicalVal[id] = val
      }
    }  
    return(clinicalVal)        
  }
  
  /*****************************************
   * Sometimes I get clinical features as a table.
   * 
   * rows are features, columns are patient IDs
   * 
   */
  static Map readClinicalFromTable(clinicalFile,featureName,Closure c){
    def clinicalVal = [:]
    def clinicalTable = new Table(clinicalFile);
    def attributeRow = clinicalTable.getRowIdx(featureName)
    if (attributeRow == null){
      System.err.println "ERROR, no attribute named $featureName in table. "
      System.err.println clinicalTable.rowNames()
    }
        
    (0..<clinicalTable.numCols).each{col->
      def val = clinicalTable[attributeRow][col]
      val = c.call(val)      
      def id = clinicalTable.colNames[col]
      
      // Check for missing clinical values... just skip those, what else can you do?
      if ((val != "") && (val != null)){
        clinicalVal[id] = val
      }
    }  
    return(clinicalVal)        
  }
  

  /*********************************************************
  *  Function to take digma outputs and create a set of weka Instances from them. 
  * 
  */
  
  /*
  static Instances digma2Instances(dataFile,clinicalFile,featureName,featureCutoffs){
    // Need to add an option for the which selector...
    def featureCutoff
    def lowCutoff
    def highCutoff

    isQuartile = false
    err.println "featureCutoffs="+featureCutoffs
    if (featureCutoffs.contains(',')){
      isQuartile = true
      cutoffs = featureCutoffs.split(",")
      lowCutoff = cutoffs[0] as Double
      highCutoff = cutoffs[1] as Double
    }else{
      featureCutoff = featureCutoffs as Double
    }


    // Read the expression file...
    TableFileLoader loader = new TableFileLoader()
    loader.setAddInstanceNamesAsFeatures(true)

    Instances data = loader.read(dataFile,"\t"){
    //  it.split(",")[1] as Double
      it as Double
    }


    clinicalVal = DigmaUtils.readClinicalFromTable(clinicalFile,featureName){
        if ((it == "") || (it == null)){
          return (Instance.missingValue())
        }else{
          it as Double
          //return (it.split(",")[0] as Double)
        }
    }


    // Record the instances we want to remove because they have 
    // negative or missing values. 
    // KJD TODO:  Add option, since we don't *always* want to remove negative values.
    int numNeg = 0
    instancesWithBadValues = [] as Set
    clinicalVal.each{key,value->
      if ((value < 0) || (value == Instance.missingValue())) {
        numNeg++
        instancesWithBadValues.add(key)
      }  
    }
    err.println "numNeg: "+numNeg

    // ADD the new clinical attribute...
    err.print "Add new clinical attribute $featureName..."
    newData = new Instances(data);
    FastVector values = new FastVector();
    values.addElement("low");
    values.addElement("high");
    //values.addElement("middle");
    newData.insertAttributeAt(new Attribute("$featureName", values), newData.numAttributes());

    Attribute id = newData.attribute("ID");
    attIdx = newData.numAttributes()-1

    (0..<newData.numInstances()).each{i->
      def valIdx
      Instance instance = newData.instance(i);
      sampleID = instance.stringValue(id);

      // Missing value
      if (clinicalVal[sampleID] == ""){
        newData.instance(i).setValue(attIdx,Instance.missingValue())
      }else{
        thisvalue = clinicalVal[sampleID] as Integer
        //err.println "$sampleID: $thisvalue ? $featureCutoff"

        if (isQuartile){
          if (thisvalue >= highCutoff) valIdx = 1      // 1 is high, 0 is low
          else if (thisvalue < lowCutoff) valIdx = 0
          else {
            valIdx = 2    
            att = newData.attribute("ID")
            name = newData.instance(i).stringValue(att)

            // Want to remove the middle values too, while we're at it. 
            instancesWithBadValues.add(name)
          }
        }else{ // median
          if (thisvalue >= featureCutoff) valIdx = 1
          else valIdx = 0
        }
        newData.instance(i).setValue(attIdx,valIdx);  
      }
    }
    err.println "done."

    // Remove any instances that are not in both datasets...
    clinicalInstanceNames = clinicalVal.keySet() as Set
    dataInstanceNames = FileUtils.firstLine(dataFile).split(/\s/)[1..-1]
    //dataFeatures = loader.instanceNames
    err.println "dataInstanceNames.size="+dataInstanceNames.size()

    intersection = dataInstanceNames.intersect(clinicalInstanceNames)
    union = dataInstanceNames.plus(clinicalInstanceNames)
    union.unique()
    diff = union - intersection
    err.println "union: "+union.size()+" intersection: "+intersection.size()+" diff: "+diff.size()
    newData = InstanceUtils.removeNamedInstances(newData,diff);

    err.println "newData.numInstances="+newData.numInstances()

    // Remove any instances that have values marked as 'bad' for some reason...
    err.print "Removing instances with bad (NaN or -1) values..."
    newData = iu.removeNamedInstances(newData,instancesWithBadValues)
    err.println "done.  ${newData.numInstances()} instances remaining. "

    return(newData)    
  }
  */
  

}