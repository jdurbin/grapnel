
package durbin.cyto;

import java.util.*;
import  hep.aida.bin.StaticBin1D;


/**
* 
*/
class Utils{
  
  
  
  /**
  * Wasn't any faster than Groovy native version...
  */ 
  public static addIfContains(HashMap h,String name,String svalue){
    
    double value = (double) Double.valueOf(svalue);
    
    if (h.keySet().contains(name)){
      h.get(name).add(value);
    }else{
      StaticBin1D bin = new StaticBin1D();
      bin.add(value)
      h.put(name,bin);
    }
  }
}