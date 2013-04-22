package org.sagebionetworks.javadoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.print.attribute.standard.Fidelity;

import org.apache.commons.io.FileUtils;

/**
 * Copy the base files from the classpath.
 * 
 * @author John
 *
 */
public class CopyBaseFiles {

	
	public static void copyFiel(File outputDirectory) throws IOException{
		// Find the main CSS file
		File mainCSS = findFileOnClasspath("webapp/Main.css");
		// Get the parent folder
		FileUtils.copyDirectory(mainCSS.getParentFile(), outputDirectory, true);
	}
	
	
	/**
	 * Find a file on the classpath
	 * @param fileName
	 * @return
	 */
	public static File findFileOnClasspath(String fileName){
		URL url = CopyBaseFiles.class.getClassLoader().getResource(fileName);
		if(url == null) throw new IllegalArgumentException("Failed to find a File: "+fileName+" on the classpath");
		File file = new File(url.getFile().replaceAll("%20", " "));
		return file;
	}
	
	/**
	 * Load the HTML template file as a string.
	 * @return
	 * @throws IOException 
	 */
	public static String loadHTMLTemplateAsString() throws IOException{
		File templateFile = findFileOnClasspath("template.html");
		return FileUtils.readFileToString(templateFile);
	}

}
