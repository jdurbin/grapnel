
package durbin.weka;

import weka.core.*;
import java.io.*;

/***
* Attempt to speed up saveDataFromInstances in wekaMine, which is fast enough on 
* my Mac, but dog slow on tcga1.
*/ 
public class SaveTab{
	
	/***
  *  Return a vector of the values for a named attribute. 
  */
	static FastVector attributeValues(Instances instances,Attribute attribute){
			
		FastVector rvals = new FastVector();

    if (attribute.isNumeric()){
			double[] rdouble = instances.attributeToDoubleArray(attribute.index());
			for(int i = 0;i < rdouble.length;i++){
				rvals.addElement(rdouble[i]);
			}
    }else if (attribute.isNominal()){
			for(int i = 0;i < instances.numInstances();i++){
				Instance instance = instances.instance(i);
        String value = instance.toString(attribute);
        rvals.addElement(value);
			}
    }else if (attribute.isString()){
			for(int i = 0;i < instances.numInstances();i++){		
        Instance instance = instances.instance(i);
        String value = instance.stringValue(attribute); 
        rvals.addElement(value);
      }
    }
    return(rvals);      
  }

	/***
	* takes a set of instances and creates a tab file from them...
	*/ 
	static void saveDataFromInstances(String fileName,Instances instances) throws Exception{
				
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
		
		
		// Write header...
		Attribute IDAttribute = instances.attribute("ID");
		FastVector instNames = attributeValues(instances,IDAttribute);		
		pw.write("Features\t");
		for(int i = 0;i < instNames.size()-1;i++){
			pw.print(instNames.elementAt(i));
			pw.print(",");
		}
		pw.print(instNames.elementAt(instNames.size()-1));
		pw.print("\n");
		
		System.err.println("BOB");
		
		int classIdx = instances.classIndex();
		for(int i = 0;i < instances.numAttributes();i++){
			if ((i != 0) && (i != classIdx)){
				String attName = instances.attribute(i).name();
				Attribute attribute = instances.attribute(i);						
			
				FastVector atValues = attributeValues(instances,attribute);
				pw.print(attName);
				pw.print(",");
				for(int v = 0;v < atValues.size()-1;v++){
					pw.print(atValues.elementAt(v));
					pw.print(",");
				}
				pw.print(atValues.elementAt(atValues.size()-1));
				pw.print("\n");
			}
		}
	}
}
			