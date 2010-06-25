
package durbin.bioInt

import groovy.sql.Sql

/**
* Class to create a set of instances from the bioInt database. 
*
*/
class BioIntLoader{
  
  def db;
  
  /**
  * Make a connection to the database using user and password found in .hg.conf
  *
  */ 
  def newConnection(){
        
    // Look up user and pass from .hg.conf
    def env = System.getenv()
    def home = env['HOME']
    def props = new Properties()
    props.load(new FileInputStream("$home/.hg.conf")); 
    def user = props['central.user']
    def pass = props['central.password']

    def server = "jdbc:mysql://127.0.0.1:3306/bioInt"
    def dbdriver = "com.mysql.jdbc.Driver"
    db = Sql.newInstance(server,user,pass,dbdriver)
    return(db)
  }
  
}