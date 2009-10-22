
// Some ideas about how I'd like to see table file and 
// other things work. 


// Read in data from a table...
data = new Table(args[0])
data.each{
  split(",")[0].toDouble()
}

// compute correlation of the two datasets
corr = Math.correlation(data['Score'],data['peakHeight'])


bin1 = data['Score'] as DynamicBin1D
bin2 = data['peakHeight'] as DynamicBin1D

corr = bin2.correlation(bin1)