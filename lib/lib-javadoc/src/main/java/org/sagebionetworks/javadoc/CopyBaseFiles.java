package org.sagebionetworks.javadoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Copy the base files from the classpath.
 * 
 * @author John
 * 
 */
public class CopyBaseFiles {

	public static void copyDirectory(File outputDirectory) throws IOException {
		// Copy the files from the zip that serve as the bass of the web-app
		String fileName = "webapp.zip";
		InputStream in = CopyBaseFiles.class.getClassLoader().getResourceAsStream(fileName);
		if (in == null) throw new IllegalArgumentException("Failed to find a File: "+ fileName + " on the classpath");
		copyZipToDirectory(outputDirectory, in);
	}

	/**
	 * Copy the contents of a zip file to the given directory.
	 * @param outputDirectory
	 * @param in
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static void copyZipToDirectory(File outputDirectory, InputStream in) throws IOException, FileNotFoundException {
		ZipInputStream zis = new ZipInputStream(in);
		try {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File out = new File(outputDirectory, entry.getName());
				if(!entry.isDirectory()){
					out.createNewFile();
					FileOutputStream output = new FileOutputStream(out);
					try {
						IOUtils.copy(zis, output);
					} finally {
						IOUtils.closeQuietly(output);
					}
				}else{
					// This is a directory so make sure the file path exits.
					out.mkdirs();
				}
			}
		} finally {
			IOUtils.closeQuietly(zis);
		}
	}

	/**
	 * Find a file on the classpath
	 * 
	 * @param fileName
	 * @return
	 */
	public static File findFileOnClasspath(String fileName) {
		URL url = CopyBaseFiles.class.getClassLoader().getResource(fileName);
		if (url == null)
			throw new IllegalArgumentException("Failed to find a File: "
					+ fileName + " on the classpath");
		File file = new File(url.getFile().replaceAll("%20", " "));
		return file;
	}

	/**
	 * Load the HTML template file as a string.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String loadHTMLTemplateAsString() throws IOException {
		File templateFile = findFileOnClasspath("template.html");
		return FileUtils.readFileToString(templateFile);
	}

}
