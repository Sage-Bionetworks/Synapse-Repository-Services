package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sagebionetworks.javadoc.CopyBaseFiles;
import org.sagebionetworks.javadoc.linker.FileLink;
import org.sagebionetworks.javadoc.linker.Linker;
import org.sagebionetworks.javadoc.linker.PropertyRegExLinker;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
/**
 * Java Doclet for generating javadocs for Spring MVC web-services.
 * 
 * @author jmhill
 *
 */
public class SpringMVCDoclet {
	
	static Linker linker = new PropertyRegExLinker();
	static List<FileWriter> writers = new LinkedList<FileWriter>();
	static{
		writers.add(new SchemaWriter());
	}


	/**
	 * The main entry point of the Doclet
	 * @param root
	 * @return
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws JSONObjectAdapterException 
	 */
	public static boolean start(RootDoc root) throws Exception {
		// Pass this along to the standard doclet
		// First determine the output directory.
		File outputDirectory = getOutputDirectory(root.options());
		
		// Copy all of the base file to the output directory
		CopyBaseFiles.copyDirectory(outputDirectory);
		
		// Run all of the file writers
		List<FileLink> fileLinkes = new LinkedList<FileLink>();
		for(FileWriter writer: writers){
			fileLinkes.addAll(writer.writeAllFiles(outputDirectory, root));
		}
		// Link all of the files.
		linker.link(outputDirectory, fileLinkes);
		return true;
	}
	
	/**
	 * Get the output directory.
	 * @param options
	 * @return
	 */
	public static File getOutputDirectory(String[][] options){
		String outputDirectoryPath = Options.getOptionValue(options, Options.DIRECTORY_FLAG);
		if(outputDirectoryPath == null){
			outputDirectoryPath = System.getProperty("user.dir");
		}
		File outputDirectory = new File(outputDirectoryPath);
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}
		return outputDirectory;
	}
	
    /**
     * Check for doclet added options here.
     *
     * @return number of arguments to option. Zero return means
     * option not known.  Negative value means error occurred.
     */
    public static int optionLength(String option) {
    	return Options.optionLength(option);
    }

    /**
     * Check that options have the correct arguments here.
     * <P>
     * This method is not required and will default gracefully
     * (to true) if absent.
     * <P>
     * Printing option related error messages (using the provided
     * DocErrorReporter) is the responsibility of this method.
     *
     * @return true if the options are valid.
     */
    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
    	System.out.println("options:");
    	for(int i=0; i<options.length; i++){
    		for(int j=0; j<options[i].length; j++){
    			System.out.print(" "+options[i][j]);
    		}
    		System.out.println();
    	}
        return true;
    }
    
    /**
     * Indicate that this doclet supports the 1.5 language features.
     * @return JAVA_1_5, indicating that the new features are supported.
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }


}
