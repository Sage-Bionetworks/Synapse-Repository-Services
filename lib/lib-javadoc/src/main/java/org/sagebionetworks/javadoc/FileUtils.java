package org.sagebionetworks.javadoc;

import java.io.File;
import java.io.IOException;

/**
 * Helpers for working with files.
 * 
 * @author John
 *
 */
public class FileUtils {
	
	/**
	 * Create a new File for the given class name.
	 * If the file already exists it will be deleted and re-created;
	 * @param outputDirectory
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	public static File createNewFileForClassName(File outputDirectory, String name, String suffix){
		if (outputDirectory == null)
			throw new IllegalArgumentException("The outputDirectory cannot be null");
		// Get he name of the file
		String fileName = getFileNameForClassName(name, suffix);
		File outputFile = new File(outputDirectory, fileName);
		// If the file alread exists then delete it
		if (outputFile.exists()) {
			if (!outputFile.delete()) {
				throw new IllegalStateException("Failed to overwrite the file: "+ outputFile.getAbsolutePath());
			}
		}
		// Create the new file
		outputFile.getParentFile().mkdirs();
		try {
			outputFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return outputFile;
	}
	
	/**
	 * Get the file name for a class name.
	 * @param className
	 * @return
	 */
	public static String getFileNameForClassName(String className, String suffix){
		return className.replaceAll("\\.", "/")+"."+suffix;
	}

}
