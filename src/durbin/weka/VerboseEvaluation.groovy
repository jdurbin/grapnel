
package durbin.weka;

import weka.classifiers.*;
import weka.classifiers.evaluation.ThresholdCurve
import weka.core.*;
import java.util.*;



class VerboseEvaluation extends Evaluation{
  
  public VerboseEvaluation(data){
    super(data)
  }

  public void crossValidateModel(Classifier classifier,Instances data, int numFolds, Random random, Object... forPredictionsPrinting) throws Exception {
                            
    // Make a copy of the data we can reorder
    data = new Instances(data);
    data.randomize(random);
    if (data.classAttribute().isNominal()) {
      data.stratify(numFolds);
    }

    // We assume that the first element is a StringBuffer, the second a Range (attributes
    // to output) and the third a Boolean (whether or not to output a distribution instead
    // of just a classification)
    if (forPredictionsPrinting.length > 0) {
      // print the header first
      StringBuffer buff = (StringBuffer)forPredictionsPrinting[0];
      Range attsToOutput = (Range)forPredictionsPrinting[1];
      boolean printDist = ((Boolean)forPredictionsPrinting[2]).booleanValue();
      printClassificationsHeader(data, attsToOutput, printDist, buff);
    }

    // Do the folds
    for (int i = 0; i < numFolds; i++) {
      Instances train = data.trainCV(numFolds, i, random);
      setPriors(train);
      Classifier copiedClassifier = Classifier.makeCopy(classifier);
      copiedClassifier.buildClassifier(train);
      Instances test = data.testCV(numFolds, i);
      def predictions = evaluateModel(copiedClassifier, test, forPredictionsPrinting);
            
      def tc = new ThresholdCurve();
      def result = tc.getCurve(predictions,0)
      def roc0 = tc.getROCArea(result)
      result = tc.getCurve(predictions,1)
      def roc1 = tc.getROCArea(result)
      
      System.err.println("Fold: "+i+" cumulative auc:"+ weightedAreaUnderROC()+
      " auc0: "+roc0+" auc1: "+auc1)      
    }
    m_NumFolds = numFolds;
  }
}

/*


double areaUnderROC(int classIndex){
  ThresholdCurve tc = new ThresholdCurve();
  Instances result = tc.getCurve(m_Predictions, classIndex);
  return ThresholdCurve.getROCArea(result);
}

public double weightedAreaUnderROC() {
  double[] classCounts = new double[m_NumClasses];
  double classCountSum = 0;

  for (int i = 0; i < m_NumClasses; i++) {
    for (int j = 0; j < m_NumClasses; j++) {
      classCounts[i] += m_ConfusionMatrix[i][j];
    }
    classCountSum += classCounts[i];
  }

  double aucTotal = 0;
  for(int i = 0; i < m_NumClasses; i++) {
    double temp = areaUnderROC(i);
    if (!Instance.isMissingValue(temp)) {
      aucTotal += (temp * classCounts[i]);
    }
  }

  return aucTotal / classCountSum;
}
*/
