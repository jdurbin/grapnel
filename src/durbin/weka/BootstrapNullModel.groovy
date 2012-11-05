import weka.core.*
import durbin.weka.*

import hep.aida.bin.DynamicBin1D;

/**
* A class to store and create a null model.
*/ 
class BootstrapNullModel implements Serializable{
	
	
	// Set to the auto value because a big run produced serialized objects with this
	// value that I don't want to throw away. 
	static final long serialVersionUID = -7698666011979768695;
	
	// BRCA_IPZ run used this serialVersionUID.  Oughf.  
	//serialVersionUID = 2661308148788524061, local class 
	
	static{
		WekaAdditions.enable()
	}
	
	//def instances
	static def err = System.err // sugar

	ArrayList<DynamicBin1D> nullDistribution = new ArrayList<DynamicBin1D>()		
	ArrayList<String> classValues = new ArrayList<String>()

	def BootstrapNullModel(classValues){
		this.classValues = classValues		
		def numClassValues = classValues.size()
		for(i in 0..<numClassValues){
			nullDistribution << new DynamicBin1D()
		}
		
		//nullDistribution.each{
		//	println "Null dist: $it"
		//}		
	}
	
	def getSignificance(value,nullIdx){
		def nulldist= nullDistribution[nullIdx]
		def significance = nulldist.quantileInverse(value)
		return(significance)
	}
		
	/**
	* Adds points for the null distribution for each class value. 
	*/ 
	def addPoints(ArrayList<Classification> results){		
		// Each result is a distribution for instance...
		//def classValues = model.classValues		
		results.each{r->
			def prForValues = r.prForValues
			
			// Excessively paranoid sanity test
			r.classValues.eachWithIndex{c,i->if (c != classValues[i]) err.println "ERROR: BNM results class values do not match BNM stored class values."}
						
			prForValues.eachWithIndex{probabilityForClassValue,classValueIdx->
				nullDistribution[classValueIdx].add(probabilityForClassValue); 
			}
		}		
	}
	
	/****
	* For each attribute, permute the values.  This will scramble the relationship between
	* each gene and the class while retaining the same distribution of attribute values 
	* within an attribute. 
	*/			
	static def permuteAttributeValues(data){
		def newData = new Instances(data);  // How deep is this copy?  Is there a deeper copy?
		for(attrIdx in 0..< newData.numAttributes()){
			
			// get values for that attribute...
			def attrVals = newData.attributeToDoubleArray(attrIdx) as ArrayList
			
			// Shuffle them...
			Collections.shuffle(attrVals)
			
			// Put them back in shuffled order. 
			newData.eachWithIndex{instance,instanceIdx->
				instance.setValue(attrIdx,attrVals[instanceIdx])
			}					
		}
				
		return(newData)
	}
		
	/****
	*  Permuting the class labels will leave each gene vector as a "real" gene 
	*  vector.  Some think this is less useful than permuting attribute values. 
	*/ 			
	static def permuteClassLabels(data){
		
		def newData = new Instances(data);  // How deep is this copy?  Is there a deeper copy?
		
		def classIdx = newData.classIndex()
		def attrVals = newData.attributeToDoubleArray(classIdx) as ArrayList
		Collections.shuffle(attrVals) 
		
		newData.eachWithIndex{instance,i->
			instance.setClassValue(attrVals[i])
		}						
		return(newData)
	}
}



// test permute function... it's currently destructive to input... how to make a copy of a set of instances?
// write code to permute/eval/permute