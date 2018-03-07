package org.sagebionetworks.profiler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This profiler logs data to the log.
 * 
 * @author jmhill
 * 
 */
public class LoggingProfiler implements ProfileHandler {

	static private Logger log = LogManager.getLogger(LoggingProfiler.class);
	private long elapseTimeThresholdMilis;

	public LoggingProfiler(long elapseTimeThresholdMilis){
		this.elapseTimeThresholdMilis = elapseTimeThresholdMilis;
	}

	@Override
	public boolean shouldCaptureProfile() {
		return (log.isTraceEnabled() || log.isDebugEnabled() || log
				.isErrorEnabled());
	}

	@Override
	public void fireProfile(Frame data) {
		if (data.getTotalTimeMilis() < elapseTimeThresholdMilis) {
			if (log.isTraceEnabled())
				log.trace(data.toString());
		} else {
			if (log.isDebugEnabled())
				log.debug(data.toString());

		}
	}

}
