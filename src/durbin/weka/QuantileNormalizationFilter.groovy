package durbin.weka;

import weka.core.*;
import weka.core.Capabilities.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;

import org.apache.commons.math.stat.ranking.*;
import org.apache.commons.math.distribution.*;

/*
public class QuantileNormalizationFilter extends GenericFilter {

	static{WekaAdditions.enable();}
	//static err = System.err

	public String globalInfo() {
		return   "Replaces  "
		+ "containing the index of the processed instance.";
	}

	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
//		result.enableAllAttributes();
		result.disableAllAttributes();
		result.enableAllClasses();
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.NO_CLASS);  //// filter doesn't need class to be set//
		result.enable(Capability.MISSING_CLASS_VALUES); // doesn't even need a class value
		result.enable(Capability.MISSING_VALUES);
		return result;
	}

	protected Instances determineOutputFormat(Instances inputFormat) {
		Instances result = new Instances(inputFormat, 0);
		return result;
	}
*/
	
	/*
	A quick illustration of such normalizing on a very small dataset:
	
	Arrays 1 to 3, genes A to D
	A    5    4    3
	B    2    1    4
	C    3    4    6
	D    4    2    8
	
	For each column determine a rank from lowest to highest and assign number i-iv
	A    iv    iii   i
	B    i     i     ii
	C    ii    iii   iii
	D    iii   ii    iv
	
	These rank values are set aside to use later. Go back to the first set of data. Rearrange that first set 
	of column values so each column is in order going lowest to highest value. (First column consists 
	of 5,2,3,4. This is rearranged to 2,3,4,5. Second Column 4,1,4,2 is rearranged to 1,2,4,4, and 
	column 3 consisting of 3,4,6,8 stays the same because it is already in order from lowest to highest value.) 
	The result is:
	
	A    5    4    3    becomes A 2 1 3
	B    2    1    4    becomes B 3 2 4
	C    3    4    6    becomes C 4 4 6
	D    4    2    8    becomes D 5 4 8
	
	Now find the mean for each row to determine the ranks
	A (2 1 3)/3 = 2.00 = rank i
	B (3 2 4)/3 = 3.00 = rank ii
	C (4 4 6)/3 = 4.67 = rank iii
	D (5 4 8)/3 = 5.67 = rank iv
	
	Now take the ranking order and substitute in new values
	A    iv    iii   i
	B    i     i     ii
	C    ii    iii   iii
	D    iii   ii    iv
	
	becomes:
	A    5.67    4.67    2.00
	B    2.00    2.00    3.00
	C    3.00    4.67    4.67
	D    4.67    3.00    5.67
*/

	/*
	protected Instances process(Instances instances) throws Exception {
	
		Instances result = new Instances(determineOutputFormat(instances), 0);
		ExponentialDistributionImpl exp = new ExponentialDistributionImpl(1.0);
		
		// Save rank lists for each attribute...
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.FIXED,TiesStrategy.MAXIMUM);						

		for(int i = 0;i < instances.numInstances();i++){
		//for(i in 0..< instances.numInstances()){			
			double[] newValues = new double[instances.numAttributes()]; // create space for new values
			Instance inst = instances.instance(i);
			
			// rank the attribute values...
			double[] attrvals = inst.toDoubleArray(); 											
			double [] attrranks = ranking.rank(attrvals);
			double attsum = attrranks.length - countNAN(attrvals);
			double invattsum = (double)(1.0/attsum);
			
			for (int a = 0; a < instances.numAttributes(); a++){
				double oldval = attrvals[a];
				double rank = attrranks[a];
				double scaled = rank*invattsum - invattsum;
				double newVal = Math.abs(exp.inverseCumulativeProbability(Math.abs(scaled)));
				newValues[a] = newVal;
			}
			result.add(new Instance(1, newValues));
		}
		return result;
	}
	
	double countNAN(double[] vals){
		int count = 0;
		for(int i = 0;i < vals.length;i++){
			double val = vals[i];
			if (val == Double.NaN) count++;
		}
		return(count);
	}
	
		
}
	*/