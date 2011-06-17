#!/usr/bin/env groovy 

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
		def TwoDMap(fileName,delimiter){
			read(fileName,delimiter)
		}

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
          else rowVals << val
        }
        print  "$rowKey$delimiter"
        println rowVals.join(delimiter)        
      }
    }
    
    /**
    * Write out a 2D table that has max keys in each direction...
    */ 
    def writeTable(fileName,delimiter,nullVal){

      new File(fileName).withWriter(){w->

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
            else rowVals << val
          }
          w.write("$rowKey$delimiter")
          w.writeLine(rowVals.join(delimiter))
        }
      }        
    }
}
