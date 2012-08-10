package durbin.util;


/***********************************
* Support for accessing a table one row at a time, accessing
* fields by name.  
* 
*  new OnlineTable(fileName).eachRow{row->
*		println row.name
*   println row.address
* }
* 
*/
class OnlineTable{			
	
			String fileName
			String sep = null;
		
			def OnlineTable(String f,String delimiter){
				fileName = f
				sep = delimiter
			}
		
			def OnlineTable(String f){
				fileName = f
			}			
			
			def eachRow(Closure c){
				new File(fileName).withReader{r->
					def headingStr = r.readLine()
					if (!sep) sep = determineSeparator(headingStr)
					def headings = headingStr.split(sep,-1)
					r.eachLine{rowStr->
						def rfields = rowStr.split(sep,-1)
						def row = [:]
						rfields.eachWithIndex{f,i->row[headings[i]]=f}
						c(row)
					}
				}
			}	
						
			def determineSeparator(line){
				if (line.contains(",")) sep = ","
				else if (line.contains("\t")) sep = "\t"
				else {
					System.err.println "File does not appear to be a csv or tab file.";
					throw new RuntimeException();
				}
				return(sep);
			}		
}