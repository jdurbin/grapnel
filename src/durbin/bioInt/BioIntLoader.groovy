/*********************************************************************
* Copyright 2007-2010 -- The Regents of the University of California 
**********************************************************************/

package durbin.bioInt

import groovy.sql.Sql
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;
import weka.core.converters.*

import durbin.paradigm.*
import durbin.weka.*

/**
* Class to create a set of instances from the bioInt database. 
*
*/
class BioIntLoader{
  def foo;
  def db;
  def err = System.err // sugar
  def featureId2NameMap;
  def featureId2IdxMap;
  def sampleID2ClassVal;
  
  def BioIntLoader(){}
  
  def BioIntLoader(confFile,port,dbName){
    db = newConnection(confFile,port,dbName) 
  }
  
  /**
  * Make a connection to the database using user and password found in confFile, 
  * (e.g. .hg.conf)
  */ 
  def newConnection(confFile,port,dbName){
        
    // Look up user and pass from .hg.conf
    def env = System.getenv()
    def home = env['HOME']
    def props = new Properties()
    props.load(new FileInputStream("$home/$confFile")); 
    def user = props['central.user']
    def pass = props['central.password']

    //def server = "jdbc:mysql://127.0.0.1:3306/bioInt"
    def server = "jdbc:mysql://127.0.0.1:$port/$dbName"
    def dbdriver = "com.mysql.jdbc.Driver"
    def rvalDB = Sql.newInstance(server,user,pass,dbdriver)
    return(rvalDB)
  }
    
  /***
  * Queries the bioInt database and builds a set of instances based on a given 
  * data table with the specified class.   
  */ 
  def getInstances(table,className){
    // Get the features from DB and setup attributes and instances objects...

    // This map will contain ALL features known, not just those in table.     
    featureId2NameMap = getID2NameMap(db,table)
    err.println "featureId2NameMap.size="+featureId2NameMap.size()

    // Look up which features are actually used in table... subset of features in name map. 
    featureId2IdxMap = getUsedID2IdxMap(db,table)
    err.println "featureId2IdxMap.size="+featureId2IdxMap.size()

    //err.println featureId2IdxMap
    //err.println "BUG(val): "+featureId2IdxMap[5]
    //err.println "BUG(val long): "+featureId2IdxMap[5 as Long]
    //err.println "BUG(string): "+featureId2IdxMap['5']

    FastVector attrs = setupAttributes(featureId2IdxMap,featureId2NameMap,className)  
    err.println "Number of attributes set up: "+attrs.size()
        
    Instances data = new Instances(table,attrs,0);

    sampleID2ClassVal = getSampleClassValues(db,className)
    err.println "${sampleID2ClassVal.size()} class values obtained from DB."

    // Stuff Instances (data) with information from table. 
    // Each row in table is just one value for a sample_id,feature_id pair. 
    // Instances are organized by sample, so we want to collect all the features
    // for one sample, stuff them into an array of vals, then when this array of 
    // vals is full, stuff them into the Instances structure. 
    long currentSampleID = -1
    def isFirstTime = true;
    def vals
    def sql = "select * from $table order by sample_id,feature_id" as String
    err.print "$sql ..."
    def tableRowCount = 0;
    def valCount = 0;
    db.eachRow(sql){row->

      // Each row contains sample_id, feature_id, val, conf
      if (row.sample_id != currentSampleID){
        if (isFirstTime) isFirstTime = false;
        else {
          // Add the class value...
          def classVal = sampleID2ClassVal[currentSampleID as Long]
          if (classVal == null) classVal = Instance.missingValue()
          vals[vals.length-1] = classVal

          // Save the vector..
          err.println "Add feature vector for sample_id: ${row.sample_id}, $valCount values, classval: $classVal"
          valCount = 0;        

          // Sanity test...
          vals.each{if (it == null) err.println "ERROR: NULL val found in vals."}

          data.add(new Instance(1.0,vals));
        }

        // Create new val array and init it to missingValue as default...
        vals = new double[data.numAttributes()]
        (0..<vals.length).each{i-> vals[i] = Instance.missingValue()}

        currentSampleID = row.sample_id as Long
      }

      tableRowCount++

      // Put this table row into the vals array...
      stuffTableRow(featureId2IdxMap,row,vals)
      valCount++
    }

    // Add last sample...
    def classVal = sampleID2ClassVal[currentSampleID]
    if (classVal == null) classVal = Instance.missingValue()
    vals[vals.length-1] = classVal
    data.add(new Instance(1.0,vals));
    err.println "done. $tableRowCount table rows read."

    (0..<data.numAttributes()).each{aidx->
      vals = data.attributeToDoubleArray(aidx)
      vals.each{if (it == null) err.println "ERROR: val is null. "}
    }


    data.setClassName(className)

    // Remove samples with missing or negative values...
    //def pipeline = new ParadigmPipeline(className)
    //data = pipeline.removeInstancesWithNegativeClassValues(data)
    //data = pipeline.removeInstancesWithMissingClassValues(data) 
    //data.deleteWithMissingClass() 
    // Split numeric class value...
    //lowerBound = 3
    //upperBound = 12
    //data = pipeline.classToNominalFromCutoffs(data,lowerBound,upperBound,"low","high")

    return(data)
  }


  /**
  * Queries the bioInt database and returns a map from attributes actually 
  * found in the given table to their attribute IDs. 
  */ 
  def getUsedID2IdxMap(db,table){
    def id2IdxMap = new HashMap<Long,Integer>()
    def sql = "select distinct(feature_id) from $table order by feature_id"
    def features = query(db,sql)
    features.eachWithIndex{fid,idx-> id2IdxMap[fid] = idx}  
    return(id2IdxMap)
  }


  /*****************************************
  *  Creates a featureID to name map and an Id to Idx map.   Name will encode 
  *  the feature ID also for easy later reference.  Currently, data tables get 
  *  their features from analysisFeatures table, and analysis results get their 
  *  features ßfrom entities_NCIPID, so there are different queries depending on the 
  *  source.   This will probably change in the future. 
  * 
  *  Note:  These maps include all features, not just all features represented 
  *  in the data table. 
  */ 
  def getID2NameMap(db,table){

    def id2NameMap = new HashMap<Long,String>()
    def sql
    if (isTableInDatasets(db,table)){
      err.println "getting features from analysisFeatures..."
      sql = "select id,feature_name from analysisFeatures order by id" as String  
      db.eachRow(sql){
        // as Long is enforced because in some tables the id is an INT, in others
        // an UNSIGNED INT, and these get returned as int and long respectively. 
        // So, up cast everything to long.. 
        id2NameMap[it.id as Long] = "${it.feature_name}(${it.id})"
      }
    }else{
      err.println "getting features from entities_ncipid..."
      sql = """
      select entity_id,pathway_id,entity_name
      from entities_ncipid order by entity_id""" as String              
      db.eachRow(sql){
        id2NameMap[it.entity_id as Long] = "${it.entity_name}(eid:${it.entity_id},pid:${it.pathway_id})"
      }
    }
    return(id2NameMap)
  }

  /** 
  *  Check to see if the given tablename is in datasets.data_table
  */ 
  def isTableInDatasets(db,table){
    
    err.println "isTableInDatasets: $table"
    
    def sql = "select data_table from datasets where data_table='$table'" as String
    def rows = db.rows(sql)
    if (rows == null) err.println "rows is null??"        
    if (rows.size() > 0) return(true)
    else return(false)
  }


  /**
  * Based on the feature index and name maps, create attributes for the Instances.  
  */ 
  def setupAttributes(fId2IdxMap,fId2NameMap,className){
    def attrs = new FastVector()

    /* 
    *  I admit this is a bit of a round about way to do this.  I amd 
    *  doing this defensively, for I am afraid of being bitten if I 
    *  assume that any of the lists of features I create are in the same
    *  order.  They should be, but by doing it this way I make sure 
    *  that even if they aren't, nothing bad happens.  */
    fId2IdxMap.each{id,idx->
      def name = fId2NameMap[id as Long]

      if ((name == null) || (name == "")){
        err.println "NULL or empty name found for feature $id, $idx"
        err.println "This could result from using a non-standard feature table (i.e. not analysisFeatures or entities_NCIPID)"
        System.exit(1);
      }    
      attrs.addElement(new Attribute(name as String))
    }

    // Finally, add an attribute for the class...
    attrs.addElement(new Attribute(className))
    return(attrs)  
  }


  /**
  * Takes a single table row, which is just one attribute value for one sample, 
  * and stuffs it into an attribute list. 
  */ 
  def stuffTableRow(featureId2IdxMap,row,vals){
    def id = row.feature_id
    def valIdx = featureId2IdxMap[row.feature_id] 

    if (valIdx == null){
      err.println "ERROR:  featureId2IdxMap[${row.feature_id}] == null"
      err.println row
      err.println featureId2IdxMap[row.feature_id as int]
    }


    if (row.val == 0) vals[valIdx] = Instance.missingValue()
    else if (row.val == null) vals[valIdx] = Instance.missingValue()
    else vals[valIdx] = row.val
  }

  /**
  * Query DB and create a map from sampleID to to the value of the given class.  
  */ 
  def getSampleClassValues(db,className){

    // Look up the ID for the named feature...
    //featureID = getFeatureID()
    def sql = "select id from features where name='$className'" as String
    def ids = query(db,sql)
    if (ids.size() > 1) err.println "Hey, ${ids.size()} ids found for $className !"
    if (ids.size() == 0) err.println "Hey, $className not found in $featureIDTable !"  
    def featureID = ids[0]

    err.println "Getting data for feature $className fid: $featureID"

    // Get values for that feature...
    sql = """
    select * from clinicalData 
    where feature_id = $featureID order by sample_id""" as String

    def sample2ClassVal = [:]
    db.eachRow(sql){
      sample2ClassVal[it.sample_id as Long] = it.val
    }
    return(sample2ClassVal)
  }

  /**
  * Perform the sql query and return the results as a list. 
  */ 
  def query(db,sql){
    def rlist = []
    db.eachRow(sql as String){
      if ((it[0] == null) || (it[0] == 'null')) return(null)
      rlist << it[0]
    }
    return(rlist)
  }

}