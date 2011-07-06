package durbin.weka;

import java.util.*;

import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.RemoveUseless;
import weka.filters.supervised.attribute.*;
import weka.filters.unsupervised.instance.RemoveRange;

/***
* Balanced subsampling that achieves balance by down-sampling the majority 
* class instead of upsampling the minority class. 
*/ 
public class BalancedSubsampler{

	/***
	* Return a list of two lists.  The first list contains the indices of 
	* all minority instances.  The second list contains the indices of all 
	* majority instances.   
	*/ 
	//@Typed 
	static FastVector getMinorMajorInstanceIdxs(Instances data){
	
		//System.err.println("CHK1");
	
		// Get all the values for the class attribute... these will be doubles, 
		// numbers for numeric, an index coded in doubles for nominal. 
		int classIdx = data.classIndex();
		Attribute classAttribute = data.attribute(classIdx);
		double[] atvals = data.attributeToDoubleArray(classIdx);
	
		//System.err.println("CHK2");
		
		HashSet<Double> valueSet = new HashSet<Double>();
		for(double val: atvals) {
			valueSet.add(val);
		}
		if (valueSet.size() > 2){
			System.err.println("ERROR: BalancedRandomTree currently only supports binary classes. ");
			System.exit(1);
		}
		double[] values = new double[2];
		int i = 0;
		for(double val: valueSet){
			values[i++] = val;
		}		
		double vA = values[0];
		double vB = values[1];

		// Now go through values and figure out minority class..			
		int numA = 0;
		int numB = 0;
		for(i = 0;i < data.numInstances();i++){
			if (atvals[i] == vA) numA++;
			if (atvals[i] == vB) numB++;
		}
		
		//System.err.println("DEBUG numA: "+numA);
		//System.err.println("\tnumB: "+numB);
	
		double majorityClass;
		if (numA > numB) majorityClass = vA;
		else majorityClass = vB;
	
		// Now create separate lists of minor and major classes... a list containing two lists...
		FastVector majorList = new FastVector();
		FastVector minorList = new FastVector();
		for(i = 0;i < data.numInstances();i++){
			if (atvals[i] == majorityClass) majorList.addElement((Integer) i);
			else minorList.addElement((Integer) i);
		}
	
		FastVector rvals = new FastVector();
		rvals.addElement(minorList);
		rvals.addElement(majorList);

		return(rvals);
	}
	
	//static Instances balanceSubsampleMinorMajor(Instances fullData,double bootstrapFraction) throws Exception{
	//	Random rng = new Random();
	//	balanceSubsampleMinorMajor(fullData,bootstrapFraction,rng);
	//}

	/***
	* Return a set of instances that are a balanced subsample of the minor and major
	*/ 
	static Instances balanceSubsampleMinorMajor(Instances fullData,double bootstrapFraction,Random rng) throws Exception{
	
		// Get a list of the minor class instances and the major class instances
		FastVector minorMajorInstanceIdxs = getMinorMajorInstanceIdxs(fullData);
	
		FastVector minorInstanceIdxs = (FastVector) minorMajorInstanceIdxs.elementAt(0);
		FastVector majorInstanceIdxs = (FastVector) minorMajorInstanceIdxs.elementAt(1);
		int numMinor = minorInstanceIdxs.size();
		int numMajor = majorInstanceIdxs.size();
	
		//System.err.println("numMinor: "+numMinor);
		//System.err.println("numMajor: "+numMajor);
		
		// Seeds with time if no seed given.  KJD: Note that SecureRandom can be 
		// used for more industrial strength, but slower, random number generation. 
		//Random rng = new Random();
	
		FastVector includeSamples = new FastVector();
	
		int bootstrapSamples = (int) (numMinor * bootstrapFraction);
		for(int i = 0;i < bootstrapSamples;i++){			
			int minorIdx = rng.nextInt(numMinor);
			int majorIdx = rng.nextInt(numMajor);			
			//System.err.println("minorIdx: "+minorIdx);
			//System.err.println("majorIdx: "+majorIdx);
			
			int minorSampleIdx = minorInstanceIdxs.elementAt(minorIdx);
			int majorSampleIdx = majorInstanceIdxs.elementAt(majorIdx);
			
			includeSamples.addElement(minorSampleIdx);
			includeSamples.addElement(majorSampleIdx);
		}
		
		// Remove everything that isn't in our sample list...
		RemoveRange remove = new RemoveRange();

		def resampleList = []
		for(int i = 0;i < includeSamples.size();i++){
			int sampleIdx = (Integer) includeSamples.elementAt(i);
			resampleList << sampleIdx
		}
		
		//System.err.println "resampleList: "+resampleList;
		
		def data = new Instances(fullData,resampleList.size())
		//System.err.println "data.numInstances: "+data.numInstances();
		resampleList.each{sampleIdx->
			def instance = fullData.instance(sampleIdx)
			//System.err.println "instance $sampleIdx class value: "+instance["BRAFmutant"]
			data.add(instance);
		}
		//System.err.println "data.numInstances: "+data.numInstances();
						
		return(data);				
	}
}


