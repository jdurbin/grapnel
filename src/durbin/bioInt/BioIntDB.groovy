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
* Class to interact with BioInt database.  
* 
* For class that creates weka Instances directly, see BioIntLoader
*
*/
class BioIntDB{
	
	def db;
  def err = System.err // sugar


	/***
	* Returns the clinical data for a given dataset as a 2D map of clinical features x samples
	*/ 
	def getClinicalAsTwoDMap(datasetName){
		def sql = """                                                                                                             
		select s.name,f.name,c.val                                                                                            
		from samples as s, features as f, clinicalData as c, ${datasetName} as d                                                  
		where c.sample_id = d.sample_id and                                                                                   
	      	s.id = d.sample_id and                                                                                          
	      	c.feature_id = f.id                                                                                             
					""" as String

		def table = new TwoDMap()
		this.db.eachRow(sql){
	     	def val = it.val
	     	def sname = it[0]
	     	def fname = it[1]
	     	table[fname][sname] = val
		}
		return(table)
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