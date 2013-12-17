package org.sagebionetworks.profiler;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
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

	private static final long CUTOFF_FOR_LOGGING_IN_NANOS = 1L * 1000L * 1000L;
	private static final long GLOBAL_LOGGING_INTERVAL_IN_NANOS = 60L * 1000L * 1000L * 1000L;
	
	private static class Count {
		long totalTime = 0;
		int count = 0;
	}

	private static class ProfileData {
		Frame currentFrame;
		Map<String, Count> methodCalls = Maps.newHashMap();
	}

	private static class GlobalEntry {
		String methodName;
		long totalTime = 0;
		int count = 0;
	}

	// Each thread gets its own stack.
	private static ThreadLocal<ProfileData> threadFrameStack = new ThreadLocal<ProfileData>() {
		@Override
		protected ProfileData initialValue() {
			return new ProfileData();
		}
	};

	private static Cache<String, GlobalEntry> globalProfile = CacheBuilder.newBuilder().build(new CacheLoader<String, GlobalEntry>() {
		@Override
		public GlobalEntry load(String methodName) throws Exception {
			GlobalEntry entry = new GlobalEntry();
			entry.methodName = methodName;
			return entry;
		}
	});

	private static AtomicLong lastProfileTime = new AtomicLong(System.nanoTime());

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
				collectMethodCallCounts(profileData.methodCalls, methodCount.totalTime);
				profileData.methodCalls.clear();

				long lastTime = lastProfileTime.get();
				if (endTime - lastTime > GLOBAL_LOGGING_INTERVAL_IN_NANOS) {
					if (lastProfileTime.compareAndSet(lastTime, endTime)) {
						// only one thread will succeed here (unless a call exceeds 1 minute?)
						collectGlobalMethodCallCounts();
					}
				}
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
		StringBuilder sb = new StringBuilder(2000);
		for (Entry<String, Count> entry : methodCalls.entrySet()) {
			String methodName = entry.getKey();
			int methodCount = entry.getValue().count;
			long methodTotalTime = entry.getValue().totalTime;

			if (totalTime > CUTOFF_FOR_LOGGING_IN_NANOS) {
				if (sb.length() > 0) {
					sb.append('#');
				}
				sb.append(methodName);
				sb.append(',');
				sb.append(methodCount);
				sb.append(',');
				sb.append(methodTotalTime);
			}

			try {
				GlobalEntry globalEntry = globalProfile.get(methodName);
				synchronized (globalEntry) {
					globalEntry.count += methodCount;
					globalEntry.totalTime += methodTotalTime;
				}
			} catch (ExecutionException e) {
				log.debug("Profile cache error", e);
			}
		}
		if (totalTime > CUTOFF_FOR_LOGGING_IN_NANOS) {
			log.info("Profile: " + sb.toString());
		}
	}
	
	private void collectGlobalMethodCallCounts() {
		StringBuilder sb = new StringBuilder(2000);
		for (GlobalEntry globalEntry : globalProfile.asMap().values()) {
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
				if (sb.length() > 0) {
					sb.append('#');
				}
				sb.append(methodName);
				sb.append(',');
				sb.append(methodCount);
				sb.append(',');
				sb.append(methodTotalTime);
			}
		}
		log.info("Global profile: " + sb.toString());
	}
}
