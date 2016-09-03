package durbin.stat;

class QuantileNormalization {
	def trainingQuantileBin = null; 
	def samplesToSave
	
	def QuantileNormalization(numQuantiles){
		samplesToSave = numQuantiles
	}

	def QuantileNormalization(){
	}

	def setNumQuantiles(numQuantiles){
		samplesToSave = numQuantiles
	}
	

	/***
	*  Save a compression of the training distribution
	*/ 
	void compressDistribution(trainingValues){
		trainingQuantileBin = new SampledQuantileBin(samplesToSave,trainingValues) 		
	}

	/***
	*  Transforms a vector of values from a test set into the 
	*  distribution of the training set.  
	*/ 
	double[] transform(double[] testValues){

		// Compute quantiles for the test values...
		// For these quantiles the sampling will be exhaustive since it's just one sample
		def testQuantileBin = new SampledQuantileBin(testValues.size(),testValues) 
		
		// Now map the quintiles of the testvalues onto the values of the 
		// training quintiles...
		def outputValues = []
		testValues.each{value->
			// look up the test quantile for each value...
			def testQuantile = testQuantileBin.inverseQuantile(value)

			// map it onto the training distribution.  
			def newValue = trainingQuantileBin.quantile(testQuantile) 
			outputValues.add(newValue)
		}
		return(outputValues as double[])						
	}	
	
	
	def save(String fileName){
		trainingQuantileBin.save(fileName)
	}
	
	
	def read(String fileName){
		trainingQuantileBin = SampledQuantileBin.read(fileName)
	}
		
}