package org.sagebionetworks.logging.s3;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the autowired bean that can be called from a timer.
 * 
 * @author John
 *
 */
public class LogSweeperFactoryImpl implements LogSweeperFactory {
	Logger log = (Logger) LogManager.getLogger(LogSweeperFactoryImpl.class);

	@Autowired
	private LogDAO logDAO;
	private long lockExpiresMs = 1000*30;
	private List<File> logDirectories;

	/**
	 * Injected via Spring
	 * 
	 * @param appendersToSweep
	 */
	public void setAppendersToSweep(List<String> appendersToSweep) {
		if(appendersToSweep == null) throw new IllegalArgumentException("Appends cannot be null");
		log.debug("appenders: "+appendersToSweep);
		// find the sweep directory
		LoggerContext context = (LoggerContext) LogManager.getContext();
		Map<String, Appender> map = context.getConfiguration().getAppenders();
		// Find each appender
		logDirectories = new LinkedList<File>();
		Set<String> directorySet = new HashSet<String>();
		for(String appenderName: appendersToSweep){
			Appender appender = map.get(appenderName);
			if(appender == null){
				throw new IllegalArgumentException("Cannot find the appender named: "+appenderName);
			}
			if(!(appender instanceof RollingFileAppender)){
				throw new IllegalArgumentException("Expected a RollingFileAppender for appender name: "+appenderName+" but found "+appender.getClass().getName());
			}
			RollingFileAppender rolling = (RollingFileAppender) appender;
			File logFile = new File(rolling.getFileName());
			File dir = logFile.getParentFile();
			if(directorySet.add(dir.getAbsolutePath())){
				logDirectories.add(dir);
			}
		}
	}

	@Override
	public List<File> getLogDirectories() {
		return logDirectories;
	}


	/**
	 * Injected via Spring
	 * @param lockExpiresMs
	 */
	public void setLockExpiresMs(long lockExpiresMs) {
		this.lockExpiresMs = lockExpiresMs;
	}


	/**
	 * This method can be called from a timer.
	 */
	@Override
	public List<String> sweepLogs() {
		if(logDirectories == null) throw new IllegalStateException("Must call setAppendersToSweep() to set valid appender names to sweep.");
		// This will contain all of the keys of any file created.
		List<String> keys = new LinkedList<String>();
		// Sweep each directory
		for(File directoryToSweep: logDirectories){
			// Sweep this directory
			LogSweeper sweeper =  new LogSweeper(directoryToSweep, lockExpiresMs, logDAO);
			keys.addAll(sweeper.sweepAllfiles());
		}
		// Create the sweeper and run it.
		return keys;
	}

}
