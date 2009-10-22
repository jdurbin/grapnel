package durbin.weka;

import java.util.*;

import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.Remove;

/**************************************************************
* Utilities to help with manipulation of Instances, especially going 
* back and forth between instance/attribute names and indices. 
*/
public class InstanceUtils {

	/***********************************
	*  Takes a collection of attribute names and returns a Set of the corresponding attribute
	*  indices for those names.
	*/
	public static Set getAttributeIndices(Instances inst, Collection<String> attributes) {
		Set<Integer> indexSet = new HashSet<Integer>();
for (String name : attributes) {
			int attrIdx = inst.attribute(name).index();
			indexSet.add(attrIdx);
		}
		return(indexSet);
	}

	/***********************************
	 *  Takes a collection of attribute names and returns an array of the corresponding
	 *  attribute indices for those names.
	 */
	public static int[] getAttributeIndexArray(Instances inst, Collection<String> attributes) {
		int [] indexArray = new int[attributes.size()];
		int i = 0;
for (String name : attributes) {
			int attrIdx = inst.attribute(name).index();
			indexArray[i] = attrIdx;
			i++;
		}
		return(indexArray);
	}


	/***********************************************
	* Merges the attributes from first with the attributes in second
	*
	* @param first Instances to be merged
	* @param second Instances to be merged
	*
	*/
	public static Instances mergeAttributes(Instances first,Instances second) {
		return(Instances.mergeInstances(first,second));
	}
	
	
	/*
	public static Set<String> getInstanceNames(Instances data){
	  HashSet<String> = new HashSet<String>()
	  for(int instIdx = 0;instIdx < data.numInstances();instIdx++){
      Instance instance = data.instance(instIdx);
      name = instance.
    }
	}
	*/
	
	/************************************************************
	*  Make a map between instance names and instance indices...
  * (Maybe I'm missing where this is part of the API already?)
  * 
  * <b>REQUIRED:</b> Instances MUST have an attribute named "ID" 
  * that contains the instance name. 
  */ 
  public static Map<String,Integer> createInstanceNameMap(Instances data){
    Attribute id = data.attribute("ID");
    Map<String,Integer> instanceNameMap = new HashMap<String,Integer>();
    for(int instIdx = 0;instIdx < data.numInstances();instIdx++){
      Instance instance = data.instance(instIdx);
      String value = instance.stringValue(id);
      //System.err.println("instanceNameMap.put: "+value+" "+instIdx);
      instanceNameMap.put(value,instIdx);
    }
    return(instanceNameMap);
  }
	
	
	/**********************************************
	* Removes instances whose nominal/string attribute, specified by attributeName, 
	* equals a specific attributeValue. 
	* 
	* <b>REQUIRED: </b> Instances MUST have an attribute named "ID" that 
	* contains the instance name.
	* 
	* <i>Note: inefficient implementation.  Might have to rework for millions of instances, 
	* but should be fine for hundreds or a few thousand.  </i>
	*/
	public static Instances removeNamedInstances(Instances data,Collection<String> names){
	  Map<String,Integer> name2idx = createInstanceNameMap(data);	    
	  for(String name : names){
	    if (name2idx.containsKey(name)){
	      int idx = name2idx.get(name);
	      data.delete(idx);
	      name2idx = createInstanceNameMap(data);
      }else{
        System.err.println("No instance named: "+name);
      }
	  }	
	  return(data);    	     
	}
	
	/***********************************************************
	*  Takes a list of attribute names and converts them into a string representation
	*  of a list of attribute indices. 
	*/
	public static String attributeNames2Indices(Instances data,Collection<String> names){
	  StringBuilder rval = new StringBuilder();
	  for(String name: names){
	    Attribute attribute = data.attribute(name);
	    int idx = attribute.index();
	    // +1 because the attribute numbers given externally are 1 based...
	    rval.append(Integer.toString(idx+1)+",");
	  }
	  // Remove specious end ","
    rval = rval.deleteCharAt(rval.length()-1);
    return(rval.toString());
	}
		
	/************************************************************
	* Creates a string representation of the indices of the instances given by names. <br>
	* 
	* <b>REQUIRED: </b> Instances MUST have an attribute named "ID" that 
	* contains the instance name.<br>
	* 
	*/ 
	public static String instanceNames2Indices(Instances data,Collection<String> names){
	  Map<String,Integer> name2idx = createInstanceNameMap(data);
	  StringBuilder rval = new StringBuilder();
	  for(String name : names){
	    if (name2idx.containsKey(name)){
	      int idx = name2idx.get(name);
	      // +1 because the attribute numbers given externally are 1 based...
        rval.append(Integer.toString(idx+1)+",");
      }else{
        System.err.println("No instance named: "+name);
      }
    }
    // Remove specious end ","
    rval = rval.deleteCharAt(rval.length()-1);
    return(rval.toString());	  
	}
	

	/**********************************************
	* Merges the attributes from first with the attributes in second that appear in the 
	* collection of attribute names to include. 
	*
	* @param first Instances to be merged
	* @param second Instances to be merged
	* @param secondAttributes Collection of attribute names to choose from second to merge with first.
	*
	* @return New set of instances merging all attributes from first with selected attributes from second.
	*
	*/
	public static Instances mergeAttributes(Instances first, Instances second,
	                                        Collection<String> secondAttributes) throws Exception {

		if (first.numInstances() != second.numInstances()) {
			throw new IllegalArgumentException("Instance sets must be of the same size");
		}				

		// Get the set of indices to remove..
		int[] secondAttributeIndices = getAttributeIndexArray(second,secondAttributes);

		// Remove the attributes NOT in indexSet from second..
		Remove remove = new Remove();
		remove.setAttributeIndicesArray(secondAttributeIndices);

		// We want to remove the attributes that are NOT in the attribute list...
		remove.setInvertSelection(true);
		remove.setInputFormat(second);
		Instances secondNew = Filter.useFilter(second, remove);


		return(Instances.mergeInstances(first,secondNew));
	}
}

