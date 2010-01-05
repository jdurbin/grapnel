
package durbin.weka;

import weka.core.*

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

import org.jfree.chart.plot.PlotOrientation as Orientation

import groovy.swing.SwingBuilder
import javax.swing.WindowConstants as WC
import org.jfree.chart.ChartUtilities
import org.jfree.chart.renderer.xy.XYErrorRenderer
import org.jfree.data.xy.YIntervalSeriesCollection
import org.jfree.chart.renderer.xy.DeviationRenderer
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import java.awt.*


/**
*  Class that encapsulates methods for plotting ROC, accuracy with errors, 
*  etc. Making plots with JFreeChart is easy enough, but these methods hide
*  most of the details for some routine plots we want to make and provide 
*  default settings that are suitable for the data we use. 
*/
class Plots{
  
  def err = System.err
  
  def width = 800
  def height = 600
  
  def alpha = 0.5  // For plots with shaded ranges. 
  def shapesAreVisible = true // Shapes with your lines?
  
  ArrayList<Shape> shapeList; // List of shapes to use for multiple plots...
  
  /**
  * Make a range renderer plot with multiple plots. 
  *
  **/ 
  def makeRangeRendererPNG(Collection<YIntervalSeries> series,chartName,xlabel,ylabel,outputName){
    // Create a chart....
    err.print "Creating chart..."
    def dataset = new YIntervalSeriesCollection()
    series.each{dataset.addSeries(it)}

    def r = new DeviationRenderer(true,false)
    //r.setSeriesStroke(0,new BasicStroke(3.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesStroke(0,new BasicStroke(1.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesFillPaint(0,new Color(255,200,200))
    r.setShapesVisible(shapesAreVisible)
    r.setAlpha(alpha)
    
    // TODO:  
    // 1.  Add facility to scale existing shapes... give scale factor, go through each series shape
    //     and scale it. 
    // 2.  Put in option to take our own list of pre-defined shapes.  
    // 
    // http://www.jfree.org/jcommon/api/org/jfree/util/ShapeUtilities.html
    //shape = new java.awt.Rectangle(-1,-1,2,2)
    //r.setBaseShape(shape)
    //r.setSeriesShape(0,shape)
    //println r.getBaseShape()  // java.awt.geom.Rectangle2D$Double[x=-3.0,y=-3.0,w=6.0,h=6.0]
    //println r.lookupSeriesShape(0) // java.awt.geom.Rectangle2D$Double[x=-3.0,y=-3.0,w=6.0,h=6.0]
    
    def xAxis = new NumberAxis(xlabel)
    def yAxis = new NumberAxis(ylabel)
    def plot = new XYPlot(dataset,xAxis, yAxis,r)
    plot.setBackgroundPaint(Color.WHITE);  // For the plot
    plot.setDomainGridlinesVisible(false);
    plot.setDomainCrosshairVisible(false);
    //plot.setRangeGridlinesVisible(false);
    //plot.setRangeCrosshairVisible(false);
    //println plot.isOutlineVisible()  
    plot.setOutlineVisible(false) // Tufte says no. 

    def chart = new JFreeChart(chartName,plot)
    chart.setBackgroundPaint(Color.WHITE) // For the while shebang...
    chart.setBorderVisible(false)
    err.println "done."

    //  OR save to PNG
    err.print "Saving png..."
    ChartUtilities.saveChartAsPNG(new File(outputName),chart,width,height)
    err.println "done."
  }
  
  
  /**
  * A range renderer plot is like an XY scatter plot with error bars, except 
  * instead of error bars it has a block rendered in a separate color.  Looks 
  * nice, but is a bit deceptive since it shows continuous lines for what is 
  * a discrete dataset. <br><br>
  * 
  * Values can be added to a YIntervalSeries like this: <br><br>
  * 
  * yintervalSeriesCollection.add(x,y,y-ymin,y+ymin)<br><br>
  * 
  * where the last two numbers define the width of the error bars. 
  *
  **/ 
  def makeRangeRendererPNG(YIntervalSeries series1,chartName,xlabel,ylabel,outputName){
    
    // Just stuff the series in a list and pass it to the multiple plot version. 
    def seriesList = []
    seriesList<<series1
    makeRangeRendererPNG(seriesList,chartName,xlabel,ylabel,outputName)
  }
   
  /**
  * Extracts an XY series corresponding to sensitivity/specificity from threshold curve. 
  * 
  * The attributes of this instance are: <br>
  * <ol start="0">
  * <li> 'True Positives' numeric
  * <li> 'False Negatives' numeric
  * <li> 'False Positives' numeric
  * <li> 'True Negatives' numeric
  * <li> 'False Positive Rate' numeric
  * <li> 'True Positive Rate' numeric
  * <li>  Precision numeric
  * <li>  Recall numeric
  * <li>  Fallout numeric
  * <li>  FMeasure numeric
  * <li>  Threshold numeric
  *</ol>
  */  
  def getXYSeriesFromThresholdCurve(Instances curve,SeriesID){
    // Want to plot the Sensitivity vs Specificity
    // Sensitivity = TN/(TN+FP)   Specificity = TP/(TP+FN)
    // Specificity = 1-FalsePositiveRate
    // Sensitivity = 1-FalseNegativeRate
    def TPIdx = 0
    def FNIdx = 1
    def FPIdx = 2
    def TNIdx = 3 
    def THRESHIdx = 10

    def tpArray = curve.attributeToDoubleArray(TPIdx) 
    def fnArray = curve.attributeToDoubleArray(FNIdx) 
    def fpArray = curve.attributeToDoubleArray(FPIdx) 
    def tnArray = curve.attributeToDoubleArray(TNIdx) 
    def threshArray = curve.attributeToDoubleArray(THRESHIdx)

    def series1 = new XYSeries(SeriesID)
    (0..<threshArray.length).eachWithIndex{threshold,i->
      def tp = tpArray[i]
      def fn = fnArray[i]
      def fp = fpArray[i]
      def tn = tnArray[i]

      def sensitivity = tn/(tn+fp)
      def specificity = tp/(tp+fn)

      series1.add(sensitivity,specificity)    
    }
    return(series1)
  }  
  
  /**
  * Creates an ROC plot with multiple ROC curves on one plot. 
  * TODO:  Make reference line dotted and faint. 
  */ 
  def plotMultipleROC(Collection<Instances> curves,java.util.List<String> seriesIDs,
      chartName,xlabel,ylabel,outputName){    

    // Reference
    def dataset = new XYSeriesCollection()    
    def lineseries = new XYSeries("Ref")
    lineseries.add(0,1)
    lineseries.add(1,0)
    dataset.addSeries(lineseries)

    def r = new XYLineAndShapeRenderer(true,true)
    r.setShapesVisible(shapesAreVisible)
    curves.eachWithIndex{curve,i->
      def idx = i+1 // Since we've already got series 0 as reference. 
      def series = getXYSeriesFromThresholdCurve(curve,seriesIDs[i])
      dataset.addSeries(series)   

      //r.setSeriesStroke(0,new BasicStroke(3.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
      r.setSeriesStroke(idx,new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
      //r.setSeriesFillPaint(idx,new Color(255,200,200))
      r.setSeriesShapesVisible(idx,shapesAreVisible)
      //def shape = new java.awt.Rectangle(-2,-2,4,4)
      //r.setSeriesShape(idx,shape)    
    }
    
    // Make the reference line faint and dotted...
    def vals = [5.0f, 3.0f] as float[]
    def dottedStroke = new BasicStroke(0.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,0.25f,
                                        vals, 0.0f)
    r.setSeriesStroke(0,dottedStroke);
    
    def xAxis = new NumberAxis(xlabel)
    def yAxis = new NumberAxis(ylabel)
    //xAxis.setRange(0,1.0)
    //yAxis.setRange(0,1.0)
    def plot = new XYPlot(dataset,xAxis, yAxis,r)
    plot.setBackgroundPaint(Color.WHITE);  // For the plot
    plot.setDomainGridlinesVisible(false);
    plot.setDomainCrosshairVisible(false);
    plot.setOutlineVisible(false)


    def chart = new JFreeChart(chartName,plot)
    chart.setBackgroundPaint(Color.WHITE) // For the while shebang...
    chart.setBorderVisible(false)
    err.println "done."  
  
    //  OR save to PNG
    err.print "Saving png to $outputName ..."
    ChartUtilities.saveChartAsPNG(new File(outputName),chart,width,height)
    err.println "done."  
  }
  
    
  /**
  * Creates an ROC plot given a curve generated by ThresholdCurve. <br><br>
  * TODO:  Make this just a special case of above. 
  */ 
  def plotROC(Instances curve,chartName,xlabel,ylabel,outputName){    
    series1 = getXYSeriesFromThresholdCurve(curve)
   
    def lineseries = new XYSeries("Chance Reference")
    lineseries.add(0,1)
    lineseries.add(1,0)

    def dataset = new XYSeriesCollection()
    dataset.addSeries(series1)
    dataset.addSeries(lineseries)

    def r = new XYLineAndShapeRenderer(true,true)
    //r.setSeriesStroke(0,new BasicStroke(3.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesStroke(0,new BasicStroke(1.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesFillPaint(0,new Color(255,200,200))
    r.setShapesVisible(shapesAreVisible)
    def shape = new java.awt.Rectangle(-2,-2,4,4)
    r.setSeriesShape(0,shape)

    def xAxis = new NumberAxis(xlabel)
    def yAxis = new NumberAxis(ylabel)
    //xAxis.setRange(0,1.0)
    //yAxis.setRange(0,1.0)
    def plot = new XYPlot(dataset,xAxis, yAxis,r)
    plot.setBackgroundPaint(Color.WHITE);  // For the plot
    plot.setDomainGridlinesVisible(false);
    plot.setDomainCrosshairVisible(false);
    plot.setOutlineVisible(false)


    def chart = new JFreeChart(chartName,plot)
    chart.setBackgroundPaint(Color.WHITE) // For the while shebang...
    chart.setBorderVisible(false)
    err.println "done."  
  
    //  OR save to PNG
    err.print "Saving png..."
    ChartUtilities.saveChartAsPNG(new File(outputName),chart,width,height)
    err.println "done."  
  }
  
}