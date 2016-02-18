package swiftml

class CVExperiment{
	String className
	def classPerformance = []	
	double wauc // weighted auc ..weighted by class size
	
	class ClassPerformance{
		String classValue 
		double precision
		double recall
		Map confusion
	}	
			
	def CVExperiment(eval,cn){
		wauc = eval.weightedAreaUnderROC() 
		wauc = wauc.round(4)
		className = cn
		// actuali = actual value of samples that are i
		// calledi = the number of samples that were predicted to be i
		eval.m_ClassNames.eachWithIndex{nameActual,actuali->
			def cp = new ClassPerformance()
			cp.precision = eval.precision(actuali)
			cp.recall = eval.recall(actuali)
			cp.classValue = nameActual
			cp.confusion = [:]
			eval.m_ClassNames.eachWithIndex{nameCalled,calledi->
				cp.confusion[nameCalled] = eval.m_ConfusionMatrix[actuali][calledi] as int	
			}						
			classPerformance << cp
		}
	}				
}


/**
In Weka, the confusion matrix rows are Actual values, columns are calls. 
For example:

numTruePositives
	j in numClasses
		if (j == classIndex) correct+= m_ConfusionMatrix[classIndex][j]

actual == classIndex and column == class Index === TP

numFalsePositives
	i in numClasses
		row != class
		j in numClasses
			column == class
				incorrect += m_ConfusionMatrix[i][j]

numFalseNegatives
	i in numClasses
		row == class
		j in numClasses
			column != class
				incorrect += m_ConfusionMatrix[i][j]
*/ 



