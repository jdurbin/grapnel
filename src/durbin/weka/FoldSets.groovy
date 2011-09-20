package durbin.weka;

import weka.core.*
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.evaluation.*;
import weka.classifiers.meta.FilteredClassifier;
import java.util.Random

import durbin.weka.WekaAdditions


class FoldSets{
	
	ArrayList data;
	
	def samples;
	
	def valueSet = [] as Set
	
	static{
		WekaAdditions.enable();
	}	
	
	def FoldSets(){data = new ArrayList()}
		
	ArrayList<Integer> get(int i){
		return(data.get(i))
	}
		
	def FoldSets(fileName){
		read(fileName)
	}
	
	int foldSize(){
			for(int i = 0;i < data.size();i++){
				def currentSet = data[i]		
				for(int j = 0;j < currentSet.size();j++){
					valueSet.add(currentSet[j])										
				}
			}
			return(valueSet.size())
	}
	
	// Total number of folds across all foldsets
	int countFolds(){					
		int numFolds = foldSize() * data.size();		
		return(numFolds);
	}
	
	def read(fileName){
		data = new ArrayList();
		
		new File(fileName).withReader{r->
			def headings = r.readLine().split("\t")[1..-1]
			samples = headings
			//err.println "headings: "+headings
			r.splitEachLine("\t"){fields->
				def foldValues = fields[1..-1].collect{it as int}	
				
				//System.err.println "before foldValues.size: ${foldValues.size()} foldValues:$foldValues"
				
				// Convert to zero base from 1 base..
				foldValues = foldValues.collect{it -1}	
				//System.err.println "foldValues.size: ${foldValues.size()} foldValues:$foldValues"
				
				//System.err.println "DEBUG foldValues = "+foldValues
						
				data.add(foldValues as ArrayList<Integer>)
			}
		}
		
		//System.err.println "DEBUG: data="+data
		
		return(this)
	}
	
	def add(val){
		data.add(val)
	}
	
	int size(){return(data.size())}
	
	ArrayList get(i){return(data.get(i))}
	ArrayList getAt(i){return(data.get(i))}
	

	def removeMissing(Instances instances){		
		
		def dataSamples = instances.attributeValues("ID") as Set
		
		// Make copy of old list...
		def oldList = new FoldSets()
		data.each{oldList.add(it)}
				
		// clear this for new list...
		data.clear()
		for(int i = 0;i < oldList.size();i++){
			def currentSet = oldList[i]
			def newset = []
			for(int j = 0;j < currentSet.size();j++){
				if (dataSamples.contains(samples[j])){
					newset << currentSet[j]
				}
			}
			data.add(newset);
		}
		return(this)			
	}
			
}