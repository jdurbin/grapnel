package durbin.util;

import java.util.*;
import java.io.*;
import java.lang.*;


// Parallel colt versions of these routines...
// I saw no speedup on MacBook Pro when using 
// these... would be interesting to try on a many core cpu.
// import cern.colt.matrix.tobject.*;
// import cern.colt.matrix.tobject.impl.*;
// import cern.colt.list.tdouble.*;

import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
import cern.colt.list.*;
import cern.colt.matrix.impl.AbstractMatrix2D;

import groovy.lang.*;
import groovy.lang.Closure;
import groovy.lang.IntRange;
import org.codehaus.groovy.runtime.*;


/***
* A row or a column in the table.   Specifically wrapper to make the 
* DenseDoubleMatrix1D returned by matrix.viewColumn/Row 
* into something that is iterable, since DenseDoubleMatrix1D doesn't
* implement iterable (probably because colt is old as dirt). <br>
* 
*/
//class DoubleVector extends DefaultGroovyMethodsSupport implements Iterable{
public class DoubleVector implements Iterable{
  
  DenseDoubleMatrix1D data;
  
	public DoubleVector(int n){
		data = new DenseDoubleMatrix1D(n);
	}

  public DoubleVector(DoubleMatrix1D dom){
    data = (DenseDoubleMatrix1D) dom;
  }
  
  public long size(){ return(data.size()); }
  public Double get(int idx){ return(data.get(idx)); }  
  public void set(int idx,Double value){ data.set(idx,value); }
  
  public DoubleVectorIterator iterator(){
    return(new DoubleVectorIterator(this));
  }

	public double sum(){
		return(data.zSum());
	}
	
	public double sd(){
		double sumDif = 0;
		double mean = mean();
		int N = (int) size();
		for(int i = 0;i < N;i++){
			sumDif += Math.pow((this.get(i) - mean),2);
		}
		sumDif = sumDif/(double)(N-1);
		double rval = Math.sqrt(sumDif);
		return(rval);
	}
	
	public double mean(){
		return(data.zSum()/(double)data.size());
	}
  
	public void putAt(int idx,double val){
		if (idx < 0) idx =(int) data.size()+idx; // 5 -1 = 4
		data.setQuick(idx,val);
	}

  public Double getAt(int idx){
    if (idx < 0) idx =(int) data.size()+idx; // 5 -1 = 4
    return(data.getQuick(idx));
  }

	public double[] asArray(){
		double[] rval = new double[(int)size()];
		data.toArray(rval);	
		return(rval);
	}
    
	public String toString(){
		return(data.toString());

/*
		for(int i = 0;i < size();i++){
			rval.append(getAt(i));
		}
		return(rval.toString());
		*/
	}
	
	public DoubleVector getRange(int start,int end){
		return(getAt(new IntRange(start,end)));
	}
  
  /****************************************************
  * Returns a view corresponding to the given range. 
  */ 
  public DoubleVector getAt(IntRange r){       
		int from = r.getFromInt();
		int to = r.getToInt();
		
		// when one value is negative, to and from will be reversed...
		
		//System.err.println("from: "+from); 
		//System.err.println("to: "+to);		
		
		if (from < 0) {
			int oldto = to;
			to =(int) data.size()+from; // 5 -1 = 4
			from = oldto;						
			if (from < 0){
				from = (int)data.size()+from;
			}
		}
		//System.err.println("from adjusted: "+from); 
		//System.err.println("to adjusted: "+to);		

    int start = from;
    int width = to-start+1; 

		//System.err.println("start: "+start); 
		//System.err.println("width: "+width);		

    return(new DoubleVector(data.viewPart(start,width)));
  }  
}
