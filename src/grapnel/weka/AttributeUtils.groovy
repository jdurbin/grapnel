package grapnel.weka;

import grapnel.util.*

import weka.classifiers.meta.FilteredClassifier;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.core.SerializationHelper;
import weka.filters.supervised.attribute.*
import weka.filters.*
import weka.attributeSelection.*
import weka.core.*
import weka.filters.*
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

class AttributeUtils{

	Instances instances;
	Attribute savedAttribute=null;
	boolean bRestoreClass = false;
	double[] savedAttributeValues = null;
	
	/****
	* Renames attributes according to the mapping in nameMap
	*/
	static Instances renameAttributes(Instances data,HashMap nameMap){
		def newOutputFormat = determineRenameOutputFormat(data,nameMap)

		Instances newData = new Instances(newOutputFormat, 0);
		for (int i = 0; i < data.numInstances(); i++){
			newData.add(data.instance(i).copy());
		}
		return(newData);
	}
	
	
	/**
	* 
	*/ 
	static Instances determineRenameOutputFormat(data,old2NewMap){
		
		// Keep track of new names to be sure they are unique
		// append _cpy0 for copy onto any non-unique name
		def cpyIdx = 0;
		def uniqueList = [] as Set				
		def newAtts = new ArrayList<Attribute>();
		for(int i = 0;i < data.numAttributes();i++){									
			def oldAttribute = data.attribute(i)
						
			def newName = old2NewMap[oldAttribute.name()]
			if (newName == null){
				newName = "unknown_${cpyIdx}"
			}			
			
			if (uniqueList.contains(newName)){
				newName = "${newName}_cpy${cpyIdx}"
				cpyIdx++
			}
			uniqueList << newName;
			newAtts.add(oldAttribute.copy(newName))
		}
		def newOutputFormat = new Instances(data.relationName(),newAtts,0)
		return(newOutputFormat)
	}
	
	
	/***
	*	Removes the class attribute from a set of instances and 
	*   saves it to be restored later. 
	*/ 
	Instances removeClassAttribute(Instances withClassInstances){
		if (withClassInstances.classIndex() >= 0){
			def removeIdx = withClassInstances.classIndex()		
			bRestoreClass = true;		
			savedAttribute = withClassInstances.attribute(removeIdx);
			savedAttributeValues = withClassInstances.attributeToDoubleArray(removeIdx);	
			Remove remove = new Remove();			
			remove.setAttributeIndices(Integer.toString(removeIdx+1));
			remove.setInputFormat(withClassInstances);
			instances = Filter.useFilter(withClassInstances, remove);		
		}else{
			instances = withClassInstances
		}
		return(instances)
	}
	
	/**
	*  Restores saved class attribute. 
	*/ 
	Instances restoreClassAttribute(Instances noClassInstances){
		if (bRestoreClass){
			noClassInstances.insertAttributeAt(savedAttribute,noClassInstances.numAttributes());
			noClassInstances.setClass(savedAttribute)
			for(int i = 0;i < noClassInstances.numInstances();i++){
				noClassInstances.instance(i).setClassValue(savedAttributeValues[i]);
			}
		}
		return(noClassInstances)		
	}
	
	/**
	* Remove string attributes.  Specifically the string attribute that contains 
	* the instance ID.  This is done because most attribute evaluators and classifiers
	* can not handle string attributes. 
	*/ 
	static Instances removeInstanceID(instances){
		def filter = new RemoveType()
		filter.setInputFormat(instances);
		def instances_notype = Filter.useFilter(instances,filter)
		return(instances_notype)
	}
}