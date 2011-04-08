package durbin.hg

import groovy.sql.Sql
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;
import weka.core.converters.*

import durbin.paradigm.*
import durbin.weka.*

/**
* Class to interact with UCSC hg database.  
*
*/
class hgDB{
	
	def db;
  def err = System.err // sugar

	def hgDB(confFile,port,dbName){
		db = newConnection(confFile,port,dbName);
	}

	/**
  * Make a connection to the database using user and password found in confFile, 
  * (e.g. .hg.conf)
  */ 
  def newConnection(confFile,port,dbName){
        
    // Look up user and pass from .hg.conf
    def env = System.getenv()
    def home = env['HOME']
    def props = new Properties()
    props.load(new FileInputStream("$home/$confFile")); 
    def user = props['db.user']
    def pass = props['db.password']

    //def server = "jdbc:mysql://127.0.0.1:3306/bioInt"
    def server = "jdbc:mysql://127.0.0.1:$port/$dbName"
    def dbdriver = "com.mysql.jdbc.Driver"
    def rvalDB = Sql.newInstance(server,user,pass,dbdriver)
    return(rvalDB)
  }

/**
* Lookup the gene description from geneSymbol
*/ 
def getGeneDescriptionFromName(geneSymbol){
	
	def desc
	def geneID
	def refseq

	// Look up IDs and stuff...
	def sql = """
	select * from kgXref where geneSymbol = $geneSymbol;
	"""

	def rows = db.rows(sql)
	if (rows.size() > 0){
		desc = new String(rows[0].description as byte[])
		geneID = rows[0].kgID
		refseq = rows[0].refseq
		geneSymbol = rows[0].geneSymbol
	}

	// Look up description
	sql = """
	select summary from refSeqSummary where refSeqSummary.mrnaAcc=$refseq
	"""
	rows = db.rows(sql)
	return(rows.summary)
}

}
