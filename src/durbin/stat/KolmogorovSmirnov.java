
package durbin.stat;

//import umontreal.iro.lecuyer.probdist.KolmogorovSmirnovDistQuick;

import umontreal.iro.lecuyer.probdist.*;

/** 
* Some functions for the Kolmogorov-Smirnov distribution and test. 
*
* Code translated from Cern ROOT (which is under LGPL): 
* 
* http://root.cern.ch/drupal/
* 
*/
public class KolmogorovSmirnov{

  /**
  * Calculates the Kolmogorov distribution function, which gives the 
  * probability that Kolmogorov's test statistic will exceed the value 
  * z assuming the null hypothesis.<br>
  *<pre>
  * This function returns the confidence level for the null hypothesis, where:
  *   z = dn*sqrt(n), and
  *   dn  is the maximum deviation between a hypothetical distribution
  *       function and an experimental distribution with
  *   n    events
  *
  * NOTE: To compare two experimental distributions with m and n events,
  *       use z = sqrt(m*n/(m+n))*dn
  *
  * This code is a direct translation of:
  *  
  * http://root.cern.ch/root/html/TMath.html#TMath:KolmogorovProb
  *</pre>
  */ 
  public static double prob(double z){    
    double[] fj = {-2,-8,-18,-32};
    //double fj[4] = {-2,-8,-18,-32}
    double[] r = new double[4];
    //double r[4];
    double w = 2.50662827;
    // c1 - -pi**2/8, c2 = 9*c1, c3 = 25*c1
    double c1 = -1.2337005501361697;
    double c2 = -11.103304951225528;
    double c3 = -30.842513753404244;
    
    double u = Math.abs(z);
    double p;
    if (u < 0.2) {
      p = 1;
    }else if (u < 0.755) {
      double v = 1./(u*u);
      p = 1 - w*(Math.exp(c1*v) + Math.exp(c2*v) + Math.exp(c3*v))/u;
    } else if (u < 6.8116) {
      r[1] = 0;
      r[2] = 0;
      r[3] = 0;
      double v = u*u;
      //int maxj = Math.max(1,TMath::Nint(3./u));
      int maxj = Math.max(1,(int)(3.0/u));
      for (int j=0; j<maxj;j++) {
        r[j] = Math.exp(fj[j]*v);
      }
      p = 2*(r[0] - r[1] +r[2] - r[3]);
    } else {
      p = 0;
    }
    return p;
  }
 
  
  /**  
  * Calculates the Kolmogorov Statistic D for two samples. 
  * 
  * <pre>
  * This code is a direct translation of:
  *  
  * http://root.cern.ch/root/html/TMath.html#TMath:KolmogorovProb
  * </pre>
  * Rather than have an option for distance/probability, I have two 
  * separate methods, kolmogorovDistance, kolmogorovProbability
  */ 
  public static double distance(double[] a,double[] b){
    double prob = Double.NaN;

    if ((a.length <=2) || (b.length <=2)){
      System.err.println("kolmogorovDist: Sets must have more than 2 points");
      return prob;
    }

    int na = a.length;
    int nb = b.length;

    double rna = a.length;
    double rnb = b.length;
    double sa = 1.0/rna;
    double sb = 1.0/rnb;
    double rdiff;
    int ia, ib;

    //     Starting values for main loop
    if (a[0] < b[0]) {
      rdiff = -sa;
      ia = 2;
      ib = 1;
    }else{
      rdiff = sb;
      ib = 2;
      ia = 1;
    }

    double rdmax = Math.abs(rdiff);

    //    Main loop over point sets to find max distance
    //    rdiff is the running difference, and rdmax the max.
    boolean ok = false;
    
    for (int i=0;i<(na+nb);i++) {
      int iam = ia-1;
      int ibm = ib-1;          
            
      if (a[iam] < b[ibm]) {
        rdiff -= sa;
        ia++;
        if (ia > na) {ok = true; break;}
      }else if (a[iam] > b[ibm]) {
        rdiff += sb;
        ib++;
        if (ib > nb) {ok = true; break;}
      } else {
        double x = a[ia-1];
        while(a[iam] == x && ia <= na) {
          rdiff -= sa;
          ia++;
        }
        
        while(b[ibm] == x && ib <= nb) {
          rdiff += sb;
          ib++;
        }
        if (ia > na) {ok = true; break;}
        if (ib > nb) {ok = true; break;}
      }
      rdmax = Math.max(rdmax,Math.abs(rdiff));
    }
    
    // Should never terminate this loop with ok = false!
    if (ok) {
      rdmax = Math.max(rdmax,Math.abs(rdiff));
      return(rdmax);
    }else{
      System.err.println("Kolmogorov Smirnov ERROR: Loop did not return OK");

      System.err.print("a: ");
      for(int i = 0;i < a.length;i++){System.err.print(a[i]);System.err.print(" ");}
      System.err.println("");
      
      System.err.print("b: ");
      for(int i = 0;i < b.length;i++){System.err.print(b[i]);System.err.print(" ");}
      System.err.println("");
      
      System.err.println("ia: "+ia);
      System.err.println("ib: "+ib);
      System.err.println("rdiff: "+rdiff);
      System.err.println("rna: "+rna);
      System.err.println("rnb: "+rnb);
      
      System.err.println("sa: "+sa);
      System.err.println("sb: "+sb);
      
      return(Double.NaN);
    }
  }
  
  
  /**
  * Statistical test whether two one-dimensional sets of points are compatible
  * with coming from the same parent distribution, using the Kolmogorov test.
  * That is, it is used to compare two experimental distributions of unbinned data.
  * <pre>
  * This code is a direct translation of:
  *  
  * http://root.cern.ch/root/html/TMath.html#TMath:KolmogorovProb
  * 
  * A copy of the notes from that site appear at the end of this file. 
  * Additional info about the Kolmogorov test here: 
  * 
  * http://root.cern.ch/root/html/TMath.html#TMath:KolmogorovProb
  * </pre>
  */ 
  public static double test(double[] a, double[] b){
    double d = distance(a,b);
    int rna = a.length;
    int rnb = b.length;
    
    double z = d * Math.sqrt(rna*rnb/(rna+rnb));
    int n = Math.max(a.length,b.length);
//    KolmogorovSmirnovDistQuick kdist = new KolmogorovSmirnovDistQuick(n);    
//    double prob = kdist.cdf(n,z);
    double prob = prob(z);
    return(prob);
  } 
  
  
  
  
  
  
  // ============================================================
  
  /**  
  * Calculates the signed Kolmogorov Statistic D for two samples. 
  * The signed statistic will be positive when the maximum deviation
  * point between the two CDF's occurs with a > b, and negative when 
  * the maximum deviation deviation point occurs with b > a.   This 
  * should let you not only see the difference in distributions, but
  * also the direction. 
  * 
  * 
  * <pre>
  * This code is modified from:
  *  
  * http://root.cern.ch/root/html/TMath.html#TMath:KolmogorovProb
  * </pre>
  */ 
  public static double signedDistance(double[] a,double[] b){
    double prob = Double.NaN;

    if ((a.length <=2) || (b.length <=2)){
      System.err.println("kolmogorovDist: Sets must have more than 2 points");
      return prob;
    }

    int na = a.length;
    int nb = b.length;

    double rna = a.length;
    double rnb = b.length;
    double sa = 1.0/rna;
    double sb = 1.0/rnb;
    double rdiff;
    int ia, ib;

    //     Starting values for main loop
    if (a[0] < b[0]) {
      rdiff = -sa;
      ia = 2;
      ib = 1;
    }else{
      rdiff = sb;
      ib = 2;
      ia = 1;
    }

    double rdmax = Math.abs(rdiff);
    double rdiffMax = rdiff;

    //  Main loop over point sets to find max distance rdiff is 
    //  the running difference, and rdmax the max.
    boolean ok = false;
    
    for (int i=0;i<na+nb;i++) {
      int iam = ia-1;
      int ibm = ib-1;          
            
      if (a[iam] < b[ibm]) {
        rdiff -= sa;
        ia++;
        if (ia > na) {ok = true; break;}
      }else if (a[iam] > b[ibm]) {
        rdiff += sb;
        ib++;
        if (ib > nb) {ok = true; break;}
      } else {
        double x = a[ia-1];
        while(a[iam] == x && ia <= na) {
          rdiff -= sa;
          ia++;
        }
        
        while(b[ibm] == x && ib <= nb) {
          rdiff += sb;
          ib++;
        }
        if (ia > na) {ok = true; break;}
        if (ib > nb) {ok = true; break;}
      }
      //      rdmax = Math.max(rdmax,Math.abs(rdiff));
      if (rdmax > Math.abs(rdiff)){
        rdmax = Math.abs(rdiff);
        rdiffMax = rdiff;
      }            
    }
    
    // Should never terminate this loop with ok = false!
    if (ok) {
       if (rdmax > Math.abs(rdiff)){
          rdmax = Math.abs(rdiff);
          rdiffMax = rdiff;
       }      
      //rdmax = Math.max(rdmax,Math.abs(rdiff));
      return(rdiffMax);
    }else{
      System.err.println("Kolmogorov Smirnov ERROR: Loop did not return OK");
      return(Double.NaN);
    }
  }
}







// ================= Additional Notes =====================================


//  The following notes refer to the C++ version, from which the Java 
//  translation here was constructed. 
// 
//  KolmogorovTest
//  Statistical test whether two one-dimensional sets of points are compatible
//  with coming from the same parent distribution, using the Kolmogorov test.
//  That is, it is used to compare two experimental distributions of unbinned data.
//
//  Input:
//  a,b: One-dimensional arrays of length na, nb, respectively.
//       The elements of a and b must be given in ascending order.
//  option is a character string to specify options
//         "D" Put out a line of "Debug" printout
//         "M" Return the Maximum Kolmogorov distance instead of prob
//
//  Output:
// The returned value prob is a calculated confidence level which gives a
// statistical test for compatibility of a and b.
// Values of prob close to zero are taken as indicating a small probability
// of compatibility. For two point sets drawn randomly from the same parent
// distribution, the value of prob should be uniformly distributed between
// zero and one.
//   in case of error the function return -1
//   If the 2 sets have a different number of points, the minimum of
//   the two sets is used.
//
// Method:
// The Kolmogorov test is used. The test statistic is the maximum deviation
// between the two integrated distribution functions, multiplied by the
// normalizing factor (rdmax*sqrt(na*nb/(na+nb)).
//
//  Code adapted by Rene Brun from CERNLIB routine TKOLMO (Fred James)
//   (W.T. Eadie, D. Drijard, F.E. James, M. Roos and B. Sadoulet,
//      Statistical Methods in Experimental Physics, (North-Holland,
//      Amsterdam 1971) 269-271)
//
//  Method Improvement by Jason A Detwiler (JADetwiler@lbl.gov)
//  -----------------------------------------------------------
//   The nuts-and-bolts of the TMath::KolmogorovTest() algorithm is a for-loop
//   over the two sorted arrays a and b representing empirical distribution
//   functions. The for-loop handles 3 cases: when the next points to be
//   evaluated satisfy a>b, a<b, or a=b:
//
//      for (Int_t i=0;i<na+nb;i++) {
//         if (a[ia-1] < b[ib-1]) {
//            rdiff -= sa;
//            ia++;
//            if (ia > na) {ok = kTRUE; break;}
//         } else if (a[ia-1] > b[ib-1]) {
//            rdiff += sb;
//            ib++;
//            if (ib > nb) {ok = kTRUE; break;}
//         } else {
//            rdiff += sb - sa;
//            ia++;
//            ib++;
//            if (ia > na) {ok = kTRUE; break;}
//            if (ib > nb) {ok = kTRUE; break;}
//        }
//         rdmax = TMath::Max(rdmax,TMath::Abs(rdiff));
//      }
//
//   For the last case, a=b, the algorithm advances each array by one index in an
//   attempt to move through the equality. However, this is incorrect when one or
//   the other of a or b (or both) have a repeated value, call it x. For the KS
//   statistic to be computed properly, rdiff needs to be calculated after all of
//   the a and b at x have been tallied (this is due to the definition of the
//   empirical distribution function; another way to convince yourself that the
//   old CERNLIB method is wrong is that it implies that the function defined as the
//   difference between a and b is multi-valued at x -- besides being ugly, this
//   would invalidate Kolmogorov's theorem).
//
//   The solution is to just add while-loops into the equality-case handling to
//   perform the tally:
//
//         } else {
//            double x = a[ia-1];
//            while(a[ia-1] == x && ia <= na) {
//              rdiff -= sa;
//              ia++;
//            }
//            while(b[ib-1] == x && ib <= nb) {
//              rdiff += sb;
//              ib++;
//            }
//            if (ia > na) {ok = kTRUE; break;}
//            if (ib > nb) {ok = kTRUE; break;}
//         }
//
//  NOTE1
//  A good description of the Kolmogorov test can be seen at:
//    http://www.itl.nist.gov/div898/handbook/eda/section3/eda35g.htm
