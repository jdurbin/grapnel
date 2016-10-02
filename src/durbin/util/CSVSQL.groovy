package durbin.util;

import groovy.sql.Sql
import org.h2.Driver
import durbin.util.*

/****
 This is a convenience wrapper for the H2 database engine that treats csv (or tab) files as 
 database tables and allows you to perform arbitrary select queries on those files/tables, including 
 multi-file/table joins.  The query grammer is described here: http://www.h2database.com/html/grammar.html.  
 The input files can be tab or csv and must end with one of .csv, .tsv, or .tab. 
 
 The use of this class is best seen by example:

 // specify how each file maps to a table name
 tableNameMap = ['people':'salesforcedata_2016.csv','children':'all_children_2016.csv']
 csvsql = new CSVSQL(tableNameMap)

 // Execute some queries
 query = "select people.name,children.child from people,children where people.name=children.name"
 csvsql.eachRow(query){row->
	println row.field
 }
	
*/
class CSVSQL {

	def sql
	def tableNameMap
	def sep
		
	def CSVSQL(String fileName){				
		def list = []
		list<<fileName
		tableNameMap = getMap(list)
		createFromMap(tableNameMap)
	}	
		
	def CSVSQL(java.util.ArrayList fileNames){
		tableNameMap = getMap(fileNames)
		createFromMap(tableNameMap)
	}	
		
	/**
	*	Create the in-RAM database tables from the files, giving each the listed name.
	*/ 
	def CSVSQL(Map tableNameMap){
		createFromMap(tableNameMap)
	}
	
	def createFromMap(tableNameMap){
		sql = Sql.newInstance("jdbc:h2:mem:db1","org.h2.Driver")				

		// Create tables for each csv file named, read and populate database...
		tableNameMap.each{tablename,pathname->
			if (pathname.contains(".csv")) sep = ","
			else sep = "\t"
		
			def typeString = inferColumnTypes(pathname,20,sep)
			def stmt
			if (pathname.contains(".csv")){		
				stmt = "create table $tablename($typeString) as select * from csvread('$pathname')" as String
			}else if (pathname.contains(".tab") || pathname.contains(".tsv")){
				stmt = "create table $tablename($typeString) as select * from csvread('$pathname',null,'UTF-8',chr(9))" as String	
			}else{
				throw new Exception("ERROR: File must be one of .csv, .tab, or .tsv")				
			}				
			sql.execute(stmt)	
		}
	}	
	
	def collect(String selstmt){
		def rvals = []
		sql.eachRow(selstmt){row->
		    def meta = row.getMetaData()
		    def cols = meta.getColumnCount()
		    def vals = (0..<cols).collect{row[it]}
			vals.each{v->
				rvals.add(v)	
			}
		}
		return(rvals)
	}
	
	
	def collect(String selstmt,Closure c){
		def rvals = []
		sql.eachRow(selstmt){row->
		    def meta = row.getMetaData()
		    def cols = meta.getColumnCount()
		    def vals = (0..<cols).collect{row[it]}
			vals.each{v->
				rvals.add(c.call(v))	
			}
		}
		return(rvals)
	}	
	
	def eachRow(String selstmt,Closure c){
		sql.eachRow(selstmt){row->
			c.call(row)
		}
		return(this)
	}
	
	def getMap(fileNames){
		// Just name the tables the same as the files with suffix dropped. 
		tableNameMap = [:]	
		fileNames.each{fileName->
			def tableName = fileName.replaceAll(".csv","")
			tableName = tableName.replaceAll(".tsv","")
			tableName = tableName.replaceAll(".tab","")
			tableNameMap[tableName] = fileName			
		}
		return(tableNameMap)
	}
	
	def print(headings,rows){
		if (headings != null){
			println headings.join("\t")
			rows.each{
				println it.join("\t")
			}
		}else{
			println "No rows match query."
		}
	}

	/**
	* Infer the type of each column in the file by sampling the first sampleRows
	* rows of data.  The three types supported are:  INT, DOUBLE, VARCHAR. 
	* It tries to assign types in order INT, DOUBLE, VARCHAR, assigning each 
	* column the strictest type that has uninamous vote in the sample. 
	* 
	* A negative sampleRows indicates to use whole file as sample.  This is 
	* the default, but may want to sample smaller for performance, especially 
	* if you know that first few lines are representative.  
	* 
	* Handling empty fields is a problem that needs to be thought through. 
	*/ 
	def inferColumnTypes(fileName,sampleRows = -1,separator){
		def columnType = [:]
		def numLines = FileUtils.fastCountLines(fileName)
		if (sampleRows < 0) sampleRows = numLines // Use whole file. 
		if (sampleRows > numLines) sampleRows = numLines
  
		new File(fileName).withReader{r->
			def headings = r.readLine().split(separator)
			def intcounts = new int[headings.size()]
			def doublecounts = new int[headings.size()]
        
			(0..<sampleRows-1).each{
				def line = r.readLine()
				def fields = line.split(separator,-1) // -1 to handle empty fields. 
      
				//Sanity test..
				if (fields.size() != headings.size()){
					println "headings: $headings"
					println "fields: $fields"
					throw new Exception("ERROR:  headings size ${headings.size()} != fields.size ${fields.size()}." as String)        
				}
      
				fields.eachWithIndex{f,i->
					if (f.isDouble()) doublecounts[i]++
					if (f.isInteger()) intcounts[i]++
				}
			}
    
			headings.eachWithIndex{h,i->      
				if (intcounts[i] == (sampleRows-1)) columnType[h]='INT'
				else if (doublecounts[i] == (sampleRows -1)) columnType[h] = 'DOUBLE'
				else columnType[h] = 'VARCHAR'
			}  
		}
  
		// Convert into a string...
		def pairs = []
		columnType.each{key,value-> pairs<<"$key $value"}
		def str = pairs.join(",")
		return(str)
	}  	
}