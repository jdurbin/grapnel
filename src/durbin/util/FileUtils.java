
package durbin.util;

import java.util.*;
import java.io.*;
import java.lang.*;

/**************************************************************************************
*  Utility methods to help with common file related things.
*
*/
public class FileUtils {
  
  /**************************************
  *  Gets the first line of a file. 
  *  Sort of silly, but I do this so often I wanted to have a shortcut. 
  */ 
  public static String firstLine(String fileName) throws Exception{
    BufferedReader in = new BufferedReader(new FileReader(fileName));
    String rval = in.readLine();
    in.close();
    return(rval);
  }


	/***************************************
	* Determines the number of lines in a file... like wc
	*/
	public static int fastCountLines(String fileName) throws Exception {
		InputStream bis = new BufferedInputStream(new FileInputStream(fileName));
		int numRows = fastCountLines(bis);
		bis.close();
		return(numRows);
	}

	/*****************************************
	* An optimized function to quickly count the number of lines remaining
	* in the given input stream
	*/
	public static int fastCountLines(InputStream is) throws IOException {
		byte[] c = new byte[1024];
		int count = 0;
		int readChars = 0;
		boolean lastCR=true;
		while ((readChars = is.read(c)) != -1) {
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n') ++count;
			}
			// If the last thing we read was a CR, note the fact...
			if (c[(readChars-1)] == '\n') lastCR = true;
			else lastCR = false; 
		}
		
		// If the very last thing we read wasn't a CR, then the last line doesn't
		// end in a CR and we've undercounted the lines by one...
		if (!lastCR) count++;
		
		return count;
	}
}