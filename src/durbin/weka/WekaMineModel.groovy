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

	def	discretization

	def classifier
	def attributes
	
	
	// KJD: should save the names of the classes this classifier predicts...
	// high/low... ERPOS, ERNEG, etc.   This will mean,probably, specifying the name of 
	// the classes in the configuration file and then propagating them to the results 
	// output and so on...
			
	def WekaMineModel(instances,classifier){
		
		WekaAdditions.enable()
		
		this.classifier = classifier		
		this.discretization = "none"
		attributes = instances.attributeNames()		
	}
	
	def WekaMineModel(instances,classifier,discretization){		
		WekaAdditions.enable()		
		this.classifier = classifier		
		this.discretization = discretization
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
	
	
	def printResults(results,sampleIDs){								
		println "ID,low,high"

		results.eachWithIndex{result,i->
			def r = result as ArrayList	
			def rstr = r.join(",")
			println "${sampleIDs[i]},$rstr"
		}		
	}
		
	/***
	* Print the results and compare with the clinical values in clinical...
	*/ 
	def printResultsAndCompare(results,dataSampleIDs,clinical){
		println "ID,low,high,actual,match"
		
		WekaAdditions.enable();
		
		// Build a map of clinical samples to results...
		def id2ClassMap = [:]
		def classValues = clinical.attributeValues(clinical.classAttribute().name()) as ArrayList
		def clinicalSampleIDs = clinical.attributeValues("ID") as ArrayList
		(0..classValues.size()).each{i->
			def id = clinicalSampleIDs[i]
			def classVal = classValues[i]
			id2ClassMap[id] = classVal
		}
		
		def tp = 0.0;
		def tn = 0.0;
		def fp = 0.0;
		def fn = 0.0;
							
		// Now compare predictions with actual values...
		results.eachWithIndex{result,i->
			def r = result as ArrayList	
			def rstr = r.join(",")
			def id = dataSampleIDs[i]
						
			print "${id},$rstr"
			
			// If this sample is in the clinical data set, output it's comparison.. 
			if (id2ClassMap.containsKey(id)){
				def actualVal = id2ClassMap[id]
				print ",$actualVal"
				def lowVal = r[0] as double
				def highVal = r[1] as double
				//print "\t$lowVal,$highVal,${lowVal < highVal},${actualVal == 'low'}\t"
				if (actualVal == "high"){
					if (lowVal < highVal){
						tp++;
						println ",+"
					}else{
						fp++;
						println ""
					}
				}else{
					if (lowVal > highVal){
						tn++;
						println ",+"
					}else{						
						fn++;
						println ""
					}					
				}
			}				
		}
		
		println ""
		println "TP:$tp\tFP:$fp\tTN:$tn\tFN:$fn"
		println "Fraction Correct:\t"+((tp+tn)/(tp+fp+tn+fn))
		println "Sensitivity:\t"+(tp/(tp+fn))
		println "Specificity:\t"+(tn/(tn+fp))										
		println "Precision:\t"+(tp/(tp+fp))
		println "Recall:\t"+(tp/(tp+fn))								
	}
					
}
