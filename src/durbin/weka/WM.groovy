package durbin.weka;
import durbin.weka.*
import weka.clusterers.SimpleKMeans
import weka.clusterers.HierarchicalClusterer

/***
* Class to encapsulate nicer versions of WekaMine functions. 
* Change WekaMine itself into WekaMinePipeline or some such...
*/
class WM{
	
	def WM(){
		WekaAdditions.enable()
	}
	
	/*********
	* Reading and writing files...
	*/ 
		
	static def readNumericTab(params=[:]){		
		return(WekaMine.readNumericFromTable(params.file))
	}
	
	static def readTab(params=[:]){		
		return(WekaMine.readFromTable(params.file))
	}
	
	static def makeInstances(data,clin,className){
		return(WekaMine.createInstancesFromDataAndClinical(data,clin,className))
	}	
	
	static def removeID(data){
		return(WekaMine.removeInstanceID(data))
	}
		
		
	/*********
	* CLUSTERING
	*/ 	
		
	static def SimpleKMeans(params=[:]){
		def km = new SimpleKMeans()
		km.setOptions(params2Options(params))
		return(km)
	}
	
	static def HierarchicalClusterer(params=[:]){
		def hc = new HierarchicalClusterer()
		hc.setOptions(params2Options(params))
		return(hc)
	}
	
			
	
	
	static def params2Options(params){
		def options = []
		params.each{k,v->
			// Flags have value true/false
			if (v == true){
			    options <<"-$k"
			}else{
				options << "-$k"
				options << v
			}
		}
		return(options as String[])
	}
	
	
}