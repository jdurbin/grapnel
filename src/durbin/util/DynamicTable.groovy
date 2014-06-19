package durbin.util

import com.google.common.collect.*
import groovy.transform.ToString

/**
* A dynamic, potentially sparse, table of objects. <br><br>
* 
* DynamicTable implements all the methods from google HashBasedTable via 
* Groovy Meta Object Programming and delegation. 
* 
* DynamicTable adds functionality to read and write tables, access
* sugar, etc. 
* 
*/ 
@ToString
class DynamicTable {
	static def err = System.err
	
	// For the HashBasedTable. Using delegation instead of inheritance.
	HashBasedTable delegate; 
	def defaultVal = "null"
	
	def DynamicTable(){
		delegate = HashBasedTable.create()
	}
	
	def DynamicTable(fileName,delimiter){
		delegate = HashBasedTable.create()
		read(fileName,delimiter)
	}
	
	def numRows(){return(delegate.rowKeySet().size())}
	def numCols(){return(delegate.columnKeySet().size())}
	
	// Groovy MOP magic! 
	// intercepts method access and passes them to delegate
	// 
	def invokeMethod(String name,args){
		delegate.invokeMethod(name,args)
	}	
	
	// Allow [] notation
	def putAt(String rowKey,value){
		this[rowKey] = value
	}
	
	// Allow [] notation. 
	def getAt(String rowKey){
		return(delegate.row(rowKey))
	}		
	
	
	def eachRow(Closure c){
		delegate.rowMap().each{rowKey,columnMap->
			c(columnMap.values())
		}
	}
	
	def eachRowKeyWithValues(Closure c){
		delegate.rowMap().each{rowKey,columnMap->
			c(rowKey,columnMap.values())
		}
	}
	
	def eachRowKeyWithColumnMap(Closure c){
			delegate.rowMap().each{rowKey,columnMap->
				c(rowKey,columnMap)
			}
	}
	
	
	/***
	* Print the table to stdout
	*/ 
	def print(){
		def colKeys = delegate.columnKeySet()
		def rowKeys = delegate.rowKeySet()
	 	print "Features\t"
		println colKeys.join("\t")				
		rowKeys.each{r->		
			print "${r}\t"
			def rowvals = colKeys.collect{c->delegate.get(r,c) ?: defaultVal}
			println rowvals.join("\t")
		}
	}		
	
	def write(fileName,delimiter){
		
		new File(fileName).withWriter{w->
		
			def colKeys = delegate.columnKeySet()
			def rowKeys = delegate.rowKeySet()
			w.write "Features$delimiter"
			w.writeLine colKeys.join(delimiter)		

			rowKeys.each{r->		
				w.write "${r}$delimiter"
				def rowvals = colKeys.collect{c->delegate.get(r,c) ?: defaultVal}
				w.writeLine rowvals.join(delimiter)
			}
		}
	}


	/***
	* Reads in a table of values as a GTable
	*/ 
	def read(fileName,delimiter){
		new File(fileName).withReader{r->
			def headings = r.readLine().split(delimiter)
			headings = headings[1..-1] // omit Feature label...

			r.splitEachLine(delimiter){fields->
				def rowName = fields[0]
				headings.eachWithIndex{h,i->
					delegate.put(rowName,h,fields[i+1])
				}
			}		
		}
		return(this)
	}

}