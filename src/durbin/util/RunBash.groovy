package durbin.util

import java.io.InputStream;

/**
* Run a bash command line from groovy.  You can already run commands with syntax like:<br>
* <pre>
* "ls -l".execute()
* </pre>
* That is about as simple as it gets and works great most of the time.  However, execute() runs 
* the given command passing it the list of options, the options are NOT passed through bash for 
* expansion and so on.  This class is an attempt to get around some of the rough edges of execute().  
*/ 
class RunBash{
  
  // Is there another way to do this?
  // Add a bash() method to GString and String 
  static def enable(){
    GString.metaClass.bash = {->
      RunBash.bash(delegate)
    }
    
    String.metaClass.bash = {->
        RunBash.bash(delegate)
    }    
  }
  
  static def bash(cmd){

    cmd = cmd as String

    // create a process for the shell
    ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
    pb.redirectErrorStream(true); // use this to capture messages sent to stderr
    Process shell = pb.start();
    InputStream shellIn = shell.getInputStream(); // this captures the output from the command
    int shellExitStatus = shell.waitFor(); // wait for the shell to finish and get the return code

    // at this point you can process the output issued by the command
    // for instance, this reads the output and writes it to System.out:
    int c;
    while ((c = shellIn.read()) != -1){
      System.out.write(c);
    }

    // close the stream
    try {shellIn.close();} catch (IOException ignoreMe) {}
  }
}