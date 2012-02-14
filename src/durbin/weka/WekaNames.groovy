#!/usr/bin/env groovy 

package durbin.weka;


/***
* Some methods to extract information out of weka classifier
* descriptions (e.g. the base classifier type... SMO, etc.)
*/
public class WekaNames{
	
	static err = System.err
	
	/***
	* Given: weka.classifiers.trees.J48 -C 0.25 -M 2 <br><br>
	* return: J48
	*/ 
	static def getBaseClassifierType(spec){
		def m = (spec =~ /weka\.classifiers\.\w+\.(\w+)/)		
		if (m.size() >=1){		
			if (m[0].size() >=1){
				return(m[0][1])
			}
		}
		
		m = (spec =~ /durbin\.weka\.\w+/)		
		if (m.size() >=1){		
			if (m[0].size() >=1){
				return(m[0][1])
			}
		}
				
		err.println "Null base classifier?"
		err.println "match: $m"
		return(null)
	}
}



/*

weka.classifiers.functions.SMO -C {1,16,1} -L {0.0000,0.0010,0.0001} -P 1.0E-12 -N 0 -V -1 -W 1 -K \"$kernel\"',
      'weka.classifiers.meta.AdaBoostM1 -P 100 -S 1 -I 10 -W $subclassifier',
      'weka.classifiers.trees.RandomForest -I 2 -K 0 -S 1',
      'weka.classifiers.trees.J48 -C 0.25 -M 2',
      'weka.classifiers.trees.SimpleCart -S 1 -M 2.0 -N 5 -C 1.0',
      'weka.classifiers.rules.JRip -F 3 -N 2.0 -O 2 -S 1',
      'weka.classifiers.misc.HyperPipes',
      'weka.classifiers.mi.CitationKNN -R 1 -C 1 -H 1',
      'weka.classifiers.lazy.KStar -B 20 -M a',
      'weka.classifiers.lazy.IB1',
      'weka.classifiers.functions.Logistic -R 1.0E-8 -M -1',
      'weka.classifiers.functions.SimpleLogistic -I 0 -M 500 -H 50 -W 0.0',
      'weka.classifiers.functions.VotedPerceptron -I 1 -E 1.0 -S 1 -M 10000',
      'weka.classifiers.functions.MultilayerPerceptron -L 0.3 -M 0.2 -N 500 -V 0 -S 0 -E 20 -H a'



*/