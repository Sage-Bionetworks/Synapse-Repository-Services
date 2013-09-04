package org.sagebionetworks.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper utility class for running processes externally and capturing their exit code and output.
 * 
 * @author deflaux
 * 
 */
public class ExternalProcessHelper {

	private static final Logger log = LogManager.getLogger(ExternalProcessHelper.class.getName());

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
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static ExternalProcessResult runExternalProcess(String cmd[]) throws IOException, InterruptedException
			 {
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
		int i = 1;
		boolean wasInterrupted = false;
		try {
			i = process.waitFor();
		} catch (InterruptedException e) {
			i = 1;
			wasInterrupted = true;
		}
		outputGobbler.join();
		errorGobbler.join();
		
		//Capture and return the results
		result.setReturnCode(i);
		result.setStderr(errorGobbler.getOutput());
		result.setStdout(outputGobbler.getOutput());
		log.info("Completed exec: " + StringUtils.join(cmd, " ")
				+", wasInterrputed=" + wasInterrupted
				+ ", " + result);
		
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
