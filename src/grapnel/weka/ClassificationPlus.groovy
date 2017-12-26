package grapnel.weka;

// Add sample ID to Classification.
public class ClassificationPlus implements Serializable{
	def sampleID
	def prForValues = []
	def nullForValues = []
	def classValues = []
	def class2Idx =[:]
	def nompred
	def modelName
	def preferredIdx
	
			
	def ClassificationPlus(id,modelName,classification,nullconfs,prefIdx){
		this.sampleID = id
		this.prForValues = classification.prForValues
		this.classValues = classification.classValues
		this.nompred = classification.nompred
		this.nullForValues = nullconfs
		this.modelName = modelName
		preferredIdx = prefIdx		
		classValues.eachWithIndex{k,i->
			class2Idx[k] = i
		}
	}
	
	def call(){
		def maxIdx = getMaxIdx(prForValues)
		def call = classValues[maxIdx]
		return(call)
	}
	
	
	/**
	* Determine the best call and the associated class index. 
	*/ 
	def bestCallAndIdx(){
		def maxIdx = getMaxIdx(prForValues)
		def call = classValues[maxIdx]
		return([call,maxIdx])
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
	
}