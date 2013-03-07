package durbin.weka;

import weka.core.*

// Package up distForInstance with the associated class names
class Classification{						
	def Classification(prForValues,classValues){
		this.prForValues = prForValues
		this.classValues = classValues
	}
	
	def prForValues = []
	def classValues = []
	
	def prForName(name){
		classValues.eachWithIndex{cname,i->
			if (name == cname) return(prForValues[i])
		}
		System.err.println "WARNING: prForName: $name not found."
		return(-1);
	}
	
}

/***
* Saves a trained classifier and any additional information needed to apply that classifier to a 
* new dataset.  I thought that the weka.classifier itself contained the instance names, but this seems
* to be true only for some classifiers, so this wrapper saves that and gives a central place to
* put any other model-specific info.   I'm toying with having the logic for using the model 
* here too... hmmm....
*/
class WekaMineModel implements Serializable{
	
	static {WekaAdditions.enable()}
	
	static err = System.err
	
	static final long serialVersionUID = 1L; // original version
	//static final long serialVersionUID = 2L; // modded to add data and clin file names.
	
	def atrSelMethod  // Just for records sake

	def	discretization
	def filter
	def classifier
	def attributes
	def className
	def classValues	
	
	BootstrapNullModel bnm; 
		
	String toString(){
		def rval = "";
		rval += "AttributeSeletion: $atrSelMethod\n"
		rval += "Discretization: $discretization\n"
		rval += "Class Name: $className\n"
		rval += "ClassValues: ${classValues.join(";")}\n"				
		rval += "Classifier: $classifier\n"
		return(rval);
	}
	
	def attributes(){
		return(attributes);
	}
	
	def setNullModel(BootstrapNullModel nullmodel){
		bnm = nullmodel;
	}
				
/*				
	def WekaMineModel(instances,classifier){		
		this.classifier = classifier		
		this.discretization = "none"
		attributes = instances.attributeNames()	
		attributes = attributes - "ID" // Don't save ID if there is one. 
				
		className = instances.className()
		attributes = attributes - className	
		
		classValues = []
		def classAttr = instances.attribute(className)
		for(i in (0..<classAttr.numValues())){
			classValues.add(classAttr.value(i))
		}		
	}
	
	def WekaMineModel(instances,classifier,discretization){		
		this.classifier = classifier		
		this.discretization = discretization
		attributes = instances.attributeNames()	
		attributes = attributes - "ID" // don't save ID if there is one. 
		
		// Want to keep class attribute separate from attributes.
		className = instances.className()
		attributes = attributes - className	

		classValues = []
		def classAttr = instances.attribute(className)
		for(i in (0..<classAttr.numValues())){
			classValues.add(classAttr.value(i))
		}				
	}	
*/
	
	
	def WekaMineModel(instances,classifier,discretization,filter){
		
		// Train the classifier with the instances...
		classifier.buildClassifier(instances);	
		
		this.filter = filter		
		this.classifier = classifier		
		this.discretization = discretization
		attributes = instances.attributeNames()	
		attributes = attributes - "ID" // don't save ID if there is one. 
		
		// Want to keep class attribute separate from attributes.
		className = instances.className()
		attributes = attributes - className	

		// Save the class values so we can use them later...
		classValues = []
		def classAttr = instances.attribute(className)
		for(i in (0..<classAttr.numValues())){
			classValues.add(classAttr.value(i))
		}				
	}
	
	def classAttribute(){
		return(className)
		//return(attributes[-1])
	}
	
	/****
	*
	*/ 
	ArrayList<Classification> classify(instances){				
		def rval = new ArrayList<Classification>()
		def numInstances = instances.numInstances()		
		
		for(int i = 0;i < numInstances;i++){
			def instance = instances.instance(i)											
			def prForValues = classifier.distributionForInstance(instance)						
			// Package dist up in a Classification while we have the instance
			// handy, just be sure we don't mix them up. 
			def classification = new Classification(prForValues,classValues)
			rval.add(classification)
		}
		return(rval)		
	}
	
	/***
	* Permutes the class lables and evaluates performance in a cross-validated setting. 
	* Accumulated values are saved in BootstrapNullModel for future significance computations. 
	**/ 
	def computeBootstrapNullModel(instances){						
		def rng = new Random();		
		bnm = new BootstrapNullModel(classValues)
		ArrayList<Classification> results = classify(instances)
		bnm.addPoints(results)
	}
	
	
	
	/***
	* Permutes the class lables and evaluates performance in a cross-validated setting. 
	* Accumulated values are saved in BootstrapNullModel for future significance computations. 
	**/ 
	def computeBootstrapNullModel(instances,nullModelIterations){						

		def rng = new Random();
		
		bnm = new BootstrapNullModel(classValues)
									
		// Generate the desired number of permutations of the model...
		for(i in 0..<nullModelIterations){
			err.print "Null model iteration $i..."
			//def pinstances = bnm.permuteClassLabels(instances)
			def pinstances = bnm.permuteAttributeValues(instances)
			
			// Apply classifier to these instances...
			// one result per instance, each result is a distribution for instance...
			// Note: it is assumed that instances have already been cleaned up. 
			ArrayList<Classification> results = classify(pinstances)
			
			// Will need to have a null distribution for each class value... 						
			bnm.addPoints(results)
			err.println "done."
		}
	}
		
	def printResults(	ArrayList<Classification> results,sampleIDs){
		printResults(System.out,results,sampleIDs)
	}
	
	def getMaxIdx(list){
		def maxVal = -9999999;
		def maxIdx = 0;
		list.eachWithIndex{val,i->
			if (val > maxVal) {
				maxVal = val
				maxIdx = i
			}
		}
		return(maxIdx)
	}
	
	
		
	/***
	* Print the results and compare with the clinical values in clinical...
	*/ 
	def printResultsAndCompare(results,dataSampleIDs,clinical){
		printResultsAndCompare(System.out,results,dataSampleIDs,clinical)
	}
	
	
	/***
	* QUESTIONABLE
	* 
	* Used by buildAndEvaluateOnHoldout in WekaMine.groovy
	*/	
	def accuracy(dataSampleIDs,results,clinical){
		def tp,tn,fp,fn
		(tp,tn,fp,fn) = confusionMatrixFor(dataSampleIDs,results,clinical)
		def accuracy = (tp+tn)/(tp+tn+fp+fn)
		return(accuracy)
	}
	
	/***
	* QUESTIONABLE
	* Only seems to be used by accuracy() method above. 
	*	Evaluate the accuracy of the given results...
	*/
	def confusionMatrixFor(dataSampleIDs,results,clinical){
		
			err.println "confusionMatrixFor THIS CODE IS FLAGGED FOR REVIEW"
		
			// Build a map of clinical samples to results...
			def id2ClassMap = [:]						
			def classValues = clinical.attributeValues(clinical.classAttribute().name()) as ArrayList
			def classSet = classValues as Set
			classSet = classSet as ArrayList // convert the set back to an array so we can index it later...

			if (classSet.size() > 2){
				err.println "Sorry, but model comparison currently doesn't support more than two class values."
				err.println "This is planned for an update soon."
				err.println "ClassValues:"
				err.println classSet
				return;
			}

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

			// Will break in a hurry if there are more than two...
			def val0 = classSet[0]
			def val1 = classSet[1]					

			def confusionForSample = []
			
			// Now compare predictions with actual values...
			results.eachWithIndex{result,i->
				def prForValues = result.prForValues as ArrayList
				//def r = result as ArrayList	// distribution for instance...			
				def maxIdx = getMaxIdx(prForValues)
				def call = classValues[maxIdx] // look up the name of this.
				def rstr = prForValues.join(",")	

				def id = dataSampleIDs[i]
				// If this sample is in the clinical data set, output it's comparison.. 
				if (id2ClassMap.containsKey(id)){
					def actualVal = id2ClassMap[id]
					if ((actualVal == val1) && (call == val1)){
						tp++;						
					}else if ((actualVal == val1) && (call == val0)){
						fp++
					}else if ((actualVal == val0) && (call == val0)){
						tn++;
					}else if ((actualVal == val0) && (call == val1)){
						fn++;
					}				
				}
			} // results.each
			return([tp,tn,fp,fn])
	}
		
	/***
	*  DEFECT: Will break on multi-class classifiers. 
	*/ 
	def printResults(out,	ArrayList<Classification> results,sampleIDs){
		
		//err.println "DEBUG: results.size=${results.size()} sampleIDs.size=${sampleIDs.size()}"
		
		if (bnm != null){
			out<<"ID,confidence1,confidence2,call,nullConfidence0,nullConfidence1\n"
		}else{
			out<<"ID,confidence1,confidence2,call\n"
		}
									
		results.eachWithIndex{result,i->			
			
			def prForValues = result.prForValues as ArrayList						
			def maxIdx = getMaxIdx(prForValues)
			def call = classValues[maxIdx] // look up the name of this.
			def prstr = prForValues.join(",")	
			
			if (bnm != null){
				def nullConf0 = bnm.getSignificance(prForValues[0],0)						
				def nullConf1 = bnm.getSignificance(prForValues[1],1)
				out<<"${sampleIDs[i]},$prstr,$call,$nullConf0,$nullConf1\n"
			}else{
				out<<"${sampleIDs[i]},$prstr,$call\n"
			}
		}		
	}	
	
	/***
	* results == distributionForInstance
	* dataSampleIDs == instanceIDs
	* clinical == clinical data
	* 
	*/ 
	def printResultsAndCompare(out,ArrayList<Classification> results,dataSampleIDs,Instances clinical){
		
		if (bnm != null){
			out<< "ID,lowConfidence,highConfidence,call,nullConfidence0,nullConfidence1,actual\n"		
		}else{
			out<< "ID,lowConfidence,highConfidence,call,actual\n"			
		}
						
		WekaAdditions.enable();
		
		// Build a map of clinical samples to results...
		def id2ClassMap = [:]		
		def allClassValues = clinical.attributeValues(clinical.classAttribute().name()) 
		//err.println "DEBUG allClassValues.size: "+allClassValues.size()
		//allClassValues.each{id->
		//	err.println "DEBUG allclassValues: $id"
		//}
		
		//err.println "DEBUG NUM INSTANCES: "+clinical.numInstances()
		
		err.println "\n\n"
		dataSampleIDs.eachWithIndex{id,i->
			err.println "DEBUG dataSampleID: $id  i: $i  allClassValues[i]=${allClassValues[i]}"
			id2ClassMap[id] = allClassValues[i]
		}

		// fill in id2ClassMap from clinical...		
		def tp = 0.0;
		def tn = 0.0;
		def fp = 0.0;
		def fn = 0.0;
		
		// The positive value is the value of classValues[1]. It is "positive" only 
		// in the sense that that's the value we report a confusion matrix for in 
		// the summary.csv output.  It could mean anything.  
		// MULTICLASS DEFECT:  formulation doesn't work with multiple class values. 
		def posVal = classValues[1]
		def negVal = classValues[0]				
		
		// Sanity check that the results are produced with the same class values as the instances we are 
		// looking at. 
		def diff1 = classValues-allClassValues
		def diff2 = allClassValues-classValues
		if (!(diff1==[] && diff2==[])){
			err.println "ERROR: The class values in the given input data do not match the classValues from the classification."
			err.println classValues
			err.println allClassValues
		}
																
		// Now compare predictions with actual values...
		results.eachWithIndex{result,i->
			def prForValues = result.prForValues as ArrayList  // distribution for instance
			def maxIdx = getMaxIdx(prForValues)
			def call = classValues[maxIdx] // look up the name of this.
			def rstr = prForValues.join(",")	
			
			// ASSUMPTION: dataSampleIDs are in same order as results... 			
			def id = dataSampleIDs[i]
			
			out<< "${id},$rstr,$call"
			
			if (bnm != null){
				def nullConf0 = bnm.getSignificance(prForValues[0],0)						
				def nullConf1 = bnm.getSignificance(prForValues[1],1)
				out<<",$nullConf0,$nullConf1"
			}else{
			}
			
			// If this sample is in the clinical data set, output it's comparison.. 
			if (id2ClassMap.containsKey(id)){
				def actualVal = id2ClassMap[id]
				out<< ",$actualVal"																							
				if ((actualVal == posVal) && (call == posVal)){
					tp++;
					out<<",+\n"
				}else if ((actualVal == posVal) && (call == negVal)){
					fp++
					out<<"\n"					
				}else if ((actualVal == negVal) && (call == negVal)){
					tn++;
					out<<",+\n"
				}else if ((actualVal == negVal) && (call == posVal)){
					fn++;
					out<<"\n"
				}else{
					out<<",?\n"// If there is no actualVal...
				}				
			}else{
				out<< ",?\n"
			}
		} // results.each				
		
	 	out<< "====================\n"
	  out<< "Positive: $posVal\n"
	  out<< "Negative: $negVal\n"
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
		return(mcc.round(6))
	}
					
}
