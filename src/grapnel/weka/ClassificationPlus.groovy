package grapnel.weka;

// Add sample ID to Classification.
public class ClassificationPlus implements Serializable{
	def sampleID
	def prForValues = []
	def nullConf0
	def nullConf1
	def nullForValues = []
	def classValues = []
	def nompred
	def modelName
	
	def prForName(name){
		classValues.eachWithIndex{cname,i->
			if (name == cname) return(prForValues[i])
		}
		System.err.println "WARNING: prForName: $name not found."
		return(-1);
	}
			
	def ClassificationPlus(id,modelName,classification,nullConf0,nullConf1){
		this.sampleID = id
		this.prForValues = classification.prForValues
		this.classValues = classification.classValues
		this.nompred = classification.nompred
		this.nullConf0 = nullConf0
		this.nullConf1 = nullConf1
		this.modelName = modelName
	}
	
	def call(){
		def maxIdx = getMaxIdx(prForValues)
		def call = classValues[maxIdx]
		return(call)
	}
	
	def callAndIdx(){
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