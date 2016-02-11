package swiftml;

class Classification{
	String instanceID
	def values = [:] // Distribution for values

	def call(){
		def max = -1
		def maxkey = ""
		values.each{k,v->
			if (v > max){
				max = v
				maxkey = k
			}
		}
		return(maxkey)
	}	
	
	String toString(){
		return("$instanceID\t"+call())
	}	
}