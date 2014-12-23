package durbin.util;

/**
* Support for accessing a table one row at a time, accessing
* fields by name.  <br>
* 
*  <pre>
*	new OnlineTable(fileName).eachRow{row->
*		println row.name
*		println row.address
*	}
* </pre>
* 
*/
class OnlineTable{			

	String fileName
	String sep = null;
	
	def headings
	def row

	def OnlineTable(String f,String delimiter){
		fileName = f
		sep = delimiter		
		getHeader(f)
	}
	
	// Get the headers 
	def getHeader(String fileName){
		new File(fileName).withReader{r->
			def headingStr = r.readLine()
			headings = headingStr.split(sep,-1)
		}
	}
	
	// No separator given, try to figure it out...
	def OnlineTable(String f){
		sep = FileUtils.determineSeparator(f)
		
		// If separator is a comma, use regex that allows commas inside 
		// of quotes to not split into fields. 
		if (sep == ","){
			sep = /,(?=([^\"]|\"[^\"]*\")*$)/
		}
						
		fileName = f
		getHeader(f)
	}			

	def eachRow(Closure c){
		new File(fileName).withReader{r->
			def temp = r.readLine() // consume header.
			r.eachLine{rowStr->
				def rfields = rowStr.split(sep,-1)
				row = [:]
				rfields.eachWithIndex{f,i->
					row[headings[i]]=f
				}
				c(row)
			}
		}
	}	
	
	def headings(){
		return(headings)
	}	
}