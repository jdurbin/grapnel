package swiftml

/**
	A trained classifier plus any other information needed to apply it to new data. 
	- null background model. 
	- normalization
	- other preprocessing steps...
*/
class Model implements Serializable{
	def classifier 
	
	def Model(cl){
		classifier = cl
	}
}