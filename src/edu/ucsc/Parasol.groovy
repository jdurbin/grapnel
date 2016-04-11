package edu.ucsc

import durbin.util.*

/****
* Minimal support for running parasol jobs on cluster. 
*/
class Para{
	/**
	* Run parasol jobs...
	*/ 
	static def runjobs(jobname,jobdir){
		RunBash.enable()
		
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
		
		// TODO:  detect hung jobs
		// TODO:  detect failed jobs and return some kind of error status
		
		done(jobdir)
	}
	
	static def paraCheck(jobdir){
		RunBash.enable()
		
		def checkout = """
			cd $jobdir
			para check
		""".bash()
		
		def numOK
		def numInBatch 
		def fields = checkout.split("\n")
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
	
	static def done(jobdir){
		def doneout = """
			cd $jobdir
			para stop
			rm -f batch* para*
			para freeBatch
		""".bash()
		return(doneout)
	}	
}
