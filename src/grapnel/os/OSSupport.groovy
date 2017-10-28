package grapnel.os;

import java.awt.*;
import javax.swing.*
import javax.imageio.ImageIO
import grapnel.util.*;

public class OSSupport{

	static def isOSX() {
		String osName = System.getProperty("os.name");
		return osName.contains("OS X");
	}

	static def setOSXLookAndFeel(scriptObject){

		// Manually load Apple specific class	
		// Workaround so that don't have to the platform-specific import
		// which would throw an error on non OS X machines. 
		def Application = scriptObject.class.classLoader.loadClass("com.apple.eawt.Application")
	
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ViewTable");
		// set the look and feel
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	
		// Set the dock icon. 
		//def scriptDir = new File(scriptObject.getClass().protectionDomain.codeSource.location.path).parent
		//def imgFileName = scriptDir+"/img/viewtab.png"
		//System.err.println "imgFileName: "+imgFileName
		//def dockImage = new ImageIcon(imgFileName).getImage();
		
		// This tells it to get it from the same .jar as the class OSSupport 
		def dockImage = ImageIO.read(OSSupport.class.getResource("/img/viewtab.png"));		
		//def dockImage = ImageIO.read(getClass().getResource("/img/viewtab.png"));		
		Application.getApplication().setDockIconImage(dockImage);
		
		// Let's see what other methods Application has...
		//println Application.metaClass.methods*.name.sort().unique() 
	}
}