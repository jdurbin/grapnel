package durbin.util;

/*
Maybe eventually something like this:

prepareMutFiles = [
	removeCommentsFromFiles : [files:MUTATION_FILES,inprefix:FREEZE,outprefix:TEMP],
	fixMafHeaders : [files:MUTATION_FILES,inprefix:TEMP,outprefix:PROCESSED]
]


prepareXMFFiles = [
	fixBobHeaders : [files:MUTATION_FILES,inprefix:TEMP,outprefix:PROCESSED]
]


all = [
	prepareMutFiles,
	prepareXMFFiles
]

So you invoke a target and it finds the target and executes all of the steps, 
verifying if output exists and/or timestamp differs... 

*/



/***************************
* Support for vaguely make-like functionality in a groovy script. 
* For starters, just the ability to dispatch commands and arguments from the 
* command line. 
*/
public class Make{
	
	static def runCommands(obj,args){		
		// Go through the arguments invoking each one in turn. 
		// This allows syntax like ./make clean mask blastz 
		// 
		args.each{arg->
			if (arg.contains(",")){
				def fields = arg.split(",")
				def methodName = fields[0]						
				def varName = fields[1]
				def varValue = obj."$varName"
				obj.invokeMethod(methodName,varValue)
			}else{
				def methodName = arg
				obj.invokeMethod(methodName,null)
			}
		}		
	}
	
	// Applies a closure to each input file writing the output
	// to a corresponding output file...
	static def applyToFiles(Map params,Closure c){
		def files = params.files
		def nil = ""
		files.each{file->			
			def outFileName = "${params.outprefix ?: nil}${file}${params.outsuffix ?: nil}"
			def out = new File(outFileName).withWriter{w->	
				def inFileName = "${params.inprefix ?: nil}${file}${params.insuffix ?: nil}"
				new File(inFileName).withReader{r->
					c(file,r,w)
				}
			}
		}
	}
	
	static def makeOutPath(Map params,file){
		def nil = ""
		def outFileName = "${params.outprefix ?: nil}${file}${params.outsuffix ?: nil}"
		return(outFileName)
	}
	
	static def makeInPath(Map params,file){
		def nil = ""
		def inFileName = "${params.inprefix ?: nil}${file}${params.insuffix ?: nil}"
		return(inFileName)
	}
	
	
	
	
/*
	myFunction(files:f,in_suffix:"_temp"){
		println it
	}
	def myFunction(Map params,Closure c){
		files = params.files
		nil = ""
		files.each{
			newName = "${params.in_prefix ?: nil}${it}${params.in_suffix ?: nil}"
			c(newName)
		}	
	}
	*/

		
}