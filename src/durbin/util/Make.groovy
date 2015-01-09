 package durbin.util;


/**
* Support for vaguely make-like functionality in a groovy script. <br><br>
* For starters it just adds the ability to dispatch commands and arguments 
* from the command line.  Coupled with the RunBash helper class this makes 
* it easy to script up short pipelines invoked by name.  For example, 
* the following script fragment, saved in a file like make.gv, could be used 
* to create or remove project directories: 
* 
* <pre>
* RunBash.enable()
* RunBash.bEchoCommand = true
* 
* projectDir = '/Users/james/project/'
* ROOTNAMES=['code','docs','libs'] 
* 
* def createDirs(){
* 	ROOTNAMES.each{name->
* 		 "mkdir $projectDirDir/$name".bash()
* 	}
* }
* 
* def delDirs(){
* 	ROOTNAMES.each{name->
* 		 "rm -rf $projectDirDir/$name".bash()
* 	}
*  }
*
* // Run targets by name... Has to be at end so that
* // all methods will have already been defined. 
* Make.runCommands(this,args)
* </pre>
* This could be executed like:
* <pre>
* make.gv createDirs
* </pre>
* or 
* <pre>
* make.gv delDirs
* </pre>
* 
*/
public class Make{
	
	/**
	* Runs the commands specified in args. 
	* 
	* @param obj   The object containing the methods (usually this script object)
	* @param args  The command line arguments to use. 
	*/ 
	def static runCommands(obj,args){							
		
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
				
				// If the method name is targets, print the targets, 
				// otherwise execute the command. 
				if (arg == "targets") printTargets(obj)
				else{				
					def methodName = arg
					obj.invokeMethod(methodName,null)
				}
			}
		}		
	}
	
	static def printTargets(obj){
		List<String> declaringClassOnlyMethods = obj.metaClass.methods.findAll { MetaMethod method ->
			if(method.declaringClass.name == obj.class.name) {
				method.name
			}
		}

		println "Available targets:"

		declaringClassOnlyMethods.each{
			if (it.name.contains("swapInit") || it.name.contains("main")|| it.name.contains("targets")) return;
			println "\t"+it.name
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