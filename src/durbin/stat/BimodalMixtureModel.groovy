package durbin.stat;

import Tools.ExpectationMaximization1D;
import Tools.KMeans;
import jMEF.*;
import jMEF.BregmanHierarchicalClustering.LINKAGE_CRITERION;
import jMEF.Clustering.CLUSTERING_TYPE;

import durbin.util.*;

class BimodalMixtureModel{

	/***
	* Estimates the parameters of a two gaussian mixture model fitted to the
	* values in the collection row. 
	*/ 
	static def estimateMixtureParameters(row){	

		def rowLength = row.size()
		PVector[] points = new PVector[rowLength]		
		row.eachWithIndex{val,i->
			PVector v = new PVector(1)
			v.array[0] = val
			points[i]	= v	
		}		

		def n = 2;
		Vector<PVector>[] clusters = KMeans.run(points, n);		

		// Bregman soft clustering
		MixtureModel mm;
		mm = BregmanSoftClustering.initialize(clusters, new UnivariateGaussian());
		mm = BregmanSoftClustering.run(points, mm);
	}

	/***
	* Computes the bimodality index from the gaussian mixture model parameters. 
	*/ 
	static def bimodalityIndex(mm,n){
		def mean1 = mm.param[0].array[0]
		def var1 = mm.param[0].array[1]

		def mean2 = mm.param[1].array[0]
		def var2 = mm.param[1].array[1]

		def maxvar = max(var1,var2)
	  double delta = Math.abs(mean1-mean2)/Math.sqrt(maxvar)
		double pi = mm.weight[0]
		//err.print "delta=$delta\tpi=$pi\tweight=${mm.weight[0]*n as int}\tn=${n}\t"

		double rval = Math.sqrt(pi*(1-pi))*delta
		return(rval)
	}

	static def max(a,b){
		if (a > b) return(a)
		else return(b)
	}

}