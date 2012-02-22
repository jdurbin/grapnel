package durbin.weka;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import com.google.common.collect.*;
import com.google.common.collect.HashMultimap;

class WekaMineConfigSlurperUtils{
	
	// Matches a keyword beginning with $ and ending with whitespace. 
  //static def nextKeyword = ~/\$(\w+)/

	static Pattern nextKeyword = Pattern.compile("\\$(\\w+)");
	
	/**
  * Recursively expand dollar sign substitutions until all expansions are complete. 
  * Returns a list of completely expanded items.  Note: it is up to user to ensure that
  * configuration file is structured so that dependencies come before the thing that
  * depends on them. 
  */ 
  static ArrayList<String> expandDollarSign(HashMultimap<String,String> substitutionMap,
																						ArrayList<String> list){

    boolean bAnyNewMatches = false;

		ArrayList<String> newlist = new ArrayList<String>();
		
		for(String item : list){
			
			//System.err.println("item: "+item);
			
			Matcher m = nextKeyword.matcher(item);
			if (m.find()){
				
				//System.err.println("\tmatches");
				
				String keyword = m.group(1);
				bAnyNewMatches = true;
				Set<String> keywordExpansions = substitutionMap.get(keyword);
				for(String value : keywordExpansions){
					String replaceVal = "\\$"+keyword;
					String newItem = item.replaceFirst(replaceVal,value);
					//System.err.println("\tAdd new item: "+newItem);
					newlist.add(newItem);
				}
			}else{
				//System.err.println("No match found in: "+item);
			}
		}
		
		//System.err.println("bAnyNewMatches: "+bAnyNewMatches);

    // we found some unexpanded keywords, maybe there are more, so search for 
    // them...
    if (bAnyNewMatches){
			//System.err.println("Recurse... newlist.size="+newlist.size());
      newlist = WekaMineConfigSlurperUtils.expandDollarSign(substitutionMap,newlist);
			return(newlist);
    }else{  
			//System.err.println("END Recurse... list.size="+list.size());
      // otherwise, recursion is over, return the list we have
      return(list);
    }
  }
}