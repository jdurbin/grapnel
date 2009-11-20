import weka.core.*
import weka.core.converters.ConverterUtils.DataSource


/**********************************************************
* Expando meta-class additions to weka classes.  This is mostly 
* sugar for functionality that is found in InstanceUtils.java, etc. 
* 
*/
public class WekaAdditions{
  static enable(){  
    
    /********************************************
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
        (0..<numInstances()).each{idx->
          Instance instance = instance(idx)
          def value = instance.stringValue(attribute) 
          rvals.add(value)
        }
      }
      return(rvals)      
    }
    
    
    /*************************************
    * Set the class based on a name. 
    * Returns the index of the named class. 
    */     
    Instances.metaClass.setClassName << {attributeName->
      Attribute attr = attribute(attributeName)      
      def idx = attr.index()
      setClassIndex(idx) // Zero based ..
      return(idx)
    }
        
    /***************************************
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
    
    /*********************************
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