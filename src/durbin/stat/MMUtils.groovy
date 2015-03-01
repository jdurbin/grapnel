package durbin.stat;

import Tools.ExpectationMaximization1D;
import Tools.KMeans;
import jMEF.*;
import jMEF.BregmanHierarchicalClustering.LINKAGE_CRITERION;
import jMEF.Clustering.CLUSTERING_TYPE;

import durbin.util.*;

class MMUtils{

	
	/**
	* Creates a mixture model from a parameter list like:
	*
	* paramList = [[0.357,0.079,0.362],[0.145,3.001,0.010],[0.498,10.004,0.380]]
	* 
	* Where each triplet is weight, mean, variance. 
	*/ 
	static def mixtureFromParamList(paramList){
		def mm = new MixtureModel(paramList.size())
		mm.EF = new UnivariateGaussian()
	
		for(int i = 0;i < paramList.size();i++){
			mm.weight[i] = paramList[i][0]
		
			def mean = paramList[i][1]
			def variance = paramList[i][2]
		
			def p = new PVector(2)
			p.array[0] = mean
			p.array[1] = variance
			mm.param[i] = p
		}	
		return(mm)
	}
	
	
	/***
	* Converts a string representation of the mixture into 
	* a list of mixture parameters. 
	*/ 
	static def string2List(s){
		def mixtureList = []
		def mixtures = s.split("\t")
		mixtures.each{mixture->
			def params = mixture.split(",")
			params = params.collect{it as double}
			mixtureList << params
		}	
		return(mixtureList)
	}
	
	/***
	*  Converts a mixture to a simple tab separated string of triplets like:
	*
	*  0.357,0.079,0.362	0.145,3.001,0.010	0.498,10.004,0.380
	* 
	*/ 
	static def toString(mm2){
		def totalOutput = []
		for(int i=0; i<mm2.size;i++){	
			def mean = mm2.param[i].array[0]
			def variance = mm2.param[i].array[1]
			def output = sprintf("%4.3f,%4.3f,%4.3f",[mm2.weight[i],mean,variance])
			totalOutput << output
		}
		return(totalOutput.join("\t"))
	}
	
	// Converts a single number x into a PVector point for jMEF
	static def asPoint(x){
	 	PVector[] points = new PVector[1]
	 	PVector v = new PVector(1)
	 	v.array[0] = x		
	 	points[0]	= v	
	 	return(points)
	}


	/**
	* Determines which of the n mixtures the point x has the maximum likelihood for. 
	*/ 
	static def getClass(mm,x){
		def p = asPoint(x)
		def n = mm.size
		def maxf = -99999999 as double
		def maxidx = -1
		for (int j=0; j<n; j++){
			def f_tmp = mm.weight[j] * mm.EF.density(p[0], mm.param[j]);
			if (f_tmp > maxf){
				maxf = f_tmp
				maxidx = j
			}
		}
		return(maxidx)
	}

	/***
	* Estimates the parameters of a two gaussian mixture model fitted to the
	* values in the collection row. 
	*/ 
	static def estimateMixtureParameters(row,maxMixtures){	

		def rowLength = row.size()
		PVector[] points = new PVector[rowLength]		
		row.eachWithIndex{val,i->
			PVector v = new PVector(1)
			v.array[0] = val
			points[i]	= v	
		}		

		def n = maxMixtures;
		Vector<PVector>[] clusters = KMeans.run(points, n);		

		// Bregman soft clustering
		MixtureModel mm;
		mm = BregmanSoftClustering.initialize(clusters, new UnivariateGaussian());
		mm = BregmanSoftClustering.run(points, mm);
	}

	static def max(a,b){
		if (a > b) return(a)
		else return(b)
	}

}