package org.sagebionetworks.sweeper.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.rolling.RollingPolicyBase;
import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.TriggeringPolicy;
import org.apache.log4j.spi.LoggingEvent;

public abstract class RollingTriggeringPolicy extends RollingPolicyBase
		implements TriggeringPolicy {

	@Override
	public RolloverDescription initialize(String file, boolean append)
			throws SecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RolloverDescription rollover(String activeFile)
			throws SecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isTriggeringEvent(Appender appender, LoggingEvent event,
			String filename, long fileLength) {
		// TODO Auto-generated method stub
		return false;
	}

}
