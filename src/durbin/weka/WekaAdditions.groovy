package durbin.weka;

import weka.core.*
import weka.core.converters.ConverterUtils.DataSource


/***
* Sugar for weka classes.  Mainly to allow one to access attributes and instances by name 
* rather than by index.  <br>
* <br>
* Technically, this is accomplished via Groovy's expando meta-class functionality, 
* which lets you add methods to classes without extending those classes.  In order to add these
* methods to weka classes, you will have to call enable() somewhere in your code, like: 
* <pre>
* WekaAdditions.enable()
* </pre>
* 
* Some extensions to Instances this will add include: <br>
* <ul>
* <li>Instances.attributeValues(attributeName)</li>
* <li>Instances.setClassName(attributeName)</li>
* <li>Instances.instanceValues(instanceName)</li>
* <li>Instances.attributeNames()</li>
* <li>Instances.each{}, and other Iterable sugar</li>
* </ul>
* Each of these methods does the same as the corresponding method in Instances except using
* name instead of index keys. 
* 
*/
public class WekaAdditions{
	
	/***
	* Enables all of the additions to weka in WekaAdditions.  You can stick these bits of code anywhere so long as they 
	* are executed before you try to 
	*/ 
  static enable(){  
	  
		
		/***
		* Return the indexed attribute value
		*/ 
		Instance.metaClass.getAt = {int i->
			Attribute attribute = delegate.attribute(i as int)
			if (attribute.isNumeric()){
				return(delegate.value(i))
			}else{ 
				return(delegate.stringValue(i))
			}
		}

		/***
		* Return the named attribute value.  This along with iterator allows code like:
		*
		* <pre>
		*
		* instances.each{instance->
		*		println instance['ID']
		*	}
		*
		* </pre>
		*/ 
		Instance.metaClass.getAt = {String attributeName->
			def dataset = dataset()
			Attribute attribute = dataset.attribute(attributeName)
			def i = attribute.index()
			if (attribute.isNumeric()){
				return(value(i))
			}else{ 
				return(delegate.stringValue(i))
			}
		}
		
		
			Instance.metaClass.getAt = {int attributeIdx->
				def dataset = dataset()
				Attribute attribute = dataset.attribute(attributeIdx)
				def i = attribute.index()
				if (attribute.isNumeric()){
					return(value(i))
				}else{ 
					return(delegate.stringValue(i))
				}
			}
		
		/***
		* Make instances iterable... 
		*/ 
		Instances.metaClass.iterator = {-> 
			def i = 0 
			return [hasNext: { i < delegate.numInstances() }, next: { delegate.instance(i++) }] as Iterator 
		}
		
		// Enable [] notation for instances.  data[5] returns 5th instance. 
		Instances.metaClass.getAt = {int i-> return(delegate.instance(i))}
		
		// Enable [] notation for instance attributes.  data['price'] returns values for 'price' attribute. 
		Instances.metaClass.getAt = {String attrName->return(delegate.attributeValues(attrName))}
	
	
    /***
    *  Return an array of the values for a named attribute. 
    */ 
    Instances.metaClass.attributeValues << {attributeName-> 
      Attribute attribute = attribute(attributeName)
      def rvals = []
      if (attribute.isNumeric()){
        rvals = attributeToDoubleArray(attribute.index())
      }else if (attribute.isNominal()){
         (0..<numInstances()).each{idx->
            Instance instance = instance(idx)
            def value = instance.toString(attribute)
            rvals.add(value)
          }
      }else if (attribute.isString()){
        (0..< numInstances()).each{idx->
          Instance instance = instance(idx)
          def value = instance.stringValue(attribute) 
          rvals.add(value)
        }
      }
      return(rvals)      
    }    
    
    /***
    * Set the class based on a name. 
    * Returns the index of the named class. 
    */     
    Instances.metaClass.setClassName << {attributeName->
      Attribute attr = attribute(attributeName)   
   
      def idx = attr.index()
      setClassIndex(idx) // Zero based ..
      return(idx)
    }

		Instances.metaClass.className <<{
			Attribute classAttr = attribute(classIndex())
			return(classAttr.name())
		}
		
		
		/***
		* Takes a list of instance names and returns their 
		* correspondign instance indexes.  Assumes ID attribute
		* holds instance names. 
		*/ 
		Instances.metaClass.nameListToIndexList <<{nameList->			
			def name2IdxMap = [:]
			Attribute id = attribute("ID");
			for(idx in 0..<numInstances()){
				Instance inst = instance(idx)
				String instanceName = inst.stringValue(id)
				name2IdxMap[instanceName] = idx;
			}
			
			def rList = []
			for(name in nameList){
				if (name2IdxMap.containsKey(name)){
					rList << name2IdxMap[name]
				}else{
					rList << -1 // No such instance...
				}
			}						
			return(rList)
		}


		/***
		* Takes a list of instance names and returns their 
		* correspondign instance indexes.  Assumes ID attribute
		* holds instance names. Each name can occur only once. 
		* No correspondence is assumed between order in and 
		* order out. 
		*/ 
		Instances.metaClass.nameSetToIndexList <<{nameCollection->
			def rlist = []
			Attribute id = attribute("ID");
			def nameSet = nameCollection as Set // Just in case we're given a list to begin with...
			for(idx in 0..<numInstances()){
				Instance inst = instance(idx)
				String instanceName = inst.stringValue(id)
				if (nameSet.contains(instanceName)) rlist << idx
			}
			return(rlist)
		}

        
    /***
    * Returns all the attribute values for a named instance
    */ 
    Instances.metaClass.instanceValues << {instanceName->
      def rvals = []
      
      // Find the instance with that ID
      Attribute id = attribute("ID");
      def theInstance
      (0..<numInstances()).each{idx->
     
		    Instance inst = instance(idx) 
        String value = inst.stringValue(id)
        if (value == instanceName) theInstance = inst        
      }
          
      (0..<numAttributes()).each{attrIdx->
        Attribute attribute = attribute(attrIdx)
        if (attribute.isNumeric()){
          def value = theInstance.value(attribute)
          rvals.add(value)
        }else if (attribute.isNominal()){
          def value = theInstance.toString(attribute)
          rvals.add(value)
        }else if (attribute.isString()){
          def value = theInstance.stringValue(attribute)
          rvals.add(value)
        }
      }
      return(rvals)
    }    
    
    /***
    * Returns all of the attribute names...
    */
    Instances.metaClass.attributeNames << {
      def rvals = []
      (0..<numAttributes()).each{attrIdx->
        Attribute attr = attribute(attrIdx)
        def name = attr.name()
        rvals.add(name)
      }
      return(rvals)
    } 
    
  }// end enable
}