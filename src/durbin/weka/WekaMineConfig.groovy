
package durbin.weka;

/********************************
* Really just a bit of sugar for WekaMineConfigSlurper, so that you can just create one
* of these and start iterating over experiments.
*/ 
class WekaMineConfig extends ArrayList{

	def params;
		
	def WekaMineConfig(configFileName){			
		def slurper = new WekaMineConfigSlurper()
		def cfg = slurper.parse(new File(configFileName).toURL())
		params = cfg.params // cfg includes all sections.  make alias for section we want. 

	  slurper.getExpansion('experiments').each{experiment->
	  	this << new ExperimentSpec(experiment);
	  }
	}	
}