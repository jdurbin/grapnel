
package durbin.weka;

/**
* Really just a bit of sugar for WekaMineConfigSlurper, so that you can just create one
* of these and start iterating over experiments.  For example:
* 
* <pre>
* 
* experiments = new WekaMineConfig(configFileName)
* 
* experiments.each{exp->
*    doSomethingWithExperiment(exp)
* } 
* </pre>
*/ 
class WekaMineConfig extends ArrayList{

	def params;
	
	def WekaMineConfig(){}
		
		
	/**
	* Reads the configuration as a flat list of experiments instead of a config file. 
	*/ 
	def readFlat(String experimentList,Range range){
		def headingMap = WekaMineResult.defaultHeadingMap()
		def lineIdx = 0;
		new File(experimentList).withReader{r->
			String heading = r.readLine()
			lineIdx = 0;
			
			// If -1 given in range, go from From to end...
			if (range.getTo() == -1){
				range.eachLine{expStr->
					if (lineIdx >= range.getFrom()){
						this << new ExperimentSpec(expStr,headingMap);
					}
					lineIdx++
				}
			}else{			
				r.eachLine{expStr->
					if (range.contains(lineIdx)){
						this << new ExperimentSpec(expStr,headingMap);
					}
					lineIdx++
				}
			}
		}
	
		params = new java.util.Properties()
		
		// default parameters since flat config file doesn't have any...
		// Hmmm.. maybe these should be tacked onto the end of each experiment...
		params.cvFolds = 10;
		params.cvSeed = -1;		
	
		return(lineIdx);
	}
		
	/**
	* If there is a chance the config will contain the "ALL" keyword, this version of the 
	* constructor will be able to fill in the headers...
	*/ 	
	def WekaMineConfig(String configFileName,String metaDataFile){			
		def slurper = new WekaMineConfigSlurper()
		def cfg = slurper.parse(new File(configFileName).toURL())
		params = cfg.params // cfg includes all sections.  make alias for section we want. 		

		// read the attribute names from the file...
		// KJD: This ONLY works with attribute names in rows...
		def attributeNames = [];
		new File(metaDataFile).withReader{r->
			def heading = r.readLine()
			r.splitEachLine("\t"){fields->							
				attributeNames << fields[0] 
			}
		}
		
		def headingMap = WekaMineResult.defaultHeadingMap()
	  slurper.getExpansion('experiments').each{experiment->
			
			experiment = experiment.replaceAll(",","\t") // convert the commas in the cfg file to tabs... all tabs from here on out..
		
			// If experiment calls for ALL attributes, substitute them in... 
			if (experiment.contains("ALL")){
				attributeNames.each{attributeName->
					def attrExperiment = experiment.replaceAll("ALL",attributeName)
					this << new ExperimentSpec(attrExperiment,headingMap);
				}
			}else{		
	  		this << new ExperimentSpec(experiment,headingMap);
			}
	  }
	}	
		
		
	def WekaMineConfig(configFileName){			
		def slurper = new WekaMineConfigSlurper()
		def cfg = slurper.parse(new File(configFileName).toURL())
		params = cfg.params // cfg includes all sections.  make alias for section we want. 

		def headingMap = WekaMineResult.defaultHeadingMap()
	  slurper.getExpansion('experiments').each{experiment->
	  	this << new ExperimentSpec(experiment,headingMap);
	  }
	}	
}