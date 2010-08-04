
package durbin.cyto;


import java.util.*;
import java.io.*;
import java.lang.*;

import  durbin.stat.KolmogorovSmirnov;

// COLT efficient stat classes.  http://acs.lbl.gov/software/colt/
import  hep.aida.bin.StaticBin1D;
import  hep.aida.ref.Histogram1D;

class DataUtils{
  
  // PlateMap special column names
   public static String WellHeadingCol = "Well";
   public static String CompoundHeadingCol = "MoleculeID";
   public static String ConcentrationHeadingCol = "Concentration";
   // Special column names in data file...
   // Note that DataWellHeadingCol maps to WellHeadingCol
   public static String DataWellHeadingCol = "WellName";  
  
   /***
   * This is a little trick because Java doesn't do switches on strings (WTF?)
   * So we switch on Measures.value("ksdist") or whatever. 
   */ 
   public enum Measures
   {
       meandiff,ksdist,ksprob,ksscaled,histdiff,logdiff,NOVALUE;
       public static Measures value(String str)
       {
           try {
             return valueOf(str);
           } 
           catch (Exception ex) {
               return NOVALUE;
           }
       }   
   }  
  
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
        //if (featureName.contains("CellScoringProfileMultiWaveScoring")){
        //  System.err.println("CellScoringProfileMultiWaveScoring featureValue: "+featureValue);
        //}
        
        //System.err.println("i: "+(i-1)+" featureName: "+featureName+" featureValue: "+featureValue);
        
        // KJD: All non-numeric entries in the TransFlour pipeline output are in "", so I could
        // use that as a simple test for numeric/non-numeric...        
        
        if (skipColumns.contains(featureName)) continue;
        
        if (featureValue.length() == 0) continue;
        if (featureValue == null) continue; 
        if (featureValue.startsWith("\"")) continue;          
        
        // Make sure that what is left can be parsed as double
        // Seems that a lot of strange things make it into the data files...
        // NOPE, Scanner KILLS performance. 
        //Scanner scanVal = new Scanner(featureValue);
        //if (scanVal.hasNextDouble()){                
        try{            
          if (featureBins.keySet().contains(featureName)){
            featureBins.get(featureName).add(Double.parseDouble(featureValue)); 
          }else{
            StaticBin1D bin = new StaticBin1D();
            bin.add(Double.parseDouble(featureValue));
            featureBins.put(featureName,bin);                    
          } 
        }catch(NumberFormatException e){
          // Some kind of non-parsable string, so skip it. 
          continue;
        }
        //}else{
          
          //continue;       
        //}
      }            
    }
    return(featureBins);
  }
    
 
  /**
  * Create a histogram for each Compound by Molarity pair 
  */
  public static HashMap<String,Histogram1D>getCpMByFeatureMap(String dataFileName,                
                int numBins,
                HashMap<String,StaticBin1D> featureBins,
                HashMap<String,String> well2compound,
                HashMap<String,String> well2molarity,
                Set<String> skipColumns,HashSet<String> CpMSet) throws Exception{
                              
    // Go through the file a second time, this time creating histograms 
    // of each feature vs compound+molarity...
    
    BufferedReader reader = new BufferedReader(new FileReader(dataFileName));
 		String line = reader.readLine();
 		String[] headings = line.split(",",-1); // -1 to include empty cols.
    
    HashMap<String,Integer> headings2ColMap = makeColMap(headings);
    int wellCol = headings2ColMap.get(DataWellHeadingCol);
    //System.err.println("wellCol: "+wellCol);

    HashMap<String,Histogram1D> CpMByFeature = new HashMap<String,Histogram1D>();
    
    int lineCount = 0;
    while ((line = reader.readLine()) != null) {
      String[] fields = line.split(",",-1);
    
      lineCount++;
      if ((lineCount % 10000) == 0) System.err.println("Processing line "+lineCount);

      String well = fields[wellCol];
      
      // Fragile, should probably do replace with regex...
      if (well.length() <= 2){
        System.err.println("getCpMByFeatureMap ERROR:  Empty well name: "+well);
        System.err.println("offending Line: "+line);
        return(null);
      }
      
      well = well.substring(1,well.length()-1); // Remove quotes...
      String compound = well2compound.get(well);
      
      //System.err.println("well= "+well+" compound = "+compound);

      // Sometimes Blank is listed as a compound, sometimes Blank wells just
      // don't appear in plate map...
      String molarity = "udef";
      if (compound == null) compound = "Blank";
      if (compound == "Blank") molarity = "0";
      else molarity = well2molarity.get(well);
      
      if (molarity == "") molarity = "0";
    
      String CpM = compound+"_"+molarity;
      CpMSet.add(CpM);
      
      //System.err.println("Add CpM:"+CpM);
      //System.err.println("CpM set size: "+CpMSet.size());

      int i = -1;
      for(String featureValue : fields){
        i++;

        if (featureValue == null) continue;        
        if (featureValue.length() == 0) continue;

        String featureName = headings[i];        
        if (skipColumns.contains(featureName)) continue;

        String CpMFeature = CpM+featureName;
        
        //System.err.println("CpMFeature: "+CpMFeature);

        if (CpMByFeature.containsKey(CpMFeature)){  
          //System.err.println("If  featureValue:"+featureValue);      
          Histogram1D hist = CpMByFeature.get(CpMFeature);
          
          if (hist == null){
            System.err.println("cyto.DataUtils Error: hist null for "+CpMFeature);
            continue;
          }
          //else{
          //  System.err.println("hist not null");
          //}
          
          //System.err.println("containsKey fill "+featureName+" with: "+featureValue);
          hist.fill(Double.parseDouble(featureValue));
        }else{
          //System.err.println("else  featureName:"+featureName);  
          StaticBin1D fbin = featureBins.get(featureName);
          if (fbin == null){
            System.err.println("featureBin is null for: "+featureName);
          }
                          
          double featureMin = featureBins.get(featureName).min();  // Get max and min from preprocessing
          double featureMax = featureBins.get(featureName).max();    
          
          if (featureMin == featureMax){
            featureMax = featureMin + (0.5*featureMin);
          }     

          Histogram1D hist = new Histogram1D("$featureName $CpM",numBins,
                                              featureMin,featureMax);
                                                                                            
          if (hist == null){
            System.err.println("cyto.DataUtils Error: hist null for "+CpMFeature);
            continue;
          }          
          //else                                                      
          //System.err.println(featureName+" "+CpM);
          //System.err.println("\t fill: "+featureValue);        
          
          hist.fill(Double.parseDouble(featureValue));
          CpMByFeature.put(CpMFeature,hist);
        }      
      }      
    }
    return(CpMByFeature);
  }

  /***
  * Creates a map from a name to it's index. 
  */ 
  public static HashMap<String,Integer> makeColMap(String[] headings){
    HashMap<String,Integer> map = new HashMap<String,Integer>();
    for(int i = 0;i < headings.length;i++){
      String name = headings[i];
      map.put(name,i);
    }
    return(map);  
  }

  /**
  * Get a summary statistic for the difference between control (blank) and 
  * experimental distributions.  Apologies for the messy interface, this 
  * is a translation of inline groovy code...
  */ 
  public static double getStatisticForCpM(String CpM,
                                          HashMap<String,Histogram1D> CpMByFeature, 
                                          String feature,                                        
                                          int numBins,
                                          double[] bxsmooth,
                                          double[] bxnorm,
                                          String measureType){
    ArrayList<Double> values = new ArrayList<Double>();
    // Get the smoothed histogram for this feature at this compound and molarity. 
    String CpMFeature = CpM+feature;
    Histogram1D experimentalHist = CpMByFeature.get(CpMFeature);
  
    if (experimentalHist == null){
      System.err.println("ERROR: Null distribution at "+CpMFeature);
      return(Double.NaN);
    }
  
    double[] ex = new double[numBins];
    for(int i = 0;i < numBins;i++){
      ex[i] = experimentalHist.binHeight(i);
    }
     
    double[] exsmooth = exponentialSmoothing(ex,0.25);
    double[] exnorm = normalize(exsmooth);
   
    // Pick the measure of the difference between experimental and control 
    // distributions to use...
    double measure = -1;
//    measure = KolmogorovSmirnov.test(exnorm,bxnorm); 
    
    switch(Measures.value(measureType)){

      case meandiff:
          measure = meandiff(exsmooth,bxsmooth); break;

      case ksdist:
          measure = KolmogorovSmirnov.signedDistance(exsmooth,bxsmooth); break;

      case ksprob:
          // Compute Kolmogorov Smirnov statistic for control vs experiment 
          measure = KolmogorovSmirnov.test(exnorm,bxnorm);           
          break;

      case histdiff:
          double factor = 1; // There are many more control than experimental samples. 
          measure = histSquareDiff(exnorm,bxnorm,factor); break;
          
      case logdiff:
           double factor2 = 1; // There are many more control than experimental samples. 
           measure = histLogDiff(exnorm,bxnorm,factor2); break;
      
      default: 
          measure = -1;
          System.err.println("ERROR: unknown measure type: $measureType");
    } 
    return(measure);
  }


  /**
  * Hist diff...
  *  
  * exsmooth and bxsmooth should be normalized because the 
  * magnitude of bx is so much larger than ex.  Also, I add in 
  * the -2*log of the difference.  The raw sum of the difference
  * is zero (since both bxnorm and exnorm will sum to 1).  
  *
  */ 
  public static double histLogDiff(double[] exsmooth,double[] bxsmooth,double factor){  
    //debuglist = []
    double sum = 0;
    for(int i = 0;i < bxsmooth.length;i++){
      double diff = bxsmooth[i] - (exsmooth[i]*factor);
      diff = -2*(Math.log(diff)/Math.log(2)); // log base 2
      sum += diff;
    }  
    //err.println "diffsum: $sum diff,${debuglist.join(',')}"    
    return(sum);
  }




  /**
  * Hist diff...
  *  
  * exsmooth and bxsmooth should be normalized because the 
  * magnitude of bx is so much larger than ex.  Also, I add in 
  * the square of the difference because the sum of the differences
  * is zero (since both bxnorm and exnorm will sum to 1).  
  *
  */ 
  public static double histSquareDiff(double[] exsmooth,double[] bxsmooth,double factor){  
    //debuglist = []
    double sum = 0;
    for(int i = 0;i < bxsmooth.length;i++){
      double diff = bxsmooth[i] - (exsmooth[i]*factor);
      diff = diff * diff; // square the difference...   
      sum += diff;
    }  
    //err.println "diffsum: $sum diff,${debuglist.join(',')}"    
    return(sum);
  }

  /*******
  * Performs 'exponential smoothing' on the given array. 
  * 
  * x0,x1,x2,x3,x4 input. 
  *
  *   s0 = x0 + alpha*(x1-x0)
  *   s1 = alpha*(x0-x1) + x1 + alpha*(x2-x1)
  * 
  */
  public static double[] exponentialSmoothing(double[] x,double alpha){
    int n = x.length;
    double[] s = new double[n];
    s[0] = x[0] + alpha*(x[1]-x[0]);
    for(int i = 1;i < (n-1);i++){
      s[i] = alpha*(x[i-1]-x[i])+x[i]+alpha*(x[i+1]-x[i]);
    }
    s[n-1] = alpha*(x[n-2]-x[n-1])+x[n-1];
    return(s);
  }

  /**
  * Scales bins to 0-1 scale. 
  *
  */ 
  public static double[] normalize(double[] bins){
    double sum = 0;
    for(int i = 0;i < bins.length;i++){
      sum+=bins[i];
    }
  
    double[] normbins = new double[bins.length];
    for(int i = 0;i < bins.length;i++){
      if (sum == 0) normbins[i] = 0;
      else normbins[i] = bins[i]/sum;
    }
    return(normbins);  
  }


  /**
  * Returns the difference in the means of the two lists. 
  */ 
  public static double meandiff(double[] v1,double[] v2){
    double s1 = 0,c1=0;
    for(int i = 0;i < v1.length;i++){
      s1+=v1[i];
      c1++;    
    }
    double m1 = s1/c1;
  
    double s2 = 0,c2=0;
    for(int i = 0;i < v2.length;i++){
      s2+=v2[i];
      c2++;
    }
    double m2 = s2/c2;
    double diff = m1-m2; 

    // Want to normalize it, so that the values are somewhat comparable..
    double rval = 0.000000001; // Just to prevent ugly divide by zero. 
    if (m2 != 0) rval = diff/m2;
    return(rval);
  }
}

/*
computeFeatureBins translated from Groovy code below...

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
  