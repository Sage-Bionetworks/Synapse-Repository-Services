package profiler.org.sagebionetworks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This profiler logs data to the log.
 * @author jmhill
 *
 */
public class LoggingProfiler implements ProfileHandler {
	
	static private Log log = LogFactory.getLog(LoggingProfiler.class);

	@Override
	public boolean shouldCaptureProfile(Object[] args) {
		return log.isTraceEnabled();
	}

	@Override
	public void fireProfile(Frame data) {
		log.trace(data.toString());
	}

}
