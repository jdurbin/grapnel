package swiftml

class CVExperiment{
	String className
	def values = []	
	double roc	
	
	class ValuePerformance{
		String classValue 
		int tp
		int fp
		int tn
		int fn
		double precision
		double recall
	}
			
	def CVExperiment(eval,cn){
		roc = eval.weightedAreaUnderROC() 
		roc = roc.round(4)
		className = cn
		eval.m_ClassNames.eachWithIndex{name,i->
			def vp = new ValuePerformance()
			vp.tp = eval.numTruePositives(i)
			vp.fp = eval.numFalsePositives(i)
			vp.tn = eval.numTrueNegatives(i)
			vp.fn = eval.numFalseNegatives(i)
			vp.precision = eval.precision(i)
			vp.recall = eval.recall(i)
			vp.classValue = name
			values << vp
		}
	}
	
	
	
	
}
