package durbin.util;

import javax.swing.*;
import java.awt.*;
import java.util.*;


import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.IOException;
import java.io.*;

/**
* Various utilities for handling images.  OK, right now just one utility, 
* save component as JPEG.  Easy enough to do GIF or PNG also.  
* 
* Supported Image types (Java 1.6 OS X): 
* [jpg, BMP, bmp, JPG, jpeg, wbmp, png, JPEG, PNG, WBMP, GIF, gif]
* 
*
*/ 
public class ImageUtils{
   
  public static void savePanelAsJPEG(JPanel p,String filename) throws IOException{
    savePanel(p,"jpeg",filename);
  } 
  
  public static void savePanelAsPNG(JPanel p,String filename) throws IOException{
    savePanel(p,"png",filename);
  } 
  
  public static void savePanelAsGIF(JPanel p,String filename) throws IOException{
    savePanel(p,"gif",filename);
  } 
     
  /**
  * I think it's more expensive to layout 1000 charts than it is to layout 1000 
  * already rendered, and so fixed, images.  We'll see...
  */ 
  public static Image getPanelImage(JPanel p){
    JFrame frame;
    frame = new JFrame();
    frame.setContentPane(p);
    frame.pack();

    Dimension size = p.getPreferredSize();
    BufferedImage image = new BufferedImage((int)size.width,(int)size.height, 
                                             BufferedImage.TYPE_INT_RGB);
    p.paint(image.createGraphics());
  
    return(image);
  }   
     
     
  public static void savePanel(JPanel p, String format, String filename) throws IOException {
    JFrame frame;
    frame = new JFrame();
    frame.setContentPane(p);
    frame.pack();

    Dimension size = p.getPreferredSize();
    BufferedImage image = new BufferedImage((int)size.width,(int)size.height, 
                                             BufferedImage.TYPE_INT_RGB);
    p.paint(image.createGraphics());

    ImageIO.write(image,format, new File (filename));
     
    frame.dispose();
  }    
}

/*
I think this works, but since I'm unsure, just leave it out for now...

public static void saveComponentAsJPEG(Component c, String filename, boolean subcomp) throws IOException {
    saveComponent(c, "jpeg", filename, subcomp);
}

public static void saveComponentAsPNG(Component c, String filename, boolean subcomp) throws IOException {
    saveComponent(c, "png", filename, subcomp);
}

public static void saveComponentAsGIF(Component c, String filename, boolean subcomp) throws IOException {
    saveComponent(c, "gif", filename, subcomp);
}

public static void saveComponentAsJPEG(Component c, String filename) throws IOException {
     saveComponent(c, "jpeg", filename, true);
 }

 public static void saveComponentAsPNG(Component c, String filename) throws IOException {
     saveComponent(c, "png", filename, true);
 }

 public static void saveComponentAsGIF(Component c, String filename) throws IOException {
     saveComponent(c, "gif", filename, true);
 }


public static void saveComponent(Component c, String format, String filename, boolean subcomp) throws IOException {
  
  Dimension size = c.getPreferredSize();
  BufferedImage image = new BufferedImage((int)size.width,(int)size.height, 
                                          BufferedImage.TYPE_INT_RGB);
                                          
  if (subcomp) c.paintAll(image.createGraphics());
  else c.paint(image.createGraphics());
  
  ImageIO.write(image,format, new File (filename));
}

*/


/*

    Dimension size = c.getPreferredSize();
            
    // Create a renderable image with the same width and height as the component
    //        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
    BufferedImage image = new BufferedImage((int)size.width,(int)size.height, 
                                            BufferedImage.TYPE_INT_RGB);

    if(subcomp) {
      // Render the component and all its sub components
//      c.paintAll(image.getGraphics());
      c.paintAll(image.createGraphics());            
    }else {
      // Render the component and ignoring its sub components
      //c.paint(image.getGraphics());
      c.paint(image.createGraphics());
    }
    
    // Save the image out to file
    ImageIO.write(image, format, new File(filename));
  }
  */