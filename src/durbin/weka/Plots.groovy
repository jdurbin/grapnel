
package durbin.weka;

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
import java.awt.*



/**
*  Class that encapsulates methods for plotting ROC, accuracy with errors, 
*  etc. Making plots with JFreeChart is easy enough, but these methods hide
*  most of the details for some routine plots we want to make. 
*/
class Plots{
  
  def err = System.err
  
  def plotwidth = 600
  def plotheight = 400
  
  
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
    // Create a chart....
    err.print "Creating chart..."
    def dataset = new YIntervalSeriesCollection()
    dataset.addSeries(series1)

    def r = new DeviationRenderer(true,false)
    //r.setSeriesStroke(0,new BasicStroke(3.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesStroke(0,new BasicStroke(1.0f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND))
    r.setSeriesFillPaint(0,new Color(255,200,200))
    //r.setShapesVisible(true)
    //r.setLinesVisible(false)

    xAxis = new NumberAxis(xlabel)
    yAxis = new NumberAxis(ylabel)
    plot = new XYPlot(dataset,xAxis, yAxis,r)
    plot.setBackgroundPaint(Color.WHITE);  // For the plot
    plot.setDomainGridlinesVisible(false);
    plot.setDomainCrosshairVisible(false);
    //plot.setRangeGridlinesVisible(false);
    //plot.setRangeCrosshairVisible(false);
    println plot.isOutlineVisible()  // Tufte says no. 
    plot.setOutlineVisible(false)

    chart = new JFreeChart(chartName,plot)
    chart.setBackgroundPaint(Color.WHITE) // For the while shebang...
    chart.setBorderVisible(false)
    err.println "done."

    //  OR save to PNG
    err.print "Saving png..."
    ChartUtilities.saveChartAsPNG(new File(outputName),chart,plotwidth,plotheight)
    err.println "done."
  }
  
  
  
  
}