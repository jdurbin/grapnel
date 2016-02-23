package swiftml

/**
	A trained classifier plus any other information needed to apply it to new data. 
	- null background model. 
	- normalization
	- other preprocessing steps...
*/
class Model implements Serializable{
	static final long serialVersionUID = 1L; 
	
	def classifier 
	def attributes
	def className
	
	def Model(cl,className,attr){
		classifier = cl
		className = cn
		attributes = attr
	}
}