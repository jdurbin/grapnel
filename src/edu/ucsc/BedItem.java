package edu.ucsc;

import java.util.*;

/*******************************************************************************************
* BED format provides a flexible way to define the data lines that are displayed in an annotation track.
* BED lines have three required fields and nine additional optional fields. The number of fields per line
* must be consistent throughout any single set of data in an annotation track. The order of the optional
* fields is binding: lower-numbered fields must always be populated if higher-numbered fields are used.
*
* The first three required BED fields are:
*
* chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random) or scaffold (e.g. scaffold10671).
* chromStart - The starting position of the feature in the chromosome or scaffold. The first base in a chromosome is numbered 0.
* chromEnd - The ending position of the feature in the chromosome or scaffold. The chromEnd base is not
* included in the display of the feature. For example, the first 100 bases of a chromosome are defined
* as chromStart=0, chromEnd=100, and span the bases numbered 0-99.
* The 9 additional optional BED fields are:
*
* name - Defines the name of the BED line. This label is displayed to the left of the BED line in the
*        Genome Browser window when the track is open to full display mode or directly to the left of the item in pack mode.
* score -A score between 0 and 1000. If the track line useScore attribute is set to 1 for this
*        annotation data set, the score value will determine the level of gray in which this feature is displayed
* strand - Defines the strand - either '+' or '-'.
* thickStart - The starting position at which the feature is drawn thickly (for example, the start codon in gene displays).
* thickEnd - The ending position at which the feature is drawn thickly (for example, the stop codon in gene displays).
* itemRgb - An RGB value of the form R,G,B (e.g. 255,0,0). If the track line itemRgb attribute is set to "On",
* blockCount - The number of blocks (exons) in the BED line.
* blockSizes - A comma-separated list of the block sizes. The number of items in this list should correspond to blockCount.
* blockStarts - A comma-separated list of block starts. All of the blockStart positions should be calculated relative to chromStart.
*               The number of items in this list should correspond to blockCount.
* Example:
* Here's an example of an annotation track that uses a complete BED definition:
*
* track name=pairedReads description="Clone Paired Reads" useScore=1
* chr22 1000 5000 cloneA 960 + 1000 5000 0 2 567,488, 0,3512
* chr22 2000 6000 cloneB 900 - 2000 6000 0 2 433,399, 0,3601
*
******************************************************************************************/


/******************************************************************************************
* Example from Epigenomics dataset.. 8,605,199 lines.
*
* chr1    470     505     03_6:4:53:1761:816      1       +       0       0       255,0,0
* chr1    532     567     03_6:4:1:1544:1961      1       +       0       0       255,0,0
* chr1    2919    2954    03_6:4:95:1219:564      1       -       0       0       0,0,255
* chr1    4779    4814    03_6:4:72:1779:1880     1       +       0       0       255,0,0
* chr1    4782    4817    03_6:4:49:190:1071      1       -       0       0       0,0,255
* chr1    5222    5257    03_6:4:12:494:1872      1       -       0       0       0,0,255
* chr1    6037    6072    03_6:4:60:810:1740      1       -       0       0       0,0,255
*****************************************************************************************/

/****************************************************************************************
*  A single item in a bed graph.  This is called simply Bed in the kent codebase
*  since it's a linked list and there is no separation between a single bed item and
*  a whole list of them.
*
*  Space is an issue, and someday I may want to do something more space efficient.
*
*  As it is, I am only implementing the fields I use.  If/when I add the other fields,
*  then I'll have to pay more attention to space.  Currently, this should take about
*  14+8 = 22 bytes/Item.   So, a typical 10 million entry ChIP-seq bed graph might take
*  220MB. So on a reasonable laptop, one could load two of these graphs and intersect
*  them, say.
*
*/

// TODO:  Make this inherit from BedGraphItem and have a separate BedGraph 
//        in order to use less space when extra space not needed. Maybe make 
//        BedGraphItem inherit from Coords.

public class BedItem {

	public byte chromID;         // 4 bytes
	public int  chromStart;      // 4 bytes
	public int  chromEnd;        // 4 bytes
	public byte score;          // 1 byte
	public byte strand;         // 1 byte

	// There are only a few actual chromosome IDs, so rather than save the
	// whole string over and over, just save a smaller ID for the string and look
	// it up as needed.
	static Map<Byte,String> id2Chrom = new HashMap<Byte,String>();
	static Map<String,Byte> chrom2ID = new HashMap<String,Byte>();
	static byte nextChromID = 0;

	public BedItem(String line) {
		this.parseLine(line);
	}

	public String toString() {
		String val = chromID+"\t"+chromStart+"\t"+chromEnd+"\t"+score+"\t"+((char)strand);
		return(val);
	}
	
	public String toCoords(){
	  String chrom = id2Chrom.get(chromID);
	  String val = chrom+"\t"+chromStart+"\t"+chromEnd;
		return(val);
	}
		
	public String toBedGraph(){
	  String chrom = id2Chrom.get(chromID);
	  String val = chrom+"\t"+chromStart+"\t"+chromEnd+"\t"+score;
		return(val);
	}
	
	public String toMinimalBed(){
	  String chrom = id2Chrom.get(chromID);
	  String val = chrom+"\t"+chromStart+"\t"+chromEnd+"\tNAME\t"+score+"\t"+((char)strand);
		return(val);
	}

	/*****************************************************
	* Create a BitVector containing the information from one bed entry.
	*/
	public BedItem(String chrom,int chromStart,int chromEnd, int score,char strand) {
	  setChromID(chrom);
		this.chromStart = chromStart;
		this.chromEnd = chromEnd;
		this.score = (byte)score;
		this.strand = (byte)strand;
	}
	
	/**************************************************
	* Sets the chromosome ID by looking up the chrom in the map. 
	* This trades some speed for space efficiency. 
	*/
	public void setChromID(String chrom){
			// just save an ID for the chromosome
		this.chromID = (byte) nextChromID;
		if (chrom2ID.containsKey(chrom)) this.chromID = chrom2ID.get(chrom);
		else {
			byte id = nextChromID;
			nextChromID++;
			chrom2ID.put(chrom,id);
			id2Chrom.put(id,chrom);
		}
	}

	public static int getChromID(String chrom){
	  return(chrom2ID.get(chrom));
	}
	
	/**********************************************
	* parses a single line from a bed file, returning a single BedItem
	*/
	public void parseLine(String line) {
		String[] fields = line.split("\t");
		String chr = fields[0];
		setChromID(chr);		
		this.chromStart = Integer.parseInt(fields[1]);
		this.chromEnd = Integer.parseInt(fields[2]);
		this.score = (byte) Integer.parseInt(fields[4]);
		this.strand = (byte) fields[5].charAt(0);
	}

}


// time testscripts/bedtest data/BI.H3K27me3.bed 

// time ./testscripts/bedtest.groovy full/BI.H3K4me3.bed 
// Reading full/BI.H3K4me3.bed...done.
// Bed size: 6655339
// real	0m26.612s
// 
// 475MB file, effective throughput: 18MB/sec. 



