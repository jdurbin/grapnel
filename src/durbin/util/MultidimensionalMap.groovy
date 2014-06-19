#!/usr/bin/env groovy 
package durbin.util;

//!!!! DEPRECATED !!!!
// Use GTable instead. 
// 


// Coding WARNING:
// 
// In these classes, an assignment inside a method to a variable that is not defined 
// with def or a type will be interpreted by Groovy as putting that pair in the map.  
// Thus:
//
// def myfunction(){
//  a = 5
// }
// 
// will not throw the compile-time error that a is not defined for the class
// but will blithely interpret this as putting this[a] = 5 into the map.  
// This strikes me as a bug in Groovy and it generates pernicious errors during
// runtime. 


/**
* A multi-dimensional map.  Commandeered from this post: <br>
* 
* http://groovy.329449.n5.nabble.com/Multidimensional-maps-td362232.html <br><br>
* 
* Through Groovy magic you can access elements of map with 
* dot notation or with [] notation.  
* <pre>
* m = new MultiDimensionalMap()
* m.a = 1
* m.b.c = 2
* m.d.e.f.g = "I am here..."
* m[1] = 5
* m[new Object()] = 6
* m[new Object()].aaa = 7
* m[null].bbb = 8
*
* println m.d.e.f.g
* println m["b"]["c"]
* println m["d"]["e"]["f"]["g"]
* 
* 
* m.numCells.blank[10] = 0
* m.numCells.blank[5] = 0
* m.numCells.blank[2] = 1.2
* m.numCells.blank[1] = 5
* </pre>
* 
* <b>WARNING:</b>  The map bracket syntax <b>**CREATES**</b> an entry if one doesn't 
* exist.  Care must be taken when checking for existence of a value for 
* a particular cell that you do not inadvertently create a new entry. 
* 
* KJD: This is so mind bogglingly useful that I really should translate it into Java so that
* it can be faster too.
* 
* 

*/
class MultidimensionalMap extends LinkedHashMap {
    @Override
    public Object get(Object key) {
        if (!containsKey(key)) {
            put(key, new MultidimensionalMap())
        }
        return super.get(key)
    }    
}


/***
*  Special case of MultidimensionalMap for the common 2D case...
* 
*  Differs from Table mainly in that it's completely dynamic, whereas 
*  Table requires explicit dimensions up front. 
* 
*  !! DEPRECATED !! in favor of DynamicTable . DynamicTable is based on 
*  google Guava library and is more efficient and has fewer issues than 
*  TwoDMap. 
* 
*/
class TwoDMap extends MultidimensionalMap{
    
	  def TwoDMap(){}
		def TwoDMap(String fileName,String delimiter){ read(fileName,delimiter,true)}

		def rowKeySet(){
			return(this.keySet())
		}
		
		def colKeySet(){			
			// Find the union of all column keys...
      def colKeys = [] as Set
			def rowKeys = rowKeySet()
			
      rowKeys.each{rowKey->				
				def rowSet = this[rowKey]?.keySet()
        colKeys = colKeys + this[rowKey]?.keySet()
      }
			return(colKeys)
		}

		def rows(){
			return(rowKeySet().size())
		}
		
		def cols(){			
			return(colKeySet().size())
		}
		
		def getCol(colKey){
			def rowKeys = rowKeySet()
			def colVals = []
			def nullVal = 'null'
      rowKeys.each{rowKey->
        def val = this[rowKey][colKey]
        if (val == [:]) colVals << nullVal
        else colVals << val		
			}
			return(colVals)
		}
		
		def getRow(rowKey){
			def rval = []
			def nullVal = 'null'			
			colKeySet().each{colKey->
				def val = this[rowKey][colKey]
				if (val == [:]) rval << nullVal
				else rval << val
			}
			return(rval)
		}
		

		// Increment item, initializing to zero if not already set. 
		public void count(key1,key2){
			if (this.contains(key1,key2)){
				def val = this[key1][key2]
				val = val+1
				this[key1][key2] = val
			}else{
				this[key1][key2] = 1;
			}						
		}

    /**
    * Test to see if 2D map (the most common case) already contais
    * the given keys.  Need to generalize to n keys...
    */ 
    public boolean contains(key1,key2){      
      if (!keySet().contains(key1)) return(false);
      else{
        def secondMap = this[key1]
				def secondKeySet = secondMap.keySet()
        if (!secondKeySet.contains(key2)) return(false);
        else return(true);
      }  
			return(false);
    }

		def getIfExists(key1,key2){
			if (!keySet().contains(key1)) return(null);
			else{
				def secondMap = this[key1]
				def secondKeySet = secondMap.keySet()
				if (!secondKeySet.contains(key2)) return(null);
	      else return(secondMap[key2])
			}  
			return(null)
		}
    
		/**
    * Print out a 2D table that has max keys in each direction...
    */ 
    def printTableTranspose(delimiter,nullVal){

      def rowKeys = rowKeySet()
			def colKeys = colKeySet()		

      // Print the heading...
      print "Features$delimiter"
      println rowKeys.join(delimiter)
			
      // Print the table proper...
      colKeys.each{colKey->
        def colVals = []
        rowKeys.each{rowKey->
          def val = this[rowKey][colKey]
          if (val == [:]) colVals << nullVal
          else colVals << val
        }
        print  "$colKey$delimiter"
        println colVals.join(delimiter)        
      }
    }

		/***
		* Reads in a table of values as a TwoDMap
		*/ 
		def read(datafile,delimiter,isRowLabel){
			if (isRowLabel){
				read(datafile,delimiter)
			}else{
				readNoRowLabel(datafile,delimiter)
			}
		}
			
		def readNoRowLabel(datafile,delimiter){
			TwoDMap map = this;
			new File(datafile).withReader{r->
				def headings = r.readLine().split(delimiter)
				headings = headings[1..-1] // omit Feature label...

				def rowIdx = 0;
				r.splitEachLine(delimiter){fields->
					//def rowName = fields[0]
					rowName = "Row$rowIdx"
					headings.eachWithIndex{h,i->
						map[rowName][h] = fields[i+1]		
					}
				}		
			}
			return(map)
		}


		/***
		* Reads in a table of values as a TwoDMap
		*/ 
		def read(datafile,delimiter){
			TwoDMap map = this;
			new File(datafile).withReader{r->
				def headings = r.readLine().split(delimiter)
				headings = headings[1..-1] // omit Feature label...

				r.splitEachLine(delimiter){fields->
					def rowName = fields[0]
					headings.eachWithIndex{h,i->
						map[rowName][h] = fields[i+1]		
					}
				}		
			}
			return(map)
		}

    
    /**
    * Print out a 2D table that has max keys in each direction...
    */ 
    def printTable(delimiter,nullVal){

      def rowKeys = rowKeySet()

      // Find the union of all column keys...
//      def colKeys = [] as Set
 //     rowKeys.each{rowKey->
  //      colKeys = colKeys + this[rowKey].keySet()
   //   }

			def colKeys = colKeySet()

      // Print the heading...
      print "Features$delimiter"
      println colKeys.join(delimiter)

      // Print the table proper...

      rowKeys.each{rowKey->
        def rowVals = []
        colKeys.each{colKey->
          def val = this[rowKey][colKey]
          if (val == [:]) rowVals << nullVal
					else if (val == "null") rowVals << nullVal
					else if (val == "NULL") rowVals << nullVal
          else rowVals << val
        }
        print  "$rowKey$delimiter"
        println rowVals.join(delimiter)        
      }
    }
    

		def writeTable(File out){
			writeTable(out,"\t","NA"){}
		}

		def writeTable(File out,Closure c){
			writeTable(out,"\t","NA",c)
		}
		

		def writeTable(String fileName){
			writeTable(fileName,"\t","NA"){}
		}
		
		def writeTable(String fileName,String delimiter,String nullVal){
			def out = new File(fileName)
			writeTable(out,delimiter,nullVal){it}
		}

		def writeTableQuick(String fileName,String delimiter,String nullVal){
			def out = new File(fileName)
			writeTableQuick(out,delimiter,nullVal)
		}
		
		def writeTableQuick2(String fileName,String delimiter,String nullVal){
			def out = new File(fileName)
			writeTableQuick2(out,delimiter,nullVal)
		}
		

		  /**
	    * Print out a 2D table that has max keys in each direction...
	    */ 
	    def writeTable(File out,delimiter,nullVal,Closure c){

	      def rowKeys = rowKeySet()
				def colKeys = colKeySet()

				out.withWriter(){w->
	      	// Print the heading...
	      	w.write "Features$delimiter"
	      	w.writeLine colKeys.join(delimiter)

	      	// Print the table proper...

	      	rowKeys.each{rowKey->
	        	def rowVals = []
	        	colKeys.each{colKey->
	          	def val = this[rowKey][colKey] // 'doh... this will create an entry, filling out a sparse matrix
	          	if (val == [:]) rowVals << nullVal
							else if (val == "null") rowVals << nullVal
							else if (val == "NULL") rowVals << nullVal
	          	else rowVals << val
	        	}
	        	w.write  "$rowKey$delimiter"
	        	w.writeLine rowVals.join(delimiter)        
	      	}
	    	}
			}
		 
			def writeTableQuick(File out,delimiter,nullVal){
				def rowKeys = rowKeySet()
				def colKeys = colKeySet()

				err.println "rowKeys.size="+rowKeys.size()
				err.println "colKeys.size="+colKeys.size()

				out.withWriter(){w->
					// Print the heading...
					w.write "Features$delimiter"
					w.writeLine colKeys.join(delimiter)
					
					int rowCount = 0;
					rowKeys.each{rowKey->
						
						if ((rowCount++ % 500)==0) err.println "$rowCount rows written"
						
						def rowVals = []
						colKeys.each{colKey->
							if (this.contains(rowKey,colKey)){
								rowVals << val
							}else{
								rowVals << nullVal
							}
						}
						w.write  "$rowKey$delimiter"
						w.writeLine rowVals.join(delimiter)
						rowVals = null
					}
				}
			}
			
			
			def writeTableQuick2(File out,delimiter,nullVal){
				def rowKeys = rowKeySet()
				def colKeys = colKeySet()

				err.println "rowKeys.size="+rowKeys.size()
				err.println "colKeys.size="+colKeys.size()

				out.withWriter(){w->
					// Print the heading...
					w.write "Features$delimiter"
					w.writeLine colKeys.join(delimiter)

					int rowCount = 0;
					rowKeys.each{rowKey->
						if ((rowCount++ % 500)==0) err.println "$rowCount rows written"
						def rowVals = []
						colKeys.each{colKey->								
							rowmap = this[rowKey]
							if (rowmap.keySet().contains(colKey)){
								rowVals << rowmap[colKey]
							}else{
								rowVals << nullVal
							}
						}
						w.write  "$rowKey$delimiter"
						w.writeLine rowVals.join(delimiter)
						rowVals = null
					}
				}
			}



    /**
    * Write out a 2D table that has max keys in each direction...
    */ 
/*
    def writeTable(File out,delimiter,nullVal,Closure c){

      out.withWriter(){w->

        def rowKeys = this.keySet()

        // Find the union of all column keys...
        def colKeys = [] as Set
                  
        rowKeys.each{rowKey->
          colKeys = colKeys + this[rowKey].keySet()
        }

        // Print the heading...
        w.write("Features$delimiter")
        w.writeLine(colKeys.join(delimiter))

        // Print the table proper...

        rowKeys.each{rowKey->
          def rowVals = []
          colKeys.each{colKey->
            def val = this[rowKey][colKey]
            if (val == [:]) rowVals << nullVal
            else rowVals << c.call(val)
          }
          w.write("$rowKey$delimiter")
          w.writeLine(rowVals.join(delimiter))
        }
      }        
    }
*/


/**   Clean up and make a merge function...
mergeByRowKey  something like that...
		err.print "Reading $f1..."
		d1 = new TwoDMap(f1,"\t")
		err.println "done. ${d1.rows()}x${d1.cols()}"

		err.print "Reading $f2..."
		d2 = new TwoDMap(f2,"\t")
		err.println "done. ${d2.rows()}x${d2.cols()}"


		tout = new TwoDMap()

		d1rows = d1.rowKeySet()
		d1cols = d1.colKeySet()

		d2rows = d2.rowKeySet()
		d2cols = d2.colKeySet()

		d1rows.each{patientID->
			// If a patient isn't in both tables, omit it. 
			if (d2rows.contains(patientID)){
				d1cols.each{feature->
					tout[patientID][feature] = d1[patientID][feature]
				}
				d2cols.each{feature->
					tout[patientID][feature] = d2[patientID][feature]
				}				
			}
		}
*/








}
