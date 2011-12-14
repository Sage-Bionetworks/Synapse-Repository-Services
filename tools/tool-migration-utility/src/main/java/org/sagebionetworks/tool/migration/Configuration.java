package org.sagebionetworks.tool.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides configuration information 
 * @author John
 *
 */
public class Configuration {
	
	static private Log log = LogFactory.getLog(Configuration.class);
	public static final String CONFIGUATION_TEMPLATE_PROPERTIES = "configuation-template.properties";
	
	/**
	 * The the passed configuration file and add the properties to the system properties.
	 * @param path
	 * @throws IOException
	 */
	public static void loadConfigurationFile(String path) throws IOException{
		log.debug("Using configuation file: "+path);
		Properties props = new Properties();
		File file = new File(path);
		if(!file.exists()){
			throw new IllegalArgumentException("The configuartion file: "+path+" does not exist");
		}
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(file);
			props.load(fis);
		}finally{
			fis.close();
		}
		// Add all of these properties to the system properties.
		System.getProperties().putAll(props);
		validateConfigurationProperties();
	}

	/**
	 * Validate the properties against the command line.
	 * @throws IOException
	 */
	public static void validateConfigurationProperties() throws IOException{
		// Load the template from the classpath
		InputStream in = MigrationDriver.class.getClassLoader().getResourceAsStream(CONFIGUATION_TEMPLATE_PROPERTIES);
		if(in == null) throw new IllegalArgumentException("Cannot find: "+CONFIGUATION_TEMPLATE_PROPERTIES+" on the classpath");
		Properties template = new Properties();
		try{
			template.load(in);
		}finally{
			in.close();
		}
		// Compare the passed properties to the template
		Iterator<String> keyIt = template.stringPropertyNames().iterator();
		Properties props = System.getProperties();
		log.debug("Loading the following properties: ");
		while(keyIt.hasNext()){
			String key = keyIt.next();
			Object value = props.getProperty(key);
			if(value == null){
				throw new IllegalArgumentException("Cannot find property: "+props.getProperty(key));
			}
			if(key.indexOf("password")> 1){
				// Do not print passwords
				log.debug(key+"="+createObfuscatedPassword((String) value));
			}else{
				log.debug(key+"="+value);
			}
		}
	}
	
	/**
	 * Used to print Obfuscated Password
	 * @param password
	 * @return
	 */
	private static String createObfuscatedPassword(String password){
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<password.length(); i++){
			builder.append("*");
		}
		return builder.toString();
	}
	
	/**
	 * Get the source connection information.
	 * @return
	 */
	public static SynapseConnectionInfo getSourceConnectionInfo(){
		return new SynapseConnectionInfo(
					System.getProperty("org.sagebionetworks.source.authentication.endpoint"),
					System.getProperty("org.sagebionetworks.source.repository.endpoint"),
					System.getProperty("org.sagebionetworks.source.admin.username"),
					System.getProperty("org.sagebionetworks.source.admin.password")
				);
	}
	
	/**
	 * Get the destination connection information.
	 * @return
	 */
	public static SynapseConnectionInfo getDestinationConnectionInfo(){
		return new SynapseConnectionInfo(
					System.getProperty("org.sagebionetworks.destination.authentication.endpoint"),
					System.getProperty("org.sagebionetworks.destination.repository.endpoint"),
					System.getProperty("org.sagebionetworks.destination.admin.username"),
					System.getProperty("org.sagebionetworks.destination.admin.password")
				);
	}
	

	/**
	 * The maximum number of threads to be used.
	 * @return
	 */
	public static int getMaximumNumberThreads() {
		return Integer.parseInt(System.getProperty("org.sagebionetworks.max.threads"));
	}
	
	/**
	 * The Maximum batch size.
	 * @return
	 */
	public static int getMaximumBatchSize(){
		return Integer.parseInt(System.getProperty("org.sagebionetworks.batch.size"));
	}
	
	public static long getWorkerTimeoutMs(){
		return Long.parseLong(System.getProperty("org.sagebionetworks.worker.thread.timout.ms"));
	}
}
