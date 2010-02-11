package durbin.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.RuntimeException;

/**
* Make some functionality available to Groovy that is common in Perl
* but not implemented in Groovy by default.
*
* Example using to compute complement of DNA sequence:
*
* complement = Perlism.tr("ACTG","TGAC",fwdSeq)
*
* Or for repeated use of translation, can precompile as in this example:
* 
* pattern = Perlism.compileTRPattern("wsatugcyrkmbdhvnATUGCYRKMBDHVN",
*       	                            "WSTAACGRYMKVHDBNTAACGRYMKVHDBN")
*
* complement = Perlism.tr(pattern,fwdSeq)
* 
*/
public class Perlism {

	/********************************************
	* Translate characters, like Perl tr().  Most Groovy-only implementations of TR are slow.
	* This, however, is fast.
	*/
	public static Map<Character,Character> compileTRPattern(String pattern1,String pattern2) {
		Map<Character,Character> m = new HashMap<Character,Character>();
		for (int i = 0;i < pattern1.length();i++) {
			m.put(pattern1.charAt(i),pattern2.charAt(i));
		}
		return(m);
	}


	/********************************************
	* Translate characters, like Perl tr().  Most Groovy-only implementations of TR are slow.
	* This, however, is fast.
	*/
	public static StringBuffer tr(HashMap<Character,Character> pattern,StringBuffer source) {

		for (int i = 0;i < source.length();i++) {
			char c = source.charAt(i);
			if (pattern.containsKey(c)) {
				source.setCharAt(i,pattern.get(c));
			} else {
				source.setCharAt(i,c);
			}
		}
		return(source);
	}

  /**************************************************************************************
  *  A bunch of utility methods to help with common file related things.
  *
  */
	public static StringBuffer tr(String pattern1,String pattern2, StringBuffer source) {

		Map<Character,Character> m = new HashMap<Character,Character>();
		for (int i = 0;i < pattern1.length();i++) {
			m.put(pattern1.charAt(i),pattern2.charAt(i));
		}

		for (int i = 0;i < source.length();i++) {
			char c = source.charAt(i);
			if (m.containsKey(c)) {
				source.setCharAt(i,m.get(c));
			} else {
				source.setCharAt(i,c);
			}
		}
		return(source);
	}

  /**************************************************************************************
  *  A bunch of utility methods to help with common file related things.
  *
  */
	public static StringBuilder tr(HashMap<Character,Character> pattern,StringBuilder source) {

		for (int i = 0;i < source.length();i++) {
			char c = source.charAt(i);
			if (pattern.containsKey(c)) {
				source.setCharAt(i,pattern.get(c));
			} else {
				source.setCharAt(i,c);
			}
		}
		return(source);
	}

  /**************************************************************************************
  *  A bunch of utility methods to help with common file related things.
  *
  */
	public static StringBuilder tr(String pattern1,String pattern2, StringBuilder source) {

		Map<Character,Character> m = new HashMap<Character,Character>();
		for (int i = 0;i < pattern1.length();i++) {
			m.put(pattern1.charAt(i),pattern2.charAt(i));
		}

		for (int i = 0;i < source.length();i++) {
			char c = source.charAt(i);
			if (m.containsKey(c)) {
				source.setCharAt(i,m.get(c));
			} else {
				source.setCharAt(i,c);
			}
		}
		return(source);
	}

}