#!/usr/bin/env groovy 
package durbin.util;

/**
* A multi-dimensional map.  Commandeered from this post: <br>
* 
* http://old.nabble.com/Multidimensional-maps-td19615874.html <br><br>
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
				def rowSet = this[rowKey].keySet()
				//System.err.println "rowKey: $rowKey  rowSet: $rowSet"
        colKeys = colKeys + this[rowKey].keySet()
      }
			return(colKeys)
		}

		def rows(){
			return(rowKeySet().size())
		}
		
		def cols(){			
			return(colKeySet().size())
		}

    /**
    * Test to see if 2D map (the most common case) already contais
    * the given keys.  Need to generalize to n keys...
    */ 
    public boolean contains(key1,key2){      
      if (!keySet().contains(key1)) return(false);
      else{
        secondMap = this[key1]
        if (!secondMap.keySet().contains(key2)) return(false);
        else return(true);
      }      
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
/*  Bizarre... this doesn't work... the mere introduction of new File()
    breaks it...

    def writeTable(String fileName,delimiter,nullVal){

      def rowKeys = rowKeySet()
			def colKeys = colKeySet()

			bob = new File(fileName)

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
*/
		

	  def printTable(delimiter,nullVal){
			printTable(delimiter,nullVal,heading=true)
		}

    
    /**
    * Print out a 2D table that has max keys in each direction...
    */ 
    def printTable(delimiter,nullVal,heading){

      def rowKeys = rowKeySet()
			def colKeys = colKeySet()

			if (heading){
      	// Print the heading...
      	print "Features$delimiter"
      	println colKeys.join(delimiter)
			}

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
    

		//def writeTable(File out){
		//	writeTable(out,"\t","NA"){}
		//}

		//def writeTable(File out,Closure c){
		//	writeTable(out,"\t","NA",c)
		//}
		

		//def writeTable(String fileName){
		//	writeTable(fileName,"\t","NA")
		//}
		
		//def writeTable(String fileName,delimiter,nullVal){
		//	out = new File(fileName)
		//	writeTable(out,delimiter,nullValue){
		//		it
		//	}
		//}
		
		
	
		
		/**
    * Write out a 2D table that has max keys in each direction...
    */ 
/*
    def writeTable(String fileName){

			out = new File(fileName)
			delimiter = "\t"
			nullVal = "null"
      out.withWriter(){w->
	
				err.println "DEBUG***** "+this.class

        def rowKeys = this.keySet()

        // Find the union of all column keys...
        def colKeys = [] as Set
                  
        rowKeys.each{rowKey->
					err.println "rowKey = $rowKey"
					err.println "this.clas="+this.class
					err.println "this.[rowkey]= "+this[rowKey]
					//err.println "this[rowKey].class"+bob.class
					rowKeyRow = this[rowKey]
					err.println "rowKeyRow.class="+rowKeyRow.class
					
          colKeys = colKeys + (rowKeyRow.keySet())
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
            else rowVals << val
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
