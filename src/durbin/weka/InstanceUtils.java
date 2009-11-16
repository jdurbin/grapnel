package durbin.weka;

import java.util.*;

import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.Remove;

// KJD:  Perhaps I should make my own Instances class that has this functionality? 
// Of course, when I get Instances back from some weka function, it won't come with this...


/**************************************************************
* Utilities to help with manipulation of Instances, especially going 
* back and forth between instance/attribute names and indices. 
*/
public class InstanceUtils {

  
  /***********************************
	 *  Takes a collection of attribute names and returns an array of the corresponding
	 *  attribute indices for those names.
	 */
	public static int[] getAttributeIndexArray(Instances inst, Collection<String> attributes) {
		int [] indexArray = new int[attributes.size()];
		int i = 0;
for (String name : attributes) {
			int attrIdx = inst.attribute(name).index();
			
			//System.err.println("attrIdx: "+attrIdx+" i: "+i+" name: "+name);
			
			indexArray[i] = attrIdx;
			i++;
		}
		return(indexArray);
	}

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
	
	/***********************************************
	*  Return the attributes as a set of strings. 
	*
	*/	
  public static Set<String> getAttributeNames(Instances data){
    Set<String> rval = new HashSet<String>();
    for(int i = 0;i < data.numAttributes();i++){
      Attribute a = data.attribute(i);
      String attrName = a.name();
      rval.add(attrName);
    }
    return(rval);
  }
  
  /*****************************************
  * Return a list of instance names. 
  */ 
  public static List<String> getInstanceNames(Instances data){
    List<String> rList = new ArrayList<String>();
    Attribute id = data.attribute("ID");
    for(int instIdx = 0;instIdx < data.numInstances();instIdx++){
      Instance instance = data.instance(instIdx);
      String value = instance.stringValue(id);
      rList.add(value);
    }
    return(rList);    
  }
	
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
	*  of a list of attribute indices sutible for input to filters and the such. 
	*/
	public static String attributeNames2Indices(Instances data,Collection<String> names){
	  List<Integer> indices = new ArrayList<Integer>();
	  for(String name: names){
	    Attribute attribute = data.attribute(name);
	    int idx = attribute.index();

	    // +1 because the attribute numbers given externally are 1 based...	    
	    indices.add(idx+1);
	  }
	  Collections.sort(indices);

	  StringBuilder rval = new StringBuilder();	
	  for(int i = 0;i < indices.size()-1;i++){
	    rval.append(Integer.toString(indices.get(i))+",");
	  }
	  rval.append(Integer.toString(indices.get(indices.size()-1)));
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
	* KJD: The problem with this approach is that it assumes that the instances in first and second
	* are in the same order.  However, as the files come to me, the clinical instances are completely 
	* jumbled with respect to the original data, so I need to merge them by picking out clinical 
	* values. 
	*/
	public static Instances mergeInstances(Instances first, Instances second,
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
		
	/********************************************************
	* Like Instances.mergeInstances, but uses named ID's to merge two sets of 
	* Instances that may not have the same number of elements or the elements in 
	* the same order. 
	* 
	* Merges two sets of named Instances together (i.e. instances have ID field) 
	* The resulting set will have all the attributes of the first set plus all 
	* the attributes of the second set. With the exception of the required "ID"
	* attribute, attributes of the two sets of instances are assumed to be non-overlapping. 
	* Only instances that occur in both sets will be returned, all other instances 
	* will be omitted. 
	*
	* @param first the first set of Instances
	* @param second the second set of Instances
	* @return the merged set of Instances
	* @throws IllegalArgumentException if the datasets are not the same size
	*/
	public static Instances mergeNamedInstances(Instances first, Instances second) {
                        
    // Create the vector of merged attributes
  	FastVector newAttributes = new FastVector();
  	for (int i = 0; i < first.numAttributes(); i++) {
  	  newAttributes.addElement(first.attribute(i));
  	}
  	for (int i = 0; i < second.numAttributes(); i++) {
  		  newAttributes.addElement(second.attribute(i));
  	}
  	  	  	
  	// Create the set of Instances  
    Map<String,Integer> firstName2InstanceIdxMap = InstanceUtils.createInstanceNameMap(first);   		
  	Map<String,Integer> secondName2InstanceIdxMap = InstanceUtils.createInstanceNameMap(second); 	                                  		                                  
  	
    // Find the set of instances they have in common...
    Set<String> commonInstanceNames = firstName2InstanceIdxMap.keySet();        
    Set<String> secondNames = secondName2InstanceIdxMap.keySet();
    commonInstanceNames.retainAll(secondNames);
    
  	int maxInstances = first.numInstances()+second.numInstances();  		                                    
    Instances merged = new Instances(first.relationName() + '_'
  		                              + second.relationName(),
  		                                newAttributes,maxInstances);
  		                                    
    for(String name: commonInstanceNames){
      int firstIdx = firstName2InstanceIdxMap.get(name);
      int secondIdx = secondName2InstanceIdxMap.get(name);
      
      merged.add(first.instance(firstIdx).mergeInstance(second.instance(secondIdx)));
    }
    
    // OK, now we have two copies of ID's.  Some code expects ID to be at 0, so 
    // remove the duplicate not at index 0. 
    int removeIDIdx = -1;
    for(int i = 0;i < merged.numAttributes();i++){
      String attrName = merged.attribute(i).name();
      if (attrName.matches("ID") && (i != 0)){      
        removeIDIdx = i;
        break;
      }
    }            
    merged.deleteAttributeAt(removeIDIdx);
    return(merged);    
	}
}

