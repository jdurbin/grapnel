
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