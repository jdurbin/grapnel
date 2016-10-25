package grapnel.stat;

import com.google.common.collect.*
import cern.jet.random.sampling.RandomSampler
import cern.jet.random.engine.MersenneTwister


class Sampling{

	/**
	* Returns a random subset of the given list.  
	*/ 
	public static def randomSubset(list,n){
	    def values = new long[n]
	    RandomSampler.sample(n,list.size(),n,0,values,0,
			new MersenneTwister((int)System.currentTimeMillis()));
	    def outlist = []
	    values.each{i->
			outlist	<< list[(int)i]
	    }
		return(outlist);		
	}		
	
	
	/**
	* Returns a random subset of the given list.  
	*/ 
	public static def randomSubsetWithIndexes(list,n){
	    def values = new long[n]
	    RandomSampler.sample(n,list.size(),n,0,values,0,
			new MersenneTwister((int)System.currentTimeMillis()));
	    def outlist = []
		def outvalues = []
	    values.each{i->
			outlist	<< list[(int)i]
			outvalues << (int)i
	    }		
		return([outlist,outvalues])
	}	
	
}