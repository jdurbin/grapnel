package edu.ucsc

/****
* Minimal support for running parasol jobs on cluster. 
*/
class Para{
	/**
	* Run parasol jobs...
	*/ 
	static def runjobs(jobname,jobdir){
		"""
		cd $jobdir
		para create $jobname
		para push 
		""".bash()	
		
		def numOK = 0
		def numInBatch = 9999
		while(numOK < numInBatch){
			(numOK,numInBatch) = paraCheck(jobdir)
			println "para numInBatch: $numInBatch \tnumOK: $numOK"
			sleep(10000) // check status every ten seconds.
		}
	}
	
	static def paraCheck(jobdir){
		def checkout = """
			cd $jobdir
			para check
		""".bash()
		checkout = checkout.toString()
		
		def numOK
		def numInBatch 
		fields = checkout.split("\n")
		fields.each{f->
			if (f.contains("ranOk")){
				numOK = f.split(":")[1] as int
			}
			if (f.contains("total jobs in batch")){
				numInBatch = f.split(":")[1] as int
			}
		}
		return([numOK,numInBatch])
	}
}
