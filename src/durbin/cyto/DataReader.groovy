#!/usr/bin/env groovy 

package durbin.cyto;

import durbin.util.*;

public class DataReader{
  
  /**
  * Merge the platemap.csv file with the data.tab file and return a 
  * 3D map of data[feature][compound][titration]
  * 
  * The platemap file is a csv file with the following format:
  * 
  * ExperimentName,WellName,CompoundName,CompoundMolarity,other stuff
  *
  * The data file is a tab file with the following format:
  *
  * Feature1,Feature2,...,WellName,WellX,WellY
  *
  * The WellName is used to link these two tables and to fill out
  * a 3D map of values for each feature, compound, and titration. 
  *
  */
  public static mergePlateMapAndData(String plateMapFile,String dataFile){
    
    // Read in the platemap file...
    def well2InfoMap = [:]
    new File(plateMapFile).withReader{r->
      def headings = r.readLine().split(",")
      r.splitEachLine(","){fields->
        well2InfoMap[fields[1]] = [fields[2],fields[3]]
      }
    }


    // Read in the data file.  The data file is basically a 3D 
    // vector of features x compounds x titrations.  Well 4D, 4th dimension is 
    // the replicate, since experiments are often replicated multiple times...
    def data = new MultidimensionalMap()
    def name2ColumnMap = [:]
    def col2NameMap = [:]
    new File(dataFile).withReader{r->

      // Read in and clean up the headings, which come full of special characters...
      def headings = r.readLine().split("\t")
      headings = headings*.replaceAll("\"","") // Remove quotes
      headings = headings*.replaceAll(/\s/,"") // Remove spaces
      headings = headings*.replaceAll(/\(/,"") // Remove left braces
      headings = headings*.replaceAll(/\)/,"") // Remove right braces
      headings = headings*.replaceAll("%","Pct") // Remove % signs. 
  
      // Create a map to look up column from heading...
      headings.eachWithIndex{name,idx-> name2ColumnMap[name] = idx}
      headings.eachWithIndex{name,idx-> col2NameMap[idx] = name}
  
      // Go through each line of data file, match up the well ID with the 
      // compound and titration, then save the value for each feature 
      // in the 3D map...
      r.splitEachLine("\t"){fields->
        def wellCol = name2ColumnMap['WellName']
        def wellID = fields[wellCol].replaceAll("\"","")             
        def compoundName = well2InfoMap[wellID][0]
        def titration = well2InfoMap[wellID][1]
    
        fields.eachWithIndex{value,col->
          def featureID = col2NameMap[col]
      
          if (value == "") value = "null"      
          // Skip per cell features (which is this processed file are all null)
          if (!featureID.contains("Cell:")){

            // First time for this triple? Then replicate is 1, otherwise
            // increment replicate...
            if (data[featureID][compoundName][titration].keySet().size() ==0){
              def replicate = 1;
              data[featureID][compoundName][titration][replicate]=value;             
            }else{
              def replicate = data[featureID][compoundName][titration].keySet().max() + 1;
              data[featureID][compoundName][titration][replicate]=value;                           
            }
          }
        }
      }
    }
    return(data)
  } 
}