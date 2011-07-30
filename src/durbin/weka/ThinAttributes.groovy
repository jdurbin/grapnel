
package durbin.weka;

public class ThinAttributes{
	
	public double[][] data;
	
	ThinAttributes(double[][] data){
		this.data = data;
	}
	
	double[][] getAttributes(){
		return(this.data);
	}	
}