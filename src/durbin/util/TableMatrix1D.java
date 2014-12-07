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

//import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.RangeInfo;

/***
* An iterator for a row or a column of a table
*/
class TableMatrix1DIterator implements Iterator{
  TableMatrix1D table;
  int idx = 0;
  
  TableMatrix1DIterator(TableMatrix1D t){
    table = t;
  }
  
  public boolean hasNext(){
    if (idx < table.size()) return(true);
    else return(false);
  }
  
  public Object next(){
    Object rval = table.get(idx);
    idx++;
    return(rval);
  }
  public void remove(){}
}

/***
* A row or a column in the table.   Specifically wrapper to make the 
* ObjectMatrix1D returned by matrix.viewColumn/Row 
* into something that is iterable, since ObjectMatrix1D doesn't
* implement iterable (probably because colt is old as dirt). <br>
* 
* KJD: I've gone off the deep end here... I'm sure this is not
* the Right Way to do this, although it works...
* all of this probably should have been handled  from the Table.groovy 
* wrapper, using some kind of expando magic or some such...<br>
* 
* Ugh... even worse, all this hasn't helped performance compared to 
* getting a copy of the array for each row... 
*/
class TableMatrix1D extends DefaultGroovyMethodsSupport implements Iterable{
  
  ObjectMatrix1D data;
	public HashMap<String,Integer> names2Idx;
	public String name;
  
  //public TableMatrix1D(ObjectMatrix1D dom){
  //  data = dom;
  //}

	public TableMatrix1D(ObjectMatrix1D dom,HashMap<String,Integer> n2I,String theName){
		names2Idx = n2I;
		name = theName;
    data = dom;
  }

	public Object asType(Class clazz) {
		if (clazz.equals(java.util.ArrayList.class)) {			
			ArrayList rval = new ArrayList();
			for(int i = 0;i < data.size();i++){
				rval.add(data.get(i));
			}
			return(rval);			
		}else if ((clazz.equals(java.util.Set.class) ||
							 clazz.equals(java.util.HashSet.class))){
			HashSet rval = new HashSet();
			for(int i = 0;i < data.size();i++){
				rval.add(data.get(i));
			}
			return(rval);
		}else{
			String msg = "Can't cast TableMatrix1D to "+clazz;
			throw new ClassCastException(msg);
		}
	}

	
  
  public long size(){ return(data.size()); }
  public Object get(int idx){ return(data.get(idx)); }  
  public void set(int idx,Object value){ data.set(idx,value); }
  

  public TableMatrix1DIterator iterator(){
    return(new TableMatrix1DIterator(this));
  }

	public Object getAt(String colName){
    int cidx = names2Idx.get(colName);
		//System.err.println("colName:"+colName+" index: "+cidx);
    return(data.get(cidx));
  }
  
  public Object getAt(int idx){
    if (idx < 0) idx =(int) data.size()+idx; // 5 -1 = 4
    return(data.get(idx));
  }

	public double[] toDoubleArray(){
		double[] rval = new double[(int)size()];
		for(int i =0;i < size();i++){
			rval[i] = (Double) getAt(i);
		}
		return(rval);
	}
    
	public String toString(){
		StringBuffer rval = new StringBuffer();
		for(int i = 0;i < size();i++){
			rval.append(getAt(i));
		}
		return(rval.toString());
	}
  
  /****************************************************
  * Returns a view corresponding to the given range. 
  */ 
  public TableMatrix1D getAt(IntRange r){
    
    // KJD:  This range is coming in with from/to swapped, but why? 
    // HACK ALERT: Until I understand why, I'm just going to swap them 
    // back... so there...  
    IntRange r2 = new IntRange(r.getToInt(),r.getFromInt());
    
    // Convert Groovy relative range values (e.g. -2) into actual 
    // range numbers...
    RangeInfo ri = DefaultGroovyMethodsSupport.subListBorders((int)this.size(),r2);        
    int start = ri.from;
    int width = ri.to-start; 
    return(new TableMatrix1D(data.viewPart(start,width),names2Idx,name));
  }  
}