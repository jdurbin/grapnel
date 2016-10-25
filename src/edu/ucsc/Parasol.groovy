package edu.ucsc

import grapnel.util.*

/****
* Minimal support for running parasol jobs on cluster. 
*/
class Parasol{
	
	static def checkDelay = 10000	// 10 seconds delay between status checks. 
	static def maxStaticChecks = 90 // If it goes 15 minutes without change, kill it.
	static def maxJobs = -1
	
	/**
	* Run parasol jobs...
	*/ 
	static def runjobs(jobname,jobdir){
		RunBash.enable()
		RunBash.bEchoCommand = false
		
		def paraCmd = "para push"
		if (maxJobs > 0) paraCmd = "para push -maxJob=$maxJobs"
		
		"""
		cd $jobdir
		para create $jobname
		$paraCmd 
		""".bash()	
		
		def numOK = 0
		def numInBatch = 9999
		def lastOK = numOK
		def lastInBatch = numInBatch
		def staticCount = 0
		while((numOK < numInBatch) && (staticCount < maxStaticChecks)){
			(numOK,numInBatch) = paraCheck(jobdir)
			
			if ((numOK == lastOK) && (lastInBatch == numInBatch)) staticCount++
			
			println "para numInBatch: $numInBatch \tnumOK: $numOK"
			sleep(checkDelay) 						
		}
		
		if (staticCount >= maxStaticChecks){
			System.err.println "WARNING: JOBS KILLED DUE TO BEING STATIC FOR TOO LONG."
		}
		
		// TODO:  detect hung jobs
		// TODO:  detect failed jobs and return some kind of error status
		
		done(jobdir)
	}
	
	static def paraCheck(jobdir){
		RunBash.enable()
		RunBash.bEchoCommand = false
		
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
		RunBash.enable()
		RunBash.bEchoCommand = false
		
		def doneout = """
			cd $jobdir
			para stop
			rm -f batch* para*
			para freeBatch
		""".bash()
		return(doneout)
	}	
}
