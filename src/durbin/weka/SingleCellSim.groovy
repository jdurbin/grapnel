package durbin.weka;
import durbin.weka.*;
import weka.core.*;


class SingleCellSim{
	
	static def rng = new Random()
	
	static def readMap(mapFile){
		def map = [:]
		new File(mapFile).withReader{r->
			r.splitEachLine("\t"){fields->
				def gene = fields[0]
				def value = fields[3] as double
				map[gene] = value
			}
		}
		return(map)
	}
	
	
	static Instances removegenes(LinkedHashMap geneFreqMap,data){

		// Create new simulated data...
		Instances simdata = new Instances(data, 0);	
		
		// Scale each attribute value by the mixture model parameters...
		// Hmmm... important question... is attribute 0 the name or not?
		for (int i = 0; i < data.numInstances(); i++) {
			double[] values = new double[data.numAttributes()];
			for (int attrIdx = 0; attrIdx < data.numAttributes(); attrIdx++){
		        Attribute attr = data.attribute(attrIdx)
		        def gene = attr.name()
				def prGene = geneFreqMap[gene]
				//println "gene name="+gene+" freq="+prGene
				// If the random number falls outside the range for that gene, return a missing value.
				// otherwise, leave the value unchanged. 
				values[attrIdx] = data.instance(i).value(attrIdx)
				if (prGene != null){
					if ((rng.nextInt(1000)/1000.0) > prGene) values[attrIdx] = Utils.missingValue()
				}
			}
			simdata.add(new DenseInstance(1, values));
		}
		return(simdata)	
	}
}