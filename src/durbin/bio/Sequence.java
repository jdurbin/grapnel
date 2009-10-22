/****************************************************************************************
 * Sequence
 *
 * $Id: $
 * $Change:  $  $LastChangedBy: $
 *
 * Original author:
 *
 *
 *        1         2         3         4         5         6         7
 *2345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
 */

package durbin.bio;

import java.io.*;
import java.lang.Iterable;
import java.util.Iterator;
import groovy.lang.IntRange;

/****************************************************************************************
 * A basic class to represent a DNA sequence.
 *
 * The sequences we work with usually have a name and a description and the actual sequence.
 *
 * In addition to the sorts of operations one would do on strings, with Sequences one may
 * also wish to compute the reverse complement, to validate that the characters are valid
 * DNA bases, and so on.
 *
 * <b>Note:</b> One of the goals of this class is to be useful also as the underlying
 * representation for algorithms that operate on sequences, such as SWAligner and so on.
 * To that end, we would like this class to be as fast and space efficient as possible.
 * It should be able to handle quite large sequences, say 300 Mbp long. The internal representation
 * will likely change to improve performance.   At a minimum, it'll probably change to some
 * 3-bit representation (e.g. for A,C,T,G,N,X) in order to improve space efficiency.  It's
 * worth the bother because Sequence is THE CORE data _ for all analysis.  Everything
 * will use Sequence objects, lots of them and huge ones.
 *
 */
public class Sequence implements Serializable {


	/********************************************************************************
	*
	*/
	static class SequenceIterator implements Iterator<Character> {

		Sequence seq;
		int idx=0;

		public SequenceIterator(Sequence s) {
			this.seq = s;
			idx = 0;
		}

		public boolean hasNext() {
			return(idx < (seq.size()-1));
		}

		public Character next() {
			return(seq.getAt(idx++));
		}

		public void remove() {
			System.err.println("Fasta.remove() is not currently supported.");
			throw new RuntimeException("Fasta.remove() is not currently supported.");
		}
	}


	// Define a complement array...
	static final char[] comp= new char[128];
	static {
		comp['A']=comp['a']='T';
		comp['C']=comp['c']='G';
		comp['G']=comp['g']='C';
		comp['T']=comp['t']='A';
		comp['V']=comp['v']='B';
		comp['H']=comp['h']='D';
		comp['R']=comp['r']='Y';
		comp['M']=comp['m']='K';
		comp['Y']=comp['y']='R';
		comp['K']=comp['k']='M';
		comp['B']=comp['b']='V';
		comp['D']=comp['d']='H';
		comp['U']=comp['u']='A';
		comp['N']=comp['n']='N';
		comp['W']=comp['w']='W';
		comp['S']=comp['s']='S';
	}

	public String name = "";
	public String description = "";
	public StringBuilder data = new StringBuilder();

	// Quality is optional.  KJD:  In my C++ classes I made quality a separate object and
	// almost always found that to have been more of a pain than if I had made it part of a
	// Sequence.  The point is debatable, nonetheless.
	public int[]   quality;


	public Sequence() {}
	public Sequence(Sequence s) {
		this.name = s.name;
		this.description = s.description;
		this.data = new StringBuilder(s.data.toString());
	}

	public Sequence(String s) {
		data = new StringBuilder(s);
	}

	public Sequence plus(Sequence a) {
		Sequence rval = new Sequence(this);
		rval.data = rval.data.append(a.data);
		return(rval);
	}

	public int hashCode() {
		int rval = this.data.toString().hashCode();
		return(rval);
	}


	public boolean equals(Object o) {
		Sequence s = (Sequence)o;
		boolean rval = (s.data.toString().equals(this.data.toString()));
		return(rval);
	}

	public boolean equals(Sequence s) {
		boolean rval = (s.data.toString().equals(this.data.toString()));
		return(rval);
	}

	public int compareTo(Object o) {
		Sequence s = (Sequence) o;
		int rval = s.data.toString().compareTo(this.data.toString());
		return(rval);
	}

	public int compareTo(Sequence s) {
		int rval = s.data.toString().compareTo(this.data.toString());
		return(rval);
	}

	/****************************************
	* isPalindrome

	public boolean isPalindrome(){

		if (size() % 2 ){
			// for odd length sequences, revcomp($synthetic) = $synthetic is palindrome
			// where $synthetic is $seq with middle letter removed.
			int halfLen = (int)(size()/2);
			int end1 = halfLen-1;
			int start2 = halfLen+1;
			int end2 = seq.size()-1;
			SequenceBuffer synthetic = data.substring[0..end1]+seq[start2..end2];
			Sequence rev = synthetic.reverseComplementCopy();

	//		println "seq=$seq\tsynthetic=$synthetic\trev=$rev"

			return (rev == synthetic);
		}else{
			rev = seq.reverseComplementCopy();
			return(rev == seq);
		}
	}
	*/

	/******************************
	*
	*/
	public Iterator<Character> iterator() {
		return new SequenceIterator(this);
	}

	/*******************************
	* Reverse complements this sequence.
	*/
	public Sequence reverseComplement() {
		data.reverse();
		int buflen = data.length();
		for (int i = 0;i < buflen;i++) {
			data.setCharAt(i,comp[data.charAt(i)]);
		}
		return(this);
	}


	/*******************************
	* Reverse complements this sequence.
	*/
	public Sequence reverseComplementCopy() {
		Sequence rtnSeq = new Sequence();
		rtnSeq.setName(this.name);
		rtnSeq.setDescription(this.description);
		rtnSeq.setData(new StringBuilder(this.data.toString()));
		rtnSeq.reverseComplement();
		return(rtnSeq);
	}
	
	
	public Sequence subSequence(int start,int end){
	  Sequence rseq = new Sequence();
	  rseq.name = this.name;
	  rseq.description = this.description;
	  rseq.data = new StringBuilder(this.data.substring(start,end+1));
	  return(rseq);
	}

	/*******************************************************************************
	*       Getters and Setters
	********************************************************************************/

	public StringBuilder getData() {
		return(data);
	}

  public char baseAt(int idx){
    return(data.charAt(idx));
  }

  // Support for Groovy operator overloading. 
	public char getAt(int idx) {
		return(data.charAt(idx));
	}
	
	public Sequence getAt(IntRange r) {
		Sequence rseq = new Sequence();
		rseq.name = this.name;
		rseq.description = this.description;
		rseq.data = new StringBuilder(this.data.substring(r.getFromInt(),r.getToInt()+1));
		return(rseq);
	}

	public String toString() {
		return(this.data.toString());
	}


	public int size() {
		return(data.length());
	}

	public void setData(StringBuilder d) {
		data=d;
	}

	public String getName() {
		return(name);
	}
	public String getDescription() {
		return(description);
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setDescription(String desc) {
		this.description = desc;
	}

}
