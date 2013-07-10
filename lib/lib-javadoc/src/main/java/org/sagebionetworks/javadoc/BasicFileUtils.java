package org.sagebionetworks.javadoc;

import java.io.File;
import java.io.IOException;

/**
 * Helpers for working with files.
 * 
 * @author John
 *
 */
public class BasicFileUtils {
	
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

	/**
	 * Build the path to the root folder for a given class name.
	 * @return
	 */
	public static String pathToRoot(String className){
		String[] split = className.split("\\.");
		return pathToRoot(split.length);
	}

	/**
	 * Path to root given a depths
	 * @param length
	 * @return
	 */
	public static String pathToRoot(int depths) {
		StringBuilder builder = new  StringBuilder();
		for(int i=0; i<depths-1; i++){
//			if(i > 0){
//				builder.append("/");
//			}
			builder.append("../");
		}
		return builder.toString();
	}
	
	/**
	 * Build the path to root for a given file.
	 * @param root
	 * @param file
	 * @return
	 */
	public static String pathToRoot(File root, File file){
		int depth = countDepth(root, file, 0);
		return pathToRoot(depth);
	}
	
	/**
	 * Recursively count the depth that the child is from the parent.
	 * @param root
	 * @param file
	 * @param depth
	 * @return
	 */
	private static int countDepth(File root, File child, int depth){
		depth++;
		if(root.equals(child.getParentFile())){
			return depth;
		}else{
			return countDepth(root, child.getParentFile(), depth);
		}
	}
}
