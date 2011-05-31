package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * In .m2/settings.xml add something like the following specific to your
 * machine, see http://maven.apache.org/settings.html#Profiles for more info
 * 
 * <profiles> 
 *   ...
 *   <properties>
 *     <local.python27.path>/usr/local/bin/python2.7</local.python27.path>
 *     <local.r.path>/usr/bin/r</local.r.path>
 *   </properties>
 *   ...
 * </profiles>
 * 
 * @author deflaux
 * 
 */
public class Helpers {

	public static String getRepositoryServiceBaseUrl() {
		return System
				.getProperty("org.sagebionetworks.repository.service.base.url");
	}

	public static String getPortalBaseUrl() {
		return System.getProperty("org.sagebionetworks.portal.base.url");
	}

	public static String getPython27Path() {
		if(null == System.getProperty("local.python27.path")) {
			throw new Error("Missing properties from .m2/settings.xml, see the javadoc for details");
		}
		return System.getProperty("local.python27.path");
	}

	public static String getRPath() {
		if(null == System.getProperty("local.r.path")) {
			throw new Error("Missing properties from .m2/settings.xml, see the javadoc for details");
		}
		return System.getProperty("local.r.path");
	}

	private static final Logger log = Logger.getLogger(Helpers.class.getName());

	public static class ExternalProcessResult {

		int returnCode;
		String stdout;
		String stderr;

		/**
		 * @return the layer id for the layer newly created by this processing
		 *         step
		 */
		public int getReturnCode() {
			return returnCode;
		}

		/**
		 * @param returnCode
		 */
		public void setReturnCode(int returnCode) {
			this.returnCode = returnCode;
		}

		/**
		 * @return all output sent to stdout by this script
		 */
		public String getStdout() {
			return stdout;
		}

		/**
		 * @param stdout
		 */
		public void setStdout(String stdout) {
			this.stdout = stdout;
		}

		/**
		 * @return all output sent to stderr by this script
		 */
		public String getStderr() {
			return stderr;
		}

		/**
		 * @param stderr
		 */
		public void setStderr(String stderr) {
			this.stderr = stderr;
		}
	}

	/**
	 * @param cmd
	 * @return the ExternalProcessResult holding stdout, stderr, and the return code
	 * @throws Exception
	 */
	public static ExternalProcessResult runExternalProcess(String cmd[])
			throws Exception {
		ExternalProcessResult result = new ExternalProcessResult();

		log.info("About to exec: " + StringUtils.join(cmd, " "));
		Process process = Runtime.getRuntime().exec(cmd, null, null);

		String line;

		// Collect stdout from the script
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		StringBuilder stdoutAccumulator = new StringBuilder();
		while ((line = inputStream.readLine()) != null) {
			stdoutAccumulator.append(line);
			log.info(line);
		}
		inputStream.close();
		result.setStdout(stdoutAccumulator.toString());

		// Collect stderr from the script
		BufferedReader errorStream = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		StringBuilder stderrAccumulator = new StringBuilder();
		while ((line = errorStream.readLine()) != null) {
			stderrAccumulator.append(line);
			log.info(line);
		}
		errorStream.close();
		result.setStderr(stderrAccumulator.toString());

		result.setReturnCode(process.waitFor());
		log.info("Completed exec: " + StringUtils.join(cmd, " ") + ", exit code: "
				+ result.getReturnCode());
		assertTrue(0 == result.getReturnCode());

		return result;
	}

}
