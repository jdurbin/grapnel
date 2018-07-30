package grapnel.weka;

import java.nio.file.Paths;
import weka.core.Instances;

/**
*  Class to handle manipulation of multiple signature sets and their 
*  results. 
*/ 
public class SignatureSets extends ArrayList{
		
	def SignatureSets(){}	
	def allResults = []
	
	def modelName2Model = [:]
		
	def SignatureSets(String dirName){
		readSets(dirName)
	}	
	
	// As we add, merge the modelName2Model maps...
	boolean add(SignatureSet s){
		s.modelName2Model.each{k,v->
			modelName2Model[k] = v
		}
		return(super.add(s));
	}
		
	// Reads in an array of signature sets from a directory. 
	def readSets(String dirName){	
		System.err.println "dirName: "+dirName
		System.err.println "ssDN.class:"+dirName.class
		File f = new File(dirName)
		//System.err.println "F = "+f
		//System.err.println "F.class = "+f.class	
		f.eachFileMatch(~/.*.cfg/){cfgFile->
			System.err.println "CONFIG FILE: "+cfgFile
			def ss = new SignatureSet(cfgFile);
			this<<ss
		}
	}
	
	// Apply all signatures in the signature sets to data 
	def applyModelsRecordSets(expressionData){
		def results2sets = [:]
		this.each{signatureSet->
			def results = signatureSet.applyModels(expressionData)
			results.each{r->
				allResults << r
				results2sets[r] = signatureSet
			}
		}
		return([allResults,results2sets])		
	}	
	

	// Apply all signatures in the signature sets to data 
	def applyModels(expressionData){
		this.each{signatureSet->
			def results = signatureSet.applyModels(expressionData)
			results.each{r->
				allResults << r
			}
		}
		return(allResults)		
	}	
}