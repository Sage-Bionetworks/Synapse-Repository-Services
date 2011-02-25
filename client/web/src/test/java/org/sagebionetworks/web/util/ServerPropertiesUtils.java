package org.sagebionetworks.web.util;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sagebionetworks.web.server.servlet.SearchServiceImpl;

/**
 * A simple utility for loading the server properties from the classpath.
 * 
 * @author jmhill
 *
 */
public class ServerPropertiesUtils {
	
	/**
	 * Helper to load the server properties off the classpath.
	 * @return
	 * @throws IOException
	 */
	public static Properties loadProperties() throws IOException{
		String propsFileName = "ServerConstants.properties";
		InputStream in = SearchServiceImpl.class.getClassLoader().getResourceAsStream(propsFileName);
		assertNotNull("Failed to load:"+propsFileName+" from the classpath", in);
		Properties props = new Properties();
		try{
			props.load(in);		
		}finally{
			if(in != null) in.close();
		}
		return props;
	}

}
