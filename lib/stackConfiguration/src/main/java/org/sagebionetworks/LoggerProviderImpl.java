package org.sagebionetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerProviderImpl implements LoggerProvider {

	@Override
	public Logger getLogger(String className) {
		return LogManager.getLogger(className);
	}

}
