package durbin.charts;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.data.statistics.*;

/**
* Configuring JFreeChart charts is not hard, but it does clutter up your code. 
* Moreover, I tend to always want the same settings.  So this class just encapsulates
* the sort of standard version of a lot of charts I commonly create.  
*
*/ 
class BasicCharts{
  
  /**
  * Creates an XY series from a Groovy [:], aka LinkedHashMap
  */ 
  static createXYFromMap(LinkedHashMap data,String seriesName){
    XYSeries series1 = new XYSeries(seriesName);
    
    for(x in data.keySet()){
      def y = data.get(x);
      series1.add(x,y);
    }
        
    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series1);
    
    return(dataset);
  }
  
  
  /**
  * Returns a default line chart.  Not hard to do, but I like to just grab my standard
  * chart without having to set these things up each time. 
  */
  static lineChart(String title,LinkedHashMap data,int xsize,int ysize){            
    def xydata = createXYFromMap(data,"Series 1");    
    return(lineChart(title,xydata,xsize,ysize))
  }
  
  static lineChart(String title,XYSeriesCollection xydata,int xsize,int ysize){
    
    // create the chart...
    JFreeChart chart = ChartFactory.createXYLineChart(
        title,      // chart title
        "X",                      // x axis label
        "Y",                      // y axis label
        xydata,                  // data
        PlotOrientation.VERTICAL,
        true,                     // include legend
        false,                     // tooltips
        false                     // urls
    );

    // get a reference to the plot for further customisation...
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setDomainPannable(true);
    plot.setRangePannable(true);
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseShapesVisible(true);
    renderer.setBaseShapesFilled(true);

    // change the auto tick unit selection to integer units only...
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    ChartPanel chartPanel = new ChartPanel(chart);    
    chartPanel.setPreferredSize(new java.awt.Dimension(xsize,ysize));    

    return chartPanel;
  }
  
  static scatterPlot(String title,XYSeriesCollection xydata,int xsize,int ysize){
    return(scatterPlot(title,"X","Y",xydata,xsize,ysize))
  }
  
  static scatterPlot(String title,String xlabel,String ylabel, 
    XYSeriesCollection xydata,int xsize,int ysize){
    
    // Only show legend if there is more than one series. 
    def bShowLegend = false;
    if (xydata.getSeriesCount() > 1) bShowLegend = true;
    else bShowLegend = false;
    
    // create the chart...
    JFreeChart chart = ChartFactory.createScatterPlot(
        title,      // chart title
        xlabel,                      // x axis label
        ylabel,                      // y axis label
        xydata,                  // data
        PlotOrientation.VERTICAL,
        bShowLegend,                     // include legend
        false,                     // tooltips
        false                     // urls
    );

    // get a reference to the plot for further customisation...
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setDomainPannable(true);
    plot.setRangePannable(true);
    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseShapesVisible(true);
    renderer.setBaseShapesFilled(true);
    renderer.setBaseLinesVisible(false);

    // change the auto tick unit selection to integer units only...
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    ChartPanel chartPanel = new ChartPanel(chart);    
    chartPanel.setPreferredSize(new java.awt.Dimension(xsize,ysize));    

    return chartPanel;
  }
  
  
  static histogram(String title,HistogramDataset xydata,int xsize,int ysize){
    return(histogram(title,"Count","X",xydata,xsize,ysize))
  }
  
  static histogram(String title,String xlabel,String ylabel, 
    HistogramDataset xydata,int xsize,int ysize){
    
    // Only show legend if there is more than one series. 
    def bShowLegend = false;
    if (xydata.getSeriesCount() > 1) bShowLegend = true;
    else bShowLegend = false;
    
    // create the chart...
    JFreeChart chart = ChartFactory.createHistogram(
        title,      // chart title
        xlabel,                      // x axis label
        ylabel,                      // y axis label
        xydata,                  // data
        PlotOrientation.VERTICAL,
        bShowLegend,                     // include legend
        false,                     // tooltips
        false                     // urls
    );

    // get a reference to the plot for further customisation...
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setDomainPannable(true);
    plot.setRangePannable(true);
    def renderer = (XYBarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(false);
    renderer.setBarPainter(new StandardXYBarPainter());
    renderer.setShadowVisible(false);

    // change the auto tick unit selection to integer units only...
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    ChartPanel chartPanel = new ChartPanel(chart);    
    chartPanel.setPreferredSize(new java.awt.Dimension(xsize,ysize));    

    return chartPanel;
  }
  
  
     
  static barChart(String title,double[][] data,int xsize,int ysize){
    
  }
}