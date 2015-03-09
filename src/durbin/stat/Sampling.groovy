
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
	}		
}