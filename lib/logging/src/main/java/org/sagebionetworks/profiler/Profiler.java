package org.sagebionetworks.profiler;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.repo.model.performance.PerformanceRecord;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.google.common.collect.Maps;

/**
 * This is a Profiler that logs the results.
 * 
 * @author jmhill
 * 
 */
@Aspect
public class Profiler {

	private static final Logger log = LogManager.getLogger(Profiler.class);
	private static final Logger callPerformanceLogger = LogManager.getLogger("org.sagebionetworks.profiler.call.performance");

	private static class Count {
		long totalTime = 0;
		int count = 0;
	}

	private static class ProfileData {
		Frame currentFrame;
		Map<String, Count> methodCalls = Maps.newHashMap(); // only used from data structure that is accessed via
															// ThreadLocal, so no need to be synchronized
	}

	private static class GlobalEntry {
		final String methodName;
		long totalTime = 0;
		int count = 0;

		public GlobalEntry(String methodName) {
			this.methodName = methodName;
		}
	}

	// Each thread gets its own stack.
	private static ThreadLocal<ProfileData> threadFrameStack = new ThreadLocal<ProfileData>() {
		@Override
		protected ProfileData initialValue() {
			return new ProfileData();
		}
	};

	private static ConcurrentMap<String, GlobalEntry> globalProfile = new ConcurrentHashMap<String, GlobalEntry>();
	private boolean shouldLogPerformance = false;

	private List<ProfileHandler> handlers = null;

	public List<ProfileHandler> getHandlers() {
		return handlers;
	}

	/**
	 * Injected via Spring.
	 * 
	 * @param handlers
	 */
	public void setHandlers(List<ProfileHandler> handlers) {
		this.handlers = handlers;
	}

	/**
	 * Injected via Spring.
	 * 
	 * @param handlers
	 */
	public void setShouldLogPerformance(boolean shouldLogPerformance) {
		this.shouldLogPerformance = shouldLogPerformance;
	}

	/**
	 * Should we even profile.
	 * 
	 * @param args
	 * @return
	 */
	private boolean shouldCaptureData(Object[] args) {
		if (handlers == null) {
			return false;
		}
		for (ProfileHandler handler : this.handlers) {
			if (handler.shouldCaptureProfile(args)) {
				return true;
			}
		}
		return false;
	}

	// execution(* org.sagebionetworks..*.*(..)) means profile any bean in the
	// package org.sagebionetworks or any sub-packages
	@Around("execution(* org.sagebionetworks..*.*(..)) && !within(org.sagebionetworks.profiler.*)")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {

		// Do nothing if loggin is not on
		if (!shouldCaptureData(pjp.getArgs())) {
			// Just proceed if logging is off.
			return pjp.proceed();
		}

		Signature signature = pjp.getSignature();
		Class<?> declaring = signature.getDeclaringType();

		// Do nothing if we don't know where we came from
		if (declaring == null) {
			return pjp.proceed();
		}

		ProfileData profileData = threadFrameStack.get();

		Frame parentFrame = profileData.currentFrame;

		// Set a new frame as the current frame.
		long startTime = System.nanoTime();
		String methodName = declaring.getName() + "." + signature.getName();
		Frame currentFrame = new Frame(startTime, methodName);
		if (parentFrame != null) {
			parentFrame.addChild(currentFrame);
		}

		profileData.currentFrame = currentFrame;

		// Now start the method.
		try {
			return pjp.proceed();
		} finally {
			long endTime = System.nanoTime();
			currentFrame.setEnd(endTime);
			// set current frame back to parent
			profileData.currentFrame = parentFrame;

			// mark average method
			Count methodCount = profileData.methodCalls.get(methodName);
			if (methodCount == null) {
				methodCount = new Count();
				profileData.methodCalls.put(methodName, methodCount);
			}
			methodCount.count++;
			methodCount.totalTime += (endTime - startTime);

			// Is there a previous frame?
			if (parentFrame == null) {
				// This should get replace with a logger.
				doFireProfile(currentFrame);
				if (shouldLogPerformance) {
					collectMethodCallCounts(profileData.methodCalls, methodCount.totalTime);
				}
				profileData.methodCalls.clear();
			}
		}
	}

	private void doFireProfile(Frame frame) {
		if (handlers != null) {
			for (ProfileHandler handler : this.handlers) {
				if (handler.shouldCaptureProfile(null)) {
					handler.fireProfile(frame);
				}
			}
		}
	}

	private void collectMethodCallCounts(Map<String, Count> methodCalls, long totalTime) {
		for (Entry<String, Count> entry : methodCalls.entrySet()) {
			String methodName = entry.getKey();
			int methodCount = entry.getValue().count;
			long methodTotalTime = entry.getValue().totalTime;

			// avoid always allocating new global entry by first trying get and only then putIfAbsent
			// over time, all methods will have entries and putIfAbsent will not need to be called anymore.
			GlobalEntry globalEntry = globalProfile.get(methodName);
			if (globalEntry == null) {
				globalProfile.putIfAbsent(methodName, new GlobalEntry(methodName));
				globalEntry = globalProfile.get(methodName);
			}

			synchronized (globalEntry) {
				globalEntry.count += methodCount;
				globalEntry.totalTime += methodTotalTime;
			}
		}
	}

	public void collectGlobalMethodCallCounts() {
		for (GlobalEntry globalEntry : globalProfile.values()) {
			int methodCount;
			long methodTotalTime;
			String methodName;
			synchronized (globalEntry) {
				methodName = globalEntry.methodName;
				methodCount = globalEntry.count;
				methodTotalTime = globalEntry.totalTime;
				globalEntry.count = 0;
				globalEntry.totalTime = 0;
			}
			if (methodCount > 0) {
				PerformanceRecord performanceRecord = new PerformanceRecord();
				performanceRecord.setMethod(methodName);
				performanceRecord.setTotalCount((long) methodCount);
				performanceRecord.setTotalTime(methodTotalTime);
				try {
					JSONObjectAdapter adapter = performanceRecord.writeToJSONObject(new JSONObjectAdapterImpl());
					callPerformanceLogger.info(adapter.toJSONString());
				} catch (JSONObjectAdapterException e) {
					log.debug("Cannot convert PerformanceRecord object to json: " + e.getMessage(), e);
				}
			}
		}
	}
}
