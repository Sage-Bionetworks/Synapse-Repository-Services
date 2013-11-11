package org.sagebionetworks.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads the client version information from a property file that is populated at build time.
 * 
 * @author John
 *
 */
public class ClientVersionInfo {
	
	/**
	 * Lazy static load.
	 *
	 */
	private static class Holder {
		private static String versionInfo = "";

		static {
			String fileName = "/java-client-version-info.properties";
			InputStream in = ClientVersionInfo.class
					.getResourceAsStream(fileName);
			if(in == null) throw new RuntimeException("Cannot find file on classpath: "+fileName);
			Properties prop = new Properties();
			try {
				prop.load(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String propName = "org.sagebionetworks.java.client.version";
			versionInfo = prop.getProperty(propName);
			if (versionInfo == null)
				throw new RuntimeException("Cannot find the property: "
						+ propName);
		}

		private static String getVersionInfo() {
			return versionInfo;
		}

	}

	/**
	 * Fetch the version info
	 * 
	 * @return
	 */
	public static String getClientVersionInfo() {
		return Holder.getVersionInfo();
	}
}
