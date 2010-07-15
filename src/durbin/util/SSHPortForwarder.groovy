
package durbin.util;

import com.jcraft.jsch.*;

/**
* Class required for jsh to encapsulate getting password interactively. 
* Instantiated version here basically says, "No, don't get it interactively."
*/ 
class MyUserInfo implements UserInfo {
    public boolean promptYesNo (String str) { return false; }
    public boolean promptPassword (String str) { return false; }
    public boolean promptPassphrase (String str) { return false; }
    public void showMessage (String str) { return; }
    public String getPassword () { return null; }
    public String getPassphrase () { return null; }
}

/***********************************************************
* Class to encapsulate port forwarding using public key. 
* Client must have public/private keypair in .ssh/id_rsa[.pub]
* and server must have public key in .ssh/authorized_keys
* 
* Of course, problem with this approach is that it opens up the port for 
* the duration of the task.... yeah, but only for one user, right? So other 
* people running on the cluster can't take advantage of my mapped port unless
* they are logged in as me, right? 
* Easy enough to test with multiple-user accounts on my mac... just remove
* disconnect, go to second user and see if that person can log in, if so, 
* I'm vunerable to people on the same machine.   If that's the case, might
* have to step it up a notch with:
* http://markmail.org/message/22mh3hqm2chlondo
* 
* But only after someone says that it needs to be done. 
*
* Details found here: 
* http://forums.sun.com/thread.jspa?threadID=5232276
* http://benkstein.net/java/SSHSocketFactory/javadoc/
*
*/  
class SSHPortForwarder{

  def config;
  def jsch; 
  def session;
  
  /***************************************
  * 
  * user = "james"
  * host = "tcga1.cse.ucsc.edu"
  * lport = 3307
  * rhost = "127.0.0.1"
  * rport = 3306
  * 
  */ 
  def connect(user,host,rhost,lport,rport){
   
    config = new Hashtable<String,String>()
    config.put("StrictHostKeyChecking","no")
    config.put("X11Forwarding","no")
    config.put("BatchMode","no")

    // Use public key authentication...
    def key = System.getenv()['HOME']+"/.ssh/id_rsa"
    jsch = new JSch()
    jsch.addIdentity(key)

    session = jsch.getSession(user,host,22)
    session.setUserInfo(new MyUserInfo())
    session.setConfig(config)
    session.connect()
    int assinged_port = session.setPortForwardingL(lport, rhost, rport);
    System.err.println "localhost:"+assinged_port+" -> "+rhost+":"+rport+" on "+host
  }
  
  // If you don't put this in, it'll never exit...
  // What about exceptions?  Will crashed processes leave connection in place?
  def disconnect(){
    session.disconnect()
  }
}
