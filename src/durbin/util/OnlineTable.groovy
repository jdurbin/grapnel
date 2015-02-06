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
* 
*/
class OnlineTable{			

	File file = null
	InputStream inStream;
	String sep = null;
	
	def headings
	def row

	def OnlineTable(InputStream s,String delimiter){
		sep = delimiter
		headings = getHeader(s)
	}
		
	def OnlineTable(String f,String delimiter){
		fileName = f
		sep = delimiter	
		file = new File(fileName)	
		inStream = file.newInputStream()
		headings = getHeader(inStream)
	}	
	
	// Get the headers 
	def getHeader(InputStream instream){
		def r = instream.newReader()
		def headingStr = r.readLine()
		def h = headingStr.split(sep,-1)
		return(h)		
	}
	
	
	// Get the headers 
	static def getHeader(String f){
		def tempsep = FileUtils.determineSeparator(f)
		def r = new File(f).newReader()
		def headingStr = r.readLine()
		def h = headingStr.split(tempsep,-1)
		return(h)
	}
	
	// Sometimes a stream input won't have a heading, so give one
	// explicitly..
	def OnlineTable(InputStream s,ArrayList h,String delimiter){		
		inStream = s
		headings = h
		sep = delimiter
	}
	
	
	// No separator given, try to figure it out...
	def OnlineTable(String f){		
		sep = FileUtils.determineSeparator(f)
		
		// If separator is a comma, use regex that allows commas inside 
		// of quotes to not split into fields. 
		if (sep == ","){
			sep = /,(?=([^\"]|\"[^\"]*\")*$)/
		}		
		file = new File(f) 
		inStream = file.newInputStream()
		headings = getHeader(inStream)
	}
		
	/**
	* Assumes inStream has already advanced past the header. 
	* Stream will be closed after this closure. 
	*/ 			
	def eachRow(Closure c){
		inStream.withReader{r->
		//new File(fileName).withReader{r->
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
	
	/**
	* Collect items from a stream...
	* Stream will be closed after this closure. 
	*/
	def collect(Closure c){
		def list = []
		inStream.withReader{r->
		//new File(fileName).withReader{r->
			def temp = r.readLine() // consume header.
			r.eachLine{rowStr->
				def rfields = rowStr.split(sep,-1)
				row = [:]
				rfields.eachWithIndex{f,i->
					row[headings[i]]=f
				}
				list << c(row)
			}
		}
		return(list)
	}
	
	
	def headings(){
		return(headings)
	}	
}