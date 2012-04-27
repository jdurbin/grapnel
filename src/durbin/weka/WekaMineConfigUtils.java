package durbin.weka;

import java.util.regex.*;
import java.util.*;
import durbin.util.*;
import com.google.common.collect.HashMultimap;
import org.apache.commons.lang3.StringUtils;

class WekaMineConfigUtils{
	
	// Matches a keyword beginning with $ and ending with whitespace. 
	static Pattern nextKeyword = Pattern.compile("\\$(\\w+)");
	
	public static String findKeyword(String item){
		for(int i = 0;i < item.length();i++){
			//System.err.println("\t charAt "+i+" = "+item.charAt(i));
			if (item.charAt(i) == '$'){
				int endIdx = 0;
				
				// Search for a terminating character...
				for(int j = i+1;j< item.length();j++){
					//System.err.println("\t\tj="+j);
					char c = item.charAt(j);
					if ((c == ' ')||(c=='\"')||(c==',')||(c=='\n')){
						endIdx = j;
						//System.err.println("\t\tEND: "+endIdx+" substring: "+item.substring(i+1,endIdx));
						return(item.substring(i+1,endIdx));
					}
				}
				// Or if no terminating character, end of string...
				endIdx = item.length();
				return(item.substring(i+1,endIdx));
			}
		}
		// Something went wrong. 
		return("ERROR: keyword beginning with dollar sign not found in "+item);
	}
	
	
	public static ArrayList<String> expandDollarSign(HashMultimap<String,
																									 String> substitutionMap,ArrayList<String> list){
		boolean bAnyNewMatches = false;
		while(true){
			bAnyNewMatches = false;
	  	ArrayList<String> newlist = new ArrayList<String>();
			for(String item : list){
				//Matcher regexMatcher = nextKeyword.matcher(item);
				//if(regexMatcher.find()){
				if (item.contains("$")){
					bAnyNewMatches = true;
					String keyword = findKeyword(item);
					//System.err.println("keyword: "+keyword+" found in item: "+item);
					//String keyword = regexMatcher.group(1);	      	
	       	Set<String> keywordExpansions = substitutionMap.get(keyword);
					for(String value: keywordExpansions){
						String replaceVal = "\\$"+keyword;
						String newItem = item.replaceFirst(replaceVal,value);
						//System.err.println("newItem: "+newItem);
						//String newItem = regexMatcher.replaceFirst(value);
						//String newItem = StringUtils.replaceOnce(item,replaceVal,value);
						newlist.add(newItem);
	       	}
	      }else{
					//System.err.println("item: "+item+" does not contain dollar sign.");
				}
	    }
			//System.err.println("bAnyNewMatches="+bAnyNewMatches);
	
			// No new matches found, so return the list we have...
			if (!bAnyNewMatches) return(list);

			// otherwise we found some unexpanded keywords, maybe there are more, so continue to 
			// search for them...
			list = newlist;
		}
	}
	
	/**
  * Recursively expand dollar sign substitutions until all expansions are complete. 
  * Returns a list of completely expanded items.  Note: it is up to user to ensure that
  * configuration file is structured so that dependencies come before the thing that
  * depends on them. 
  */ 
  static ArrayList<String> expandDollarSignRecursive(HashMultimap<String,String> substitutionMap,
																						ArrayList<String> list){

    boolean bAnyNewMatches = false;
		ArrayList<String> newlist = new ArrayList<String>();		
		for(String item : list){						
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
      newlist = WekaMineConfigUtils.expandDollarSignRecursive(substitutionMap,newlist);
			return(newlist);
    }else{  
			//System.err.println("END Recurse... list.size="+list.size());
      // otherwise, recursion is over, return the list we have
      return(list);
    }
  }
	
	
}
