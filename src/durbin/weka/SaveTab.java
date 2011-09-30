// Test
package durbin.weka;

import weka.core.*;
import java.io.*;

/***
* Attempt to speed up saveDataFromInstances in wekaMine, which is fast enough on 
* my Mac, but dog slow on tcga1.
* 
* It worked... so well I have to wonder if perhaps tcga1 was just loaded when I did 
* my original tests...
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
			pw.print("\t");
		}
		pw.print(instNames.elementAt(instNames.size()-1));
		pw.print("\n");
		
		int classIdx = instances.classIndex();
		for(int i = 0;i < instances.numAttributes();i++){
			if ((i != 0) && (i != classIdx)){
				String attName = instances.attribute(i).name();
				Attribute attribute = instances.attribute(i);						
			
				FastVector atValues = attributeValues(instances,attribute);
				pw.print(attName);
				pw.print("\t");
				for(int v = 0;v < atValues.size()-1;v++){
					pw.print(atValues.elementAt(v));
					pw.print("\t");
				}
				pw.print(atValues.elementAt(atValues.size()-1));
				pw.print("\n");
			}
		}
		pw.close();
	}
	
}

/**
Fast version:
real	0m12.939s
user	0m24.017s
sys	0m1.428s

james@tcga1 /inside/grotto/wekaMineExample  $ emacs src/wekaMine/wekaMineFilter 
james@tcga1 /inside/grotto/wekaMineExample  $ time wekaMineFilter -d data/vijver2002.tab -o filtered/vijver2002.normalized.tab -E 'weka.filters.unsupervised.attribute.Normalize' 


Old version:

real	0m17.624s
user	0m30.419s
sys	0m1.565s

So my original 1-2 minute times must have been some load issue on tcga1. 

*/ 