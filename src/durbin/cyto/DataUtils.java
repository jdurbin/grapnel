
package durbin.cyto;


import java.util.*;
import java.io.*;
import java.lang.*;

// COLT efficient stat classes.  http://acs.lbl.gov/software/colt/
import  hep.aida.bin.StaticBin1D;
import  hep.aida.ref.Histogram1D;

class DataUtils{
  
  
  
  /**
  * Scans a data file to determine the min/max of each feature.   Actually it creates
  * and returns a StaticBin1D object for each feature, and a variety of simple statistics
  * for each feature can be extracted from that.  
  * 
  * skipColumns are the names of columns we want to skip because they are calibration 
  * values or other non-informative features. 
  */ 
  public static HashMap<String,StaticBin1D> computeFeatureBins(String dataFileName,
          Set<String> skipColumns) throws Exception{

    HashMap<String,StaticBin1D> featureBins = new HashMap<String,StaticBin1D>();
    
    BufferedReader reader = new BufferedReader(new FileReader(dataFileName));
		String line = reader.readLine();
		String[] headings = line.split(",",-1); // -1 to include empty cols.

    int lineCount = 0;
		while ((line = reader.readLine()) != null) {
      String[] fields = line.split(",",-1);
      
      lineCount++;
      if ((lineCount % 10000)==0) System.err.println("Preprocessing line "+lineCount);
      
      int i = 0;
      for(String featureValue: fields){
      
        String featureName = headings[i++];
        
        //System.err.println("i: "+(i-1)+" featureName: "+featureName+" featureValue: "+featureValue);
        
        // KJD: All non-numeric entries in the TransFlour pipeline output are in "", so I could
        // use that as a simple test for numeric/non-numeric...        
        
        if (skipColumns.contains(featureName)) continue;
        if (featureValue.length() == 0) continue;
        if (featureValue == null) continue;            
            
        if (featureBins.keySet().contains(featureName)){
         featureBins.get(featureName).add(Double.parseDouble(featureValue)); 
        }else{
          StaticBin1D bin = new StaticBin1D();
          bin.add(Double.parseDouble(featureValue));
          featureBins.put(featureName,bin);                    
        }        
      }            
    }
    return(featureBins);
  }



/*
Above translated from Groovy code below...

  def computeFeatureBins(datafile){
    def headings;
    lineCount = 0
    featureBins = [:]
    new File(datafile).withReader{r->
      headings = r.readLine().split(",")
      r.splitEachLine(","){fields->

        lineCount++
        if (!(lineCount % 10000)) println "Preprocessing line $lineCount"

        fields.eachWithIndex{featureValue,i->

          if (skipColumns.contains(headings[i])) return;

          if (featureValue == "") return;
          if (!featureValue.isDouble()){
            println "$i ${headings[i]}=[$featureValue]"
            return
          }

          featureName = headings[i]   
          if (featureBins.keySet().contains(featureName)){
            featureBins[featureName].add(featureValue as Double)
          }else{
            featureBins[featureName] = new StaticBin1D();
            featureBins[featureName].add(featureValue as Double)
          }
        }
      }
    }
    return(featureBins)
  }


  */  
  
  
  
  
  
  
  
  
  
}