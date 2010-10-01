#!/usr/bin/env groovy 

/**
* A multi-dimensional map.  Commandeered from this post:
* 
* http://old.nabble.com/Multidimensional-maps-td19615874.html
* 
* Through Groovy magic you can access elements of map with 
* dot notation or with [] notation.  
* 
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
* 
* WARNING:  The map bracket syntax **CREATES** an entry if one doesn't 
* exist.  Care must be taken when checking for existence of a value for 
* a particular cell that you do not inadvertently create a new entry. 
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


/****************
*  Special case of MultidimensionalMap for the common 2D case...
* 
*  Differs from Table mainly in that it's completely dynamic, whereas 
*  Table requires explicit dimensions up front. 
* 
*/ 
class TwoDMap extends MultidimensionalMap{
    
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
    def printTable(delimiter,nullVal){

      def rowKeys = this.keySet()

      // Find the union of all column keys...
      def colKeys = [] as Set
      rowKeys.each{rowKey->
        colKeys = colKeys + this[rowKey].keySet()
      }

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
}
