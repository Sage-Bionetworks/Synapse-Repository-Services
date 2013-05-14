package org.sagebionetworks.profiler;

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
		return (log.isTraceEnabled() || log.isDebugEnabled() || log.isErrorEnabled());
	}

	@Override
	public void fireProfile(Frame data) {
		if (data.getElapse() < 2000) {
        		if (log.isTraceEnabled())
            			log.trace(data.toString());
		}
		else {
//            if (data.getElapse() < 2000) {
				if (log.isDebugEnabled())
					log.debug(data.toString());
//			}
//			else {
//				if (log.isErrorEnabled())
//					log.error(data.toString());
//			}
		}
	}

}
