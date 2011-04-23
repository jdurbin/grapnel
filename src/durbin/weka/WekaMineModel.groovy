package durbin.weka;


/***
* Saves a trained classifier and any additional information needed to apply that classifier to a 
* new dataset.  I thought that the weka.classifier itself contained the instance names, but this seems
* to be true only for some classifiers, so this wrapper saves that and gives a central place to
* put any other model-specific info.   I'm toying with having the logic for using the model 
* here too... hmmm....
*/
class WekaMineModel implements Serializable{
	
	static final long serialVersionUID = 1L;
	
	def atrSelMethod  // Just for records sake
		
	def classifier
	def attributes
	
	// KJD: should save the names of the classes this classifier predicts...
	// high/low... ERPOS, ERNEG, etc.   This will mean,probably, specifying the name of 
	// the classes in the configuration file and then propagating them to the results 
	// output and so on...
			
	def WekaMineModel(instances,classifier){
		
		WekaAdditions.enable()
		
		this.classifier = classifier		
		attributes = instances.attributeNames()		
	}
	
	def classAttribute(){
		return(attributes[-1])
	}
	
	def classify(instances){
		def rval = []
		def numInstances = instances.numInstances()
		for(int i = 0;i < numInstances;i++){
			def instance = instances.instance(i)
			def dist = classifier.distributionForInstance(instance)
			rval.add(dist)
		}
		return(rval)		
	}
		
}
