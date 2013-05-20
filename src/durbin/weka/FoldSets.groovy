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
	def bHasHoldout = true;
	
	def err = System.err
	
	def FoldSet(samples,foldValues){
		
		this.samples = samples
		
		def foldValueSet = foldValues as Set
		
		// numFolds is number of folds for CV purposes.  Foldset may have extra 
		// values for holdout but those don't count as a fold for CV purposes. 
		if (foldValueSet.contains(0)) {
			bHasHoldout = true;
			numFolds = foldValueSet.size() -1; 
		}else {
			bHasHoldout = false;
			numFolds = foldValueSet.size()
		}

		// Each index will map to a list of sample names...				
		for(i in -1..<foldValueSet.size()){ idx2sampleList[i] = [] }									
							
		foldValues.eachWithIndex{foldNum,i->			
			
			if (foldNum == -1) return;  // -1 == samples to skip.
			
			def foldIdx = foldNum-1 // fold values are 1-based, so convert to 0-based. 
			def sample = samples[i] // i is walking through samples...			
						
			//System.err.println "foldNum: $foldNum  i: $i numFolds:$numFolds foldIdx:$foldIdx"
			idx2sampleList[foldIdx].add(sample) // add the next sample to the specified fold list.
		}
	}
	
	def getHoldoutSamples(){
		if (bHasHoldout) return(idx2sampleList[-1]);
		else{
			throw new Exception("ERROR Holdout requested but none defined.");
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
* 
* Hacking it up a bit now ... adding a map version so that can get foldset by 
* name.  This to enable saving a foldset for each class attribute, one for TP53, another for TTN, etc. 
* The original purpose of multiple foldsets was to implement 5 times 5x cross validation, each line
* was one of the replicates.  But I want just one 10x fold per class attribute.  For now, use will 
* determine which it means, but need to iron this out in some sensible way...
*/ 
class FoldSets{
	
	def err = System.err
	
	ArrayList<FoldSet> data;
	HashMap<String,FoldSet> map;
	
	def samples;
	
	def valueSet = [] as Set
	
	static{
		WekaAdditions.enable();
	}	
	
	def FoldSets(HashMap<String,FoldSet> map,ArrayList<FoldSet> data){
		this.data = data
		this.map = map
	}
	
	def FoldSets(){map = new HashMap<String,FoldSet>();data= new ArrayList()}				
	def FoldSets(fileName){
		read(fileName)
	}
	
	/****
	* Read a foldset file. 
	*/ 
	def read(fileName){
		map = new HashMap<String,FoldSet>();
		data = new ArrayList();
		
		new File(fileName).withReader{r->
			def headings = r.readLine().split("\t")
			samples = headings[1..-1] // First column is not a sample
			
			r.splitEachLine("\t"){fields->
				def foldName = fields[0]
				def foldValues = fields[1..-1].collect{it as int}	
				def fs = new FoldSet(samples,foldValues);											
				map[foldName] = fs
				data.add(fs)
			}
		}		
		return(this)
	}
	
	int size(){return(data.size())}
	
	FoldSet get(i){
		return((FoldSet) data.get(i))
	}
	
	// Enables [] notation
	FoldSet getAt(i){return((FoldSet) data.get(i))}
	
	// Enables iteration with each, etc.
	Iterator iterator() {
	       return data.iterator()
	}
	
	
	/***
	* Finds the subset of foldsets that apply to this attribute
	* combined with the generic foldsets. 
	* 
	* If the foldset has a tag UNIVERSAL, it will be used for all 
	* attributes...
	*/ 
	FoldSets getFoldSetsForAttribute(Attribute a){
		// Get foldsets matching fold\d+
		//def foldKeys = map.keySet().grep(~/fold\d+/)
		def foldKeys = map.keySet().grep(~/[fF]old(s)*\d+/)
		def foldMap = map.subMap(foldKeys)
		
		//map.each{k,v-> println "Map: $k\t$v"}
		//err.println "foldKeys:"+foldKeys
		//err.println "foldMap: "+foldMap
		
		// Get foldsets matching attribute name
		def attributeName = a.name()
		def pattern = "${attributeName}.*"
		def attributeKeys = map.keySet().grep(~/${pattern}/)
		def attributeMap = map.subMap(attributeKeys)
		
		//err.println "map.keySet()= "+map.keySet()
		//err.println "attributeName: "+attributeName
		//err.println "attributeKeys:"+attributeKeys
		//err.println "attributeMap: "+attributeMap
		
		// Merge the two maps...
		def newMap = attributeMap + foldMap
		def newData = newMap.values() as ArrayList				
		def newFoldSet = new FoldSets(newMap,newData)
		return(newFoldSet)
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