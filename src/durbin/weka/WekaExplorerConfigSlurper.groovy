#!/usr/bin/env groovy 

package durbin.weka;

import com.google.common.collect.*

/**
* Provides a compact way to describe fairly complex Weka classifier experiments. See 
* cfgExample.txt for an example of the kind of file that this parses.  It extends Groovy's
* nice ConfigSlurper, which is itself a (big) superset of the standard Java parameter file. 
* 
* Info on ConfigSlurper itself can be found here: http://groovy.codehaus.org/ConfigSlurper
*
* Use like: <pre>
* 
* dcs = new WekaExperimentConfigSlurper()
* fullcfg = dcs.parse(new File(args[0]).toURL())
* params = fullcfg.params
* 
* println "cvFolds = "+params.cvFolds
* 
* experiments = dcs.getExpansion('experiments')
* experiments.each{experiment->
*   println experiment
* }
* 
* </pre>
*/
class WekaExplorerConfigSlurper extends ConfigSlurper{
  
  // Compile a regex to match strings that contain paired braces.
  // (Some people, when confronted with a problem, think “I know, I’ll use regular expressions!”
  // Now they have two problems. — Jaime Zawinski)
  static def containsPairedBraces = ~/.*?\{.*\}.*?/
  static def matchBraceContents = ~/\{(.*?)\}/

  // Matches a keyword beginning with $ and ending with whitespace. 
  static def nextKeyword = ~/\$(\w+)/
  
  def cfg;
  def substitutionMap = new HashMultimap()
  
  def getExpansion(key){
    return(substitutionMap.get(key))
  }
  
  def keys(){return(substitutionMap.keySet())}
  

  def WekaExplorerConfigSlurper(){
    super()
  }
  
  
  ConfigObject parseDB(db,configID){
    def sql = "select * from mlexperiments where id = $configID"
    def rows = db.rows(sql)      
    if (rows == null) throw new RuntimeException("ML Experiment $id does not appear to be in database.")
    
    row = rows[0]

      
  }
  

  /***
  * Slurp in the configuration with ConfigSlurper, then expand all the expandable things. 
  * Returns the ConfigObject from ConfigSlurper
  * 
  **/ 
  ConfigObject parse(java.net.URL url){
    cfg = super.parse(url)
    return(parse(cfg))
  }
  
  ConfigObject parse(cfg){
        
    cfg = super.parse(url)
    
    // User defined fields...
    //println cfg.algorithms.keySet()
    cfg.expand.keySet().each{sectionKey->
      def sectionContents = cfg.expand."$sectionKey"      
      sectionContents.each{item->        
        def hasBeenExpanded = false // flag to note if something has been expanded in any way. 

        // Expand all braces in the item...
        def expandedList = []
        if (item.matches(containsPairedBraces)){    
          expandedList = expandBraces([] << item)  
          hasBeenExpanded = true
          //println "braces: sub.putAll($sectionKey,$expandedList)"      
        }else{
          expandedList << item
        }


        if (item.contains("\$")){            
          // Since item may already be brace expanded, need to pass in the entire
          // list of brace expansions to expandDollarSign
          expandedList = expandDollarSign(substitutionMap,expandedList)

          substitutionMap.putAll(sectionKey,expandedList)  
          hasBeenExpanded = true          
          //println "keyword: sub.putAll($sectionKey,$expandedList)"      
        }else{
          if (hasBeenExpanded){
            substitutionMap.putAll(sectionKey,expandedList)
            //println "braces, no keyword: sub.putAll($sectionKey,$expandedList)"      
          }
        }

        // If it hasn't been expanded in any way, just add the item itself to the map..
        if (!hasBeenExpanded){      
          //println "NO Expansion: $item"
          substitutionMap.put(sectionKey,item)
        }    
      }  
    }  
    
    return(cfg)
  }

  /**
  * Recursively expand braces until all expansions are complete. 
  * Returns a list of completely expanded items. 
  **/ 
  def expandBraces(list){
    def bAnyNewMatches = false;
    
    def newlist = []
    list.each{item->
      if (item.matches(containsPairedBraces)){      
        // For each pair of braces...  group1 contains match w/o braces. 
        // This is equivalent to, but perhaps more readable than:  
        //    m = mystring =~ matchBraceContents
        //    group1 = m[0][1]
        item.find(matchBraceContents){fullmatch,group1->  
          bAnyNewMatches = true;
          def (start,stop,inc) = parsefields(group1)    
          for(double value = start;value <= stop;value+= inc){
            def newItem = item.replaceFirst(matchBraceContents,value as String)
            //println "BRACES NEWLIST: $newItem"
            newlist << newItem
          }        
        }
      }
    }

    // we found some unexpanded braces, maybe there are more, so search for 
    // them...
    if (bAnyNewMatches){
      newlist = expandBraces(newlist)
    }else{  
      // otherwise, recursion is over, return the list we have
      return(list)
    }
  }

  /**
  * Recursively expand dollar sign substitutions until all expansions are complete. 
  * Returns a list of completely expanded items.  Note: it is up to user to ensure that
  * configuration file is structured so that dependencies come before the thing that
  * depends on them. 
  */ 
  def expandDollarSign(substitutionMap,list){

    def bAnyNewMatches = false;

    def newlist = []
    list.each{item->
      if (item.contains("\$")){   

        item.find(nextKeyword){fullmatch,keyword->         
          bAnyNewMatches = true;
          def keywordExpansions = substitutionMap.get(keyword)
          keywordExpansions.each{ value ->
            def newItem = item.replaceFirst(nextKeyword,value)
            //println "DOLLAR NEWLIST: $newItem"
            newlist << newItem
          }
        }
      }
    } 

    // we found some unexpanded keywords, maybe there are more, so search for 
    // them...
    if (bAnyNewMatches){
      newlist = expandDollarSign(substitutionMap,newlist)
    }else{  
      // otherwise, recursion is over, return the list we have
      return(list)
    }
  }

  /**
  * Parse the three fields out of a pair of range braces {start,end,step}
  *
  **/ 
  def parsefields(group1){
    def fields = group1.split(",")
    def start = fields[0] as Double
    def stop = fields[1] as Double
    def inc = fields[2] as Double
    return [start,stop,inc]
  }

}