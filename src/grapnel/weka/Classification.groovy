package grapnel.weka;
import grapnel.util.*

import weka.core.*
import weka.classifiers.evaluation.*

/**
*  Package up distForInstance with the associated class names
*/ 
class Classification{		
	def prForValues = []
	def classValues = []
	def class2Idx = [:]
	def nompred
	
						
	def Classification(prForValues,classValues){
		this.prForValues = prForValues
		this.classValues = classValues				
		classValues.eachWithIndex{k,i->
			class2Idx[k] = i
		}
	}		
	
	def prForName(name){
		def rval = 0;
		classValues.eachWithIndex{cname,i->
			if (name == cname) {				
				rval = prForValues[i]
				return;
			}
		}
		return(rval);
	}
	
	def indexForClassName(name){
		def rval = -1;
		classValues.eachWithIndex{cname,i->
			if (name == cname) {				
				rval = i;
				return;
			}
		}
		return(rval);		
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