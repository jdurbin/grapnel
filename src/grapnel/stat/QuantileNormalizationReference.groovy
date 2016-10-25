package grapnel.stat;

import hep.aida.bin.QuantileBin1D;


/* 
	Reference version uses the COLT QuantileBin1D class.  This class efficiently
	stores the minimal information needed about distribution samples to produce
	quantiles with a specific error bound.  It's a near ideal way to do this
	because it doesn't matter if the number of probes match between the training
	and test sets.   It's a bit complicated, though, to implement in different 
	languages, so I'm trying to decide what the simplest way to do effectively 
	the same thing is.   

   https://dst.lbl.gov/ACSSoftware/colt/api/hep/aida/bin/QuantileBin1D.html
   Using little main memory, quickly computes approximate quantiles over very 
   large data sequences with and even without a-priori knowledge of the number 
   of elements to be filled; Conceptually a strongly lossily compressed multiset 
   (or bag); Guarantees to respect the worst case approximation error specified 
   upon instance construction
*/ 

class QuantileNormalizationReference implements Serializable{

	def trainingQuantileBin = null; 

	/***
	*  Save a compression of the training distribution
	*/ 
	void compressDistribution(trainingValues){
		def maxError = 0.001 
		trainingQuantileBin = new QuantileBin1D(maxError) 
		trainingValues.each{value->
			trainingQuantileBin.add(value)
		}				
	}

	/***
	*  Transforms a vector of values from a test set into the 
	*  distribution of the training set.  
	*/ 
	double[] transform(double[] testValues){

		// Compute quantiles for the test values...
		def testQuantileBin = new QuantileBin1D(0.001) 
		testValues.each{value->
			testQuantileBin.add(value)
		}
		
		def outputValues = []
		testValues.each{value->
			// look up the test quantile for each value values...
			def testQuantile = testQuantileBin.quantileInverse(value)

			// map it onto the training distribution.  
			def newValue = trainingQuantileBin.quantile(testQuantile) 
			outputValues.add(newValue)
		}
		return(outputValues as double[])						
	}
	
	
	/**
	* Save the trained object. For big datasets can be a tiny fraction of 
	* the size of the original dataset. 
	*/
	def save(fileName){
		FileOutputStream fout = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(this);
	}	
	
	static def read(fileName){
		FileInputStream fin = new FileInputStream(fileName);
		ObjectInputStream ois = new ObjectInputStream(fin);
		QuantileNormalizationReference qn = (QuantileNormalizationReference) ois.readObject();
		return(qn)
	}
	
			
}