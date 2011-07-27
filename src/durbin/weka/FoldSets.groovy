package durbin.weka;

import weka.core.*
import weka.filters.unsupervised.attribute.RemoveType
import weka.classifiers.*
import weka.classifiers.evaluation.*;
import weka.classifiers.meta.FilteredClassifier;
import java.util.Random

import durbin.weka.WekaAdditions


class FoldSets extends ArrayList{
	def samples;
	
	def valueSet = [] as Set
	
	static{
		WekaAdditions.enable();
	}	
	
	def FoldSets(){}
		
	def FoldSets(fileName){
		read(fileName)
	}
	
	def countFolds(){
				
		for(int i = 0;i < this.size();i++){
			def currentSet = this[i]			
			for(int j = 0;j < currentSet.size();j++){
				valueSet.add(currentSet[j])
			}
		}		
		int numFolds = valueSet.size() * this.size();		
		return(numFolds);
	}
	
	def read(fileName){
		new File(fileName).withReader{r->
			def headings = r.readLine().split("\t")[1..-1]
			samples = headings
			//err.println "headings: "+headings
			r.splitEachLine("\t"){fields->
				def foldValues = fields[1..-1].collect{it as int}
				this.add(foldValues)
			}
		}
		return(this)
	}

	def removeMissing(Instances data){		
		
		def dataSamples = data.attributeValues("ID") as Set
		
		// Make copy of old list...
		def oldList = new FoldSets()
		this.each{oldList<<it}
				
		// clear this for new list...
		this.clear()
		for(int i = 0;i < oldList.size();i++){
			def currentSet = oldList[i]
			def newset = []
			for(int j = 0;j < currentSet.size();j++){
				if (dataSamples.contains(samples[j])){
					newset << currentSet[j]
				}
			}
			this << newset; // Add newsets to this...
		}
		return(this)			
	}
			
}