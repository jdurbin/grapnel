package durbin.bioInt

import groovy.sql.Sql
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;
import weka.core.converters.*

import durbin.util.TwoDMap
import durbin.paradigm.*
import durbin.weka.*

/**
* Class to interact with BioInt database.  
* 
* For a class that creates weka Instances directly from DB, see BioIntLoader<br>
* 
* I will try to localize all database queries in one of the classes under durbin.bioint 
* to help a tiny bit with maintainability.  
*
*/
class BioIntDB{
	
	def db;
  def err = System.err // sugar

	def BioIntDB(confFile,port,dbName){
		db = newConnection(confFile,port,dbName);
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
	* Returns the clinical data for all datasets as a 2D map of clinical features x samples
	*/ 
	def getAllFeaturesVsSamplesTwoDMap(){
		
		// 11.7 seconds to read 511529 rows... all samples vs all clinical features
		def sql = """
		select s.name,f.name,c.val 
		from samples as s, features as f, clinicalData as c
		where s.id =c.sample_id and c.feature_id = f.id;
		""" as String

		def table = new TwoDMap()
		err.println "Reading clinical data..."
		def count = 0

		db.eachRow(sql){
	     def val = it.val
	     def sname = it[0]
	     def fname = it[1]
	     table[fname][sname] = val
		}
		return(table);
	}

	/****
	* Returns the genomic data for a given dataset as a 2D map of genes x samples
	*/ 
	def getGenesVsSamplesTwoDMap(dbTable){
		def sql = """                                                                                                             
		select s.name,f.feature_name,d.val                                                                                    
		from samples as s,${dbTable} as d,analysisFeatures as f                                                               
		where  d.sample_id = s.id and f.id = d.feature_id                                                                     
		""" as String

		def table = new TwoDMap()
		db.eachRow(sql){
		    def sname = it[0]
		    def fname = it[1]
		    table[fname][sname] = it[2]
		}
		return(table);
	}

	/***
	* Get wekaMineResults for a particular dataset...
	*/ 
	def getWekaMineResults(int datasetId){
		def sql = """
			select * from wekaMineResults where dataset_id = $datasetId
		""" as String
		def rows = db.rows(sql);		
		return(rows);
	}

	/***
	* Returns the clinical data just for a given dataset as a 2D map of 
	* clinical features x samples.  2.6 seconds for 34x118 data.
	*/ 
	def getFeaturesVsSamplesTwoDMap(int datasetId){
		// Now restrict the query to just the samples in the given dataset...
		// 2.68 seconds... 34 features, 118 samples!!! yea!
		def sql = """
		select s.name,f.name,c.val 
		from samples as s, features as f, clinicalData as c
		where s.id =c.sample_id and c.feature_id = f.id and s.dataset_id = ${datasetId};
		""" as String

		def table = new TwoDMap()
		err.println "Reading clinical data..."
		def count = 0

		db.eachRow(sql){
	     def val = it.val
	     def sname = it[0]
	     def fname = it[1]
	     table[fname][sname] = val
		}
		return(table);
	}	
	
	/**
	* Get features vs samples by dataset NAME.
	*/ 
	def getFeaturesVsSamplesTwoDMap(String datasetName){
		getFeaturesVsSamplesTwoDMap(getDatasetID(datasetName) as int)
	}
	

	/**
	* Look up the dataset id for the named dataset. 
	*/ 
	def getDatasetID(datasetName){
		def sql = "select id from datasets where datasets.data_table  = '${datasetName}'" as String
		def id
		db.eachRow(sql){
			id = it[0] 
		}
		return(id as int)
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

	/***
	* Returns the datasets data as a TwoDMap datasets by 
	* dataset attributes (name, type, id, etc.)
	*/ 
	def getDataSetInfoTwoDMap(){
		def sql = """
		select d.name,d.data_table,d.num_samples,t.name,dt.name
		from datasets as d, dataTypes as dt, tissues as t		
		where d.type_id = dt.id and d.tissue_id = t.id
		""" as String

		def table = new TwoDMap()
		err.println "Reading clinical data..."
		def count = 0

		db.eachRow(sql){
			def dataname = it[0]
			def datatable = it[1]
			def numsamples = it[2]
			def tissuetype = it[3]
			def datatype = it[4]
						 
			table['Name'][datatable] = dataname						
	    table['Tissue'][datatable] = tissuetype
		  table['DataType'][datatable] = datatype
			table['NumSamples'][datatable] = numsamples
		}
		return(table);
	}


	/****
	* Get a list of the data tables in bioInt... these are essentially the raw datasets. 
	*/ 
	def getDataTables(){
		def sql = "select data_table from datasets"
		def dataTables = query(db,sql)
		return(dataTables)
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

/**
* Returns the data table as a 2D Map of features(genes) x samples
* 
* Why the fuck doesn't this work?  Cut and paste same damn code right into 
* the program, works great, call this method, doesn't do anything, never 
* returns from query...
*/
/* 
def getDataAsTwoDMap(datasetName){

	println "HI THERE!"

	// Here's some code that works, does the join, then stuffs things into a TwoDMap
	// Takes only 33sec and returns 96 samples x 11702 features.  
	def sql = """
	select s.name,f.feature_name,d.val
	from samples as s,${datasetName} as d,analysisFeatures as f
	where  d.sample_id = s.id and f.id = d.feature_id
	""" as String

	println "sql:\n$sql"

	def table = new TwoDMap()
	this.db.eachRow(sql){
	    def sname = it[0]
	    def fname = it[1]
			def val = it[2]
			println "$sname $fname $val"
	    table[fname][sname] = val
	}
	return(table)

	// A decent way to write table out to a file
	//table.writeTable("lin2.txt","\t","NA")
}
*/