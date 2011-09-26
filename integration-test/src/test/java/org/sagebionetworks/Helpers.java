package org.sagebionetworks;

/**
 * In .m2/settings.xml add something like the following specific to your
 * machine, see http://maven.apache.org/settings.html#Profiles for more info
 * 
 * <profiles>
 * 
 * ...
 * 
 * <properties>
 * 
 * <local.python27.path>/usr/local/bin/python2.7</local.python27.path>
 * 
 * <local.r.path>/usr/bin/R</local.r.path>
 * 
 * </properties>
 * 
 * ...
 * 
 * </profiles>
 * 
 * @author deflaux
 * 
 */
public class Helpers {

	/**
	 * @return the path to python2.7
	 */
	public static String getPython27Path() {
		return (null == System.getProperty("local.python27.path")) ? "python2.7"
				: System.getProperty("local.python27.path");
	}

	/**
	 * @return the path to R
	 */
	public static String getRPath() {
		return (null == System.getProperty("local.r.path")) ? "R" : System
				.getProperty("local.r.path");
	}

}
