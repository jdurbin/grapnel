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
* An iterator for a row or a column of a table
*/
public class DoubleVectorIterator implements Iterator{
  DoubleVector table;
  int idx = 0;
  
  DoubleVectorIterator(DoubleVector t){
    table = t;
  }
  
  public boolean hasNext(){
    if (idx < table.size()) return(true);
    else return(false);
  }
  
  public Double next(){
    Double rval = table.get(idx);
    idx++;
    return(rval);
  }
  public void remove(){}
}
