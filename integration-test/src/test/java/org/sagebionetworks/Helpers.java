package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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

	/**
	 * @return the Synapse username to use for integration tests
	 */
	public static String getIntegrationTestUser() {
		return System.getProperty("org.sagebionetworks.integrationTestUser");
	}

	private static final Logger log = Logger.getLogger(Helpers.class.getName());

	/**
	 * This class is used to hold all relevant state we can capture about an
	 * externally run process
	 */
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
		
		@Override
		public String toString() {
			return "EXIT CODE: " + returnCode 
			+ "\nSTDOUT\n" + stdout
			+ "\nSTDERR\n" + stderr;
		}
	}

	/**
	 * @param cmd
	 * @return the ExternalProcessResult holding stdout, stderr, and the return
	 *         code
	 * @throws Exception
	 */
	public static ExternalProcessResult runExternalProcess(String cmd[])
			throws Exception {
		ExternalProcessResult result = new ExternalProcessResult();

		log.info("About to exec: " + StringUtils.join(cmd, " "));
		Runtime rt = Runtime.getRuntime();
		Process process = rt.exec(cmd, null, null);
		
		//Capture standard out and error simultaneously off two different threads so they can't block each other
		StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream()); 
		StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
		errorGobbler.start();
        outputGobbler.start();

        //Make sure process is complete
		int i = process.waitFor();
		outputGobbler.join();
		errorGobbler.join();
		
		//Capture and return the results
		result.setReturnCode(i);
		result.setStderr(errorGobbler.getOutput());
		result.setStdout(outputGobbler.getOutput());
		log.info("Completed exec: " + StringUtils.join(cmd, " ")
				+ ", " + result);
		
		assertEquals(0, result.getReturnCode());

		return result;
	}

	static class StreamGobbler extends Thread
	{
	    InputStream is;
	    StringBuilder s;
	    
	    StreamGobbler(InputStream is)
	    {
	        this.is = is;
	        s = new StringBuilder();
	    }
	    
	    public String getOutput() {
	    	return s.toString();
	    }
	    
	    @Override
		public void run() {
	    	InputStreamReader isr = null;
	    	BufferedReader br = null;
	        try {
	            isr = new InputStreamReader(is);
	            br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null) {
	                s.append(line).append("\n");
	            }
            } catch (IOException ioe) {
                log.warn(ioe); 
            } finally {
				try {
					is.close();
					if (isr != null)isr.close();
					if (br != null) br.close();
				} catch (IOException e) {
					log.warn(e);
				}
            }
	    }
	}
}
