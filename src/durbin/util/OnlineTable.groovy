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

	//File file = null
	//InputStream inStream;
	Reader r
	String sep = null;
	
	def headings
	def row

	def OnlineTable(InputStream s,String delimiter){
		sep = delimiter
		r = s.newReader()
		headings = getHeader()
	}
		
	def OnlineTable(String fileName,String delimiter){
		sep = delimiter	
		file = new File(fileName)
		inStream = file.newInputStream()
		r = inStream.newReader()
		headings = getHeader()
	}	
	
	// Get the headers 
	def getHeader(){
		//r = instream.newReader()
		def headingStr = r.readLine()
		def h = headingStr.split(sep,-1)
		return(h)		
	}
		
	// Get the headers 
	/*
	static def getHeader(String f){
		def tempsep = FileUtils.determineSeparator(f)
		r = new File(f).newReader()
		def headingStr = r.readLine()
		def h = headingStr.split(tempsep,-1)
		return(h)
	}
	*/
	
	// Sometimes a stream input won't have a heading, so give one
	// explicitly..
	def OnlineTable(InputStream s,ArrayList h,String delimiter){		
		r = s.newReader()
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
		r = new File(f).newReader()
		headings = getHeader()
		//headings = getHeader(inStream)
		//System.err.println "headings="+headings		 this is there ok.
	}
		
	/**
	* Assumes inStream has already advanced past the header. 
	* Stream will be closed after this closure. 
	*/ 			
	def eachRow(Closure c){									
		r.eachLine{rowStr->
			def rfields = rowStr.split(sep,-1)
			row = [:]
			rfields.eachWithIndex{f,i->
				row[headings[i]]=f
			}
			c(row)
		}
	}	
	
	/**
	* Collect items from a stream...
	* Stream will be closed after this closure. 
	*/
	def collect(Closure c){
		def list = []
		def temp = r.readLine() // consume header.
		r.eachLine{rowStr->
			def rfields = rowStr.split(sep,-1)
			row = [:]
			rfields.eachWithIndex{f,i->
				row[headings[i]]=f
			}
			list << c(row)
		}		
		return(list)
	}
		
	def headings(){
		return(headings)
	}	
}