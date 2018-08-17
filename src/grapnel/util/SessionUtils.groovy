package grapnel.util

import java.security.SecureRandom


class SessionUtils {
	
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static SecureRandom rnd = new SecureRandom();

	/**
	 * Takes a session token and generates a file name from it.  
	 * @param token
	 * @return
	 */
	static String getResultsFileNameFromToken(token,jobDir,prefix){
		// e.g. subdir = "/jobs"	
		//	System.err.println "SessionUtils.getResultsFileNameFromToken token:"+token.class
		//	String jobOutputPath = getSubdirPath("jobs/")
		def fileName
		if ((jobDir == null) || (jobDir == "")){
			fileName = "${prefix}${token}.results"	
		}else{
			fileName = "${jobDir}/${prefix}${token}.results"
		}			
		System.err.println "SessionUtils.getResultsFileNameFromToken: "+fileName
		return(fileName)	
	}
	

	static def generateResultsTokenAndFileName(jobDir,prefix){		
		// Probability of collision is very low, but non-zero, so 
		// build in something to try multiple times. 
		def ready = false;
		def token
		String fileName
		while(!ready){
			def tokenLength = 8
			token = generateSessionToken(tokenLength);
			fileName = getResultsFileNameFromToken(token,jobDir,prefix);
			def testFile = new File(fileName);
			if (!testFile.exists()) ready = true;
		}
		System.err.println "SU.generateResultsFileName token: $token"
		System.err.println "SU.generateResultsFileName fileName: $fileName"
		
		return [token,fileName]			
	}
			
		
	/**
	 * Generate a random token that has a low probability of collision. 
	 * @return
	 */
	static String generateSessionToken(len){		
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ){
		   sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
		}
		return sb.toString();
	}		
}


