package org.sagebionetworks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;

/**
 * Note: This class in not GWT compatible and it outside of the GWT module which starts at org.sagebionetworks.model.
 * 
 * @author John
 *
 */
public class ResourceEncoder {

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		String helpMesssage = "Paramters: [resource] [destPropFile] [propertyName] \n" +
				"[resource] - The name of the resrouce to base-64 encode.\n" +
				"[destPropFile] - The destination property file.\n" +
				"[propertyName] - The name of the property that will be set with base-64 encoded resource";
		if(args == null) throw new IllegalArgumentException(helpMesssage);
		if(args.length != 3) throw new IllegalArgumentException(helpMesssage);
		String resource = args[0];
		String destfile = args[1];
		String propName = args[2];
		System.out.println("resource: "+resource);
		System.out.println("destPropFile: "+destfile);
		System.out.println("propName: "+propName);
		// First load the resources
		InputStream in = ResourceEncoder.class.getClassLoader().getResourceAsStream(resource);
		if(in == null) throw new IllegalArgumentException("Cannot find "+resource+" on the classpath");
		String fileString = ResourceUtils.readToString(in);
		byte[] encoded = Base64.encodeBase64(fileString.getBytes("UTF-8"));
		StringBuilder builder = new StringBuilder();
		builder.append("\"");
		String encodedString = new String(encoded, "UTF-8");
		builder.append(encodedString);
		builder.append("\"");
		File dest = new File(destfile);
		FileOutputStream fos = null;
		try{
			if(dest.exists()){
				System.out.println("File: "+dest.getAbsolutePath()+" already exists so it will be overwritten");
				dest.delete();
			}
			// Make sure all of the parent directories exist
			dest.getParentFile().mkdirs();
			// Create the file.
			System.out.println("Creating file: "+dest.getAbsolutePath());
			dest.createNewFile();
			// create the properteis object
			Properties props = new Properties();
			props.setProperty(propName, builder.toString());
			fos = new FileOutputStream(dest);
			props.store(fos, "Auto-generated property file");
			// done
			System.out.println("New property file created: "+dest.getAbsolutePath());
		}finally{
			if(fos != null) fos.close();
		}
	}
}
