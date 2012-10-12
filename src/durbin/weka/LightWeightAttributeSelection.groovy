
package durbin.weka;

import weka.attributeSelection.*
import weka.core.*

/***
* The AttributeSelection class is fairly heavy weight, saving lots more information than we need. This saves
* just the selected attributes and their weights.   
*/ 
public class LightWeightAttributeSelection{
	def selectedAttributes = [:]
	
	def LightWeightAttributeSelection(Instances data,AttributeSelection attributeSelection,ASSearch search){
				
		// data is the data as seen by Evaluation2 as a training set.  However, by the time that the
		// attribute selector sees the data, the ID (attribute 0) has been filtered out.  This means that all the 
		// attribute selection indices will be off by one from the indices relative to data.  Attribute 0
		// in data is the ID, but attribute 0 in the selection is attribute 1 in data. 		
				
				
		// Attribute selectors can produce ranked attributes or merely a selection of attributes...
		if (search instanceof weka.attributeSelection.RankedOutputSearch){
			double[][] rankedAttrs = attributeSelection.rankedAttributes();
			
			// Already sorted, but includes all attributes...
			def numAttributes = attributeSelection.numberAttributesSelected() 
			rankedAttrs.eachWithIndex{attrRank,i->
				
				
				
				if (i >= numAttributes) return; // skip all but the requested ones.   								      
        def attIdx = (attrRank[0] as int) +1
        def attName = data.attribute(attIdx).name()
        def score = attrRank[1] as Double
				score = score.round(6)
				selectedAttributes[attName] = score
			}
			
			//System.err.println "DEBUG: rankedAttrs.length=${rankedAttrs.length}  rankedAttrs.size=${selectedAttributes.size()}. selectedAttributes.size=${selAttrs.size()} "
		}else{
			def selectedAttrs = attributeSelection.selectedAttributes() as ArrayList

			//println "data.numAttrs: "+data.numAttributes()
			//println "selectedAttrs:"+selectedAttrs
		  def classAttr = data.classAttribute()
			def classIdx = classAttr.index()
			//println "classAttr: "+classAttr
			//println "classIdx: "+classIdx
			
			// Last selectedAttrs is the classAttribute, which we do not want to report here...
			selectedAttrs[0..-2].each{attrIdx->
			  def attName = data.attribute(attrIdx+1).name()
				selectedAttributes[attName] = -1 // There is no score/rank.  
			}
		}
	}
}

