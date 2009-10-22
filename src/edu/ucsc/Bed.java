
package edu.ucsc;

import java.util.*;
import java.io.*;
import java.util.zip.*;

import groovy.lang.Closure;

/****************************************************************************************
* A collection of bed entries and algorithms to operate on them...
*
*/
public class Bed extends ArrayList<BedItem> {

	public Bed(String fileName) throws Exception {
	  //this.reserve(6000000);
		readFile(fileName);
	}
	
	
	/********************************
	* Writes a coords file corresponding to this bed file. 
	*
	* Convenience uses default options. 
	*/
	public void writeCoords(String fileName) throws Exception{
	  int bedIdx = fileName.lastIndexOf(".bed");
	  String bedName = fileName.substring(0,bedIdx);
	  String bedOptions = "track name="+bedName;
	  
	  writeCoords(fileName,bedOptions); 
	}
	
	/********************************
	* Writes a bed graph file corresponding to this bed file. 
	* 
	* Bed graph options include:
	* 
	* track type=coords name=track_label description=center_label
  *        visibility=display_mode color=r,g,b altColor=r,g,b
  *        priority=priority autoScale=on|off
  *        gridDefault=on|off maxHeightPixels=max:default:min
  *        graphType=bar|points viewLimits=lower:upper
  *        yLineMark=real-value yLineOnOff=on|off
  *        windowingFunction=maximum|mean|minimum smoothingWindow=off|2-16
	*/
	public void writeCoords(String fileName,String bedOptions) throws Exception{
	  BufferedWriter out = new BufferedWriter(new FileWriter(fileName));	  
	  out.write(bedOptions+"\n");
	  for (BedItem b : this) {
	    out.write(b.toCoords()+"\n"); 
	  }	  
	  out.close();
	}
	
	
	/********************************
	* Writes a bed graph file corresponding to this bed file. 
	*
	* Convenience uses default options. 
	*/
	public void writeBedGraph(String fileName) throws Exception{
	  int bedIdx = fileName.lastIndexOf(".bed");
	  String bedName = fileName.substring(0,bedIdx);
	  String bedOptions = "track type=bedGraph name="+bedName;
	  
	  writeBedGraph(fileName,bedOptions); 
	}
	
	/********************************
	* Writes a bed graph file corresponding to this bed file. 
	* 
	* Bed graph options include:
	* 
	* track type=bedGraph name=track_label description=center_label
  *        visibility=display_mode color=r,g,b altColor=r,g,b
  *        priority=priority autoScale=on|off
  *        gridDefault=on|off maxHeightPixels=max:default:min
  *        graphType=bar|points viewLimits=lower:upper
  *        yLineMark=real-value yLineOnOff=on|off
  *        windowingFunction=maximum|mean|minimum smoothingWindow=off|2-16
	*/
	public void writeBedGraph(String fileName,String bedOptions) throws Exception{
	  BufferedWriter out = new BufferedWriter(new FileWriter(fileName));	  
	  out.write(bedOptions+"\n");
	  for (BedItem b : this) {
	    out.write(b.toBedGraph()+"\n"); 
	  }	  
	  out.close();
	}


	/*****************************
	* Read a bed graph from a file...
	*/
	public void readFile(String fileName) throws Exception {

		// Compressed or not?
		BufferedReader reader;
		if (fileName.endsWith(".gz")) {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(fileName));
			reader = new BufferedReader(new InputStreamReader(gzip));
		} else {
			reader = new BufferedReader(new FileReader(fileName));
		}

		String line;
		while ((line = reader.readLine()) != null) {
			this.add(new BedItem(line));
		}
	}

  /************************************
  * Groovy closure iterator restricted to a particular
  * chromosome.
  */ 
	public Bed eachOnChrom(String chrom,Closure c) {
		int targetID = BedItem.getChromID(chrom);
    for (BedItem b : this) {
			if (b.chromID == targetID) {
				c.call(b);
			}
		}
		return(this);
	}
}

// Performance:

// 49MB bed.gz file:
// Reading UCSF-UBC.H3K27me3.bed.gz...done.
// Bed size: 3,545,727
// real	0m16.184s

// 482MB bed file:
//time ./bedtest.groovy UCSD.H3K36me3.bed
//Reading UCSD.H3K36me3.bed...done.
//Bed size: 8,605,199
// real	0m42.805s

//Second run:
//time ./bedtest.groovy UCSD.H3K36me3.bed
//Reading UCSD.H3K36me3.bed...done.
//Bed size: 8605199
//real	0m33.871s
//
// Third run:
// real	0m31.172s

// time ./testscripts/bedtest.groovy full/BI.H3K4me3.bed 
// Reading full/BI.H3K4me3.bed...done.
// Bed size: 6655339
// real	0m26.612s


// 113MB bed.gz file:
// time ./bedtest.groovy UCSD.H3K36me3.bed.gz
// Reading UCSD.H3K36me3.bed.gz...done.
// Bed size: 8605199
// real	0m38.287s

// Kent's function, bedLoadAll, is faster than 
// either my C++ version or my Java version...
//bedLoadAll(argv[1]);
//time bedSort full/BI.H3K4me3.bed
//real	0m12.106s
//
// bedLoadNAll(argv[1],3) is even faster:
// 0m5.711s
// 
// This is more apples-to-oranges since Kent's bed graph
// is represented as a linked list.  Don't know if that'd
// matter, hard to see how it would unless dynamic allocation
// for Vector.add() is slow. 




