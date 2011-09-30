package durbin.weka;

import weka.core.*
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.evaluation.*;
import weka.classifiers.meta.FilteredClassifier;
import java.util.Random

import durbin.weka.WekaAdditions


/**
* Each foldset is a map from the fold number to a list of sample names. 
*/ 
class FoldSet{
	def idx2sampleList = [:]		
	int numFolds=0;
	def samples;
	
	def FoldSet(samples,foldValues){
		
		this.samples = samples
		
		def foldValueSet = foldValues as Set
		numFolds = foldValueSet.size()
		
		// Each index will map to a list of sample names...				
		for(i in 0..<numFolds){
			idx2sampleList[i] = []
		}
						
		foldValues.eachWithIndex{foldNum,i->
			def foldIdx = foldNum-1 // fold values are 1-based, so convert to 0-based. 
			def sample = samples[i] // i is walking through samples...
			idx2sampleList[foldIdx].add(sample) // add the next sample to the specified fold list.
		}
	}
	
	// 0-based. 
	def getTestSamples(int foldNum){
		return(idx2sampleList[foldNum]);
	}	
	
	def getTrainSamples(int foldNum){
		def testSamples = idx2sampleList[foldNum]
		def trainSamples = samples.collect{it}
		trainSamples.removeAll(testSamples)
		return(trainSamples)
	}	
	
	int numFolds(){
		return(numFolds)
	}
}


/***
* A collection of fold sets. 
*/ 
class FoldSets{
	
	ArrayList<FoldSet> data;
	
	def samples;
	
	def valueSet = [] as Set
	
	static{
		WekaAdditions.enable();
	}	
	
	def FoldSets(){data = new ArrayList()}				
	def FoldSets(fileName){
		read(fileName)
	}
	
	/****
	* Read a foldset file. 
	*/ 
	def read(fileName){
		data = new ArrayList();
		
		new File(fileName).withReader{r->
			def headings = r.readLine().split("\t")
			samples = headings[1..-1] // First column is not a sample
			
			r.splitEachLine("\t"){fields->
				def foldValues = fields[1..-1].collect{it as int}	
				def fs = new FoldSet(samples,foldValues);											
				data.add(fs)
			}
		}		
		return(this)
	}
	
	int size(){return(data.size())}
	
	FoldSet get(i){return((FoldSet) data.get(i))}
	
	// Enables [] notation
	FoldSet getAt(i){return((FoldSet) data.get(i))}
	
	// Enables iteration with each, etc.
	Iterator iterator() {
	       return data.iterator()
	}
	
	
	// Total number of folds across all foldsets
	int countFolds(){
		int totalFolds = 0;					
		data.each{fs->totalFolds+=fs.numFolds()}
		return(totalFolds);
	}
	
	def add(val){
		data.add(val)
	}
			
}