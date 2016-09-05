package durbin.weka;

import durbin.util.*

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
}