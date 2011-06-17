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
		printResults(Syste.out,results,sampleIDs)
	}
	
	def printResults(out,results,sampleIDs){							
		out<<"ID,lowConfidence,highConfidence,call\n"

		results.eachWithIndex{result,i->
			def r = result as ArrayList	
			def rstr = r.join(",")
			def call
			
			def lowVal = r[0] as double
			def highVal = r[1] as double
			
			if (highVal > lowVal) call = "high"
			else call = "low"
			out<<"${sampleIDs[i]},$rstr,$call\n"
		}		
	}
		
	/***
	* Print the results and compare with the clinical values in clinical...
	*/ 
	def printResultsAndCompare(results,dataSampleIDs,clinical){
		printResultsAndCompare(System.out,results,dataSampleIDs,clinical)
	}
	
	def printResultsAndCompare(out,results,dataSampleIDs,clinical){
		out<< "ID,lowConfidence,highConfidence,call,actual,match\n"
		
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
			
			def lowVal = r[0] as double
			def highVal = r[1] as double
			def call
			if (highVal > lowVal) call = "high"
			else call = "low"
			out<< "${id},$rstr,$call"
			
			// If this sample is in the clinical data set, output it's comparison.. 
			if (id2ClassMap.containsKey(id)){
				def actualVal = id2ClassMap[id]
				out<< ",$actualVal"
				//print "\t$lowVal,$highVal,${lowVal < highVal},${actualVal == 'low'}\t"
				if ((actualVal == "high") && (call == "high")){
					tp++;
					out<<",+\n"
				}else if ((actualVal == "high") && (call == "low")){
					fp++
					out<<"\n"					
				}else if ((actualVal == "low") && (call == "low")){
					tn++;
					out<<",+\n"
				}else if ((actualVal == "low") && (call == "high")){
					fn++;
					out<<"\n"
				}
			}				
		}
		
	 	out<< "====================\n"
		out<< "TP\t$tp\n"
		out<< "FP\t$fp\n"
		out<< "TN\t$tn\n"
		out<< "FN\t$fn\n"
		out<< "Fraction Correct:\t${((tp+tn)/(tp+fp+tn+fn))}\n"
		out<<"Sensitivity:\t${(tp/(tp+fn))}\n"
		out<<"Specificity:\t${(tn/(tn+fp))}\n"										
		out<<"Precision:\t${(tp/(tp+fp))}\n"
		out<<"Recall:\t${(tp/(tp+fn))}\n"			
		def mcc = matthewsCorrelationCoeficient(tp,fp,tn,fn)					
		out<<"Matthews correlation coeficient: $mcc\n"
	}
	
	def matthewsCorrelationCoeficient(tp,fp,tn,fn){
		def sum1 = tp+fp
		def sum2 = tp+fn
		def sum3 = tn+fp
		def sum4 = tn+fn

		def denominator
		if ((sum1 == 0) || (sum2 == 0) || (sum3 ==0) || (sum4 ==0)){
			denominator = 1
		}else{	
			denominator = sum1*sum2*sum3*sum4
		}
		double mcc = ((tp*tn)-(fp*fn))/Math.sqrt(denominator)
		return(mcc.round(4))
	}
					
}
