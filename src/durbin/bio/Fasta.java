/****************************************************************************************
 * Fasta
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.lang.Iterable;
import java.util.Iterator;

/**********************************************************************************
* Implements reading and writing of Fasta files.  Many bioinformatics applications are
* actually I/O bound in terms of performance, so it is important to pay attention to
* performance in a class like this.
*/
public class Fasta implements Iterable<Sequence> {

	private static Pattern fastaIdPattern = Pattern.compile("^>(\\S+)\\s+(.*)");

	BufferedReader in;
	String lastName="";
	String lastDescription = "";
	boolean iseof = false;

	/********************************************************************************
	*
	*/
	static class FastaIterator implements Iterator<Sequence> {

		Fasta fa;

		public FastaIterator(Fasta fa) {
			this.fa = fa;
		}

		public boolean hasNext() {
			return(fa.hasNext());
		}

		public Sequence next() {
			try {
				return(fa.getNextSequence());
			} catch (Exception e) {
				throw new RuntimeException(e.toString());
			}
		}

		public void remove() {
			System.err.println("Fasta.remove() is not currently supported.");
			throw new RuntimeException("Fasta.remove() is not currently supported.");
		}

	}

	public Fasta(String fName) throws Exception {
		File fasta = new File(fName);
		FileReader fr = new FileReader(fasta);
		BufferedReader br = new BufferedReader(fr);
		this.in = br;
		iseof = false;
	}

	public Fasta(BufferedReader in) {
		this.in = in;
		iseof = false;
	}

	public Fasta(InputStream sin) {
		this.in = new BufferedReader(new InputStreamReader(sin),131072);
	}


	public boolean hasNext() {
		return(!iseof);
	}

	/******************************
	*
	*/
	public Iterator<Sequence> iterator() {
		return new FastaIterator(this);
	}

	/******************************
	* Disable line buffering.
	* By default, System.out flushes the buffer every time a newline is encountered.
	* Here we disable this and use a nice big buffer instead, which improves
	* performance significantly writing out big FASTA files.
	*/
	public static void disableAutoFlush() {
		FileOutputStream fdout = new FileOutputStream(FileDescriptor.out);
		BufferedOutputStream bos = new BufferedOutputStream(fdout,65536);
		PrintStream ps = new PrintStream(bos,false);
		System.setOut(ps);
	}

	/*******************************
	* Prints the sequence to STDOUT
	*/
	public static void print(Sequence seq) {

		disableAutoFlush();

		// Write the name and description..
		System.out.println(">"+seq.getName()+" "+seq.getDescription());

		// Write the sequence, 60 bases/line.
		StringBuilder data = seq.getData();

		int stop = data.length()/60;
		int lineNum = 0,j=0;
		while (lineNum < stop) {
			System.out.println(data.subSequence(j,j+60));
			j+=60;
			lineNum++;
		}
		if (j < data.length()) {
			System.out.println(data.subSequence(j,data.length()));
		}
		System.out.flush();
	}

	public List<Sequence> getSequences() throws Exception {
		ArrayList<Sequence> rval = new ArrayList<Sequence>();
		Sequence seq;
		while ((seq = getNextSequence()) != null) {
			rval.add(seq);
		}
		return(rval);
	}


	/*******************************
	* Gets the next sequence from the reader
	* Returns null when there are no more sequences in the FASTA stream.
	*/
	public Sequence getNextSequence() throws Exception {
		Sequence seq = new Sequence();
		StringBuilder data = seq.getData();

		// If the last call to this was from end of file, then simply return.
		if (iseof) return(null);

		String line=null;
		while ((line = in.readLine()) != null) {
			if (line.startsWith(">")) {
				Matcher match = fastaIdPattern.matcher(line);
				if (!match.matches()) {
					throw new Exception("Id line not found: "+line);
				}

				// If it's the first time through, just record the name...
				if (lastName=="") {
					lastName = match.group(1);
					lastDescription = match.group(2);
					continue;
				}

				// We've seen a name before, so time to return the NextSequence
				seq.setName(lastName);
				seq.setDescription(lastDescription);
				lastName = match.group(1);
				lastDescription = match.group(2);
				return(seq);
			} else {
				data.append(line);
			}
		}
		// End of file, return the last dangling sequence..
		iseof = true;
		seq.setName(lastName);
		seq.setDescription(lastDescription);
		return(seq);
	}

	public void flush() {
		System.out.flush();
	}


	/*******************************
	*
	*
	*/
	public static void main(String args[]) {

		try {
			BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
			Fasta fa = new Fasta(sin);

			Sequence seq;
			while ((seq = fa.getNextSequence()) != null) {
				fa.print(seq);
			}
			fa.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}