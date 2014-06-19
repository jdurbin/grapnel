#!/usr/bin/env groovy 


/***
* Set that counts occurrences of each elment. <br><br>
*
* Should probably be renamed to CounterSet. 
* 
* <pre>
* cm = new CounterMap()
* 
* cm.inc('bob')
* cm.inc('bob')
* cm.inc('mary')
* 
* assert cm['bob'] == 2
* assert cm['mary'] == 1
* </pre>
* 
* Eventually I'd like to do something where this syntax works whether
* or not 'bob' has already been added to the map:
* <pre>
* cm = new CounterMap()
*
* cm['bob']++
* </pre>
* Not sure if that is even possible because getAt() has no way to know
* if it's being invoked in a case where the value should be supplied or
* where the value will be operated on by math. 
* 
* In the mean time, will just have the simpler to implement:
*
* cm.inc('bob')
*/


class CounterMap extends HashMap{
	
	def inc(key){
		if (this.containsKey(key)){
			int value = this.get(key)
			value = value+1
			this.put(key,value)
		}else{
			this.put(key,1)
		}
	}		
	
	// return a default value if none given...
	def getAt(key){
		if (this.containsKey(key)){ 
			return(this.get(key))
		}else{
			return(0)
		}
	}
	
	// There's already a properties based getAt, so this
	// overrides that specifically for strings. 
	def getAt(String key){
		if (this.containsKey(key)){ 
			return(this.get(key))
		}else{
			return(0)
		}
	}
	
	
}

