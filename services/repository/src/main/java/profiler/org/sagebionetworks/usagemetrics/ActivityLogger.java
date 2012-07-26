package profiler.org.sagebionetworks.usagemetrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ActivityLogger {
	/**
	 * constant for nanosecond conversion to milliseconds
	 */
	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;

	private static Log log = LogFactory.getLog(ActivityLogger.class);

	private boolean shouldProfile;

	public static Log getLog() {
		return log;
	}

	public static void setLog(Log log) {
		ActivityLogger.log = log;
	}

	public boolean shouldProfile() {
		return shouldProfile;
	}

	public void setShouldProfile(boolean shouldProfile) {
		this.shouldProfile = shouldProfile;
	}

	public ActivityLogger() {
	}

}
