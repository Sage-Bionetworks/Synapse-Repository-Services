package org.sagebionetworks.profiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ui.context.Theme;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

/**
 * This is a Profiler that logs the results.
 * 
 * @author jmhill
 * 
 */
@Aspect
public class Profiler {

	private static class Count {
		long totalTime = 0;
		int count = 0;
	}

	private static class ProfileData {
		Frame currentFrame;
		Map<String, Count> methodCalls = Maps.newHashMap();
	}

	private static final Cache<String, Count> globalMap = CacheBuilder.newBuilder().build(new CacheLoader<String, Count>() {
		@Override
		public Count load(String key) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	});

	// Each thread gets its own stack.
	private static ThreadLocal<ProfileData> threadFrameStack = new ThreadLocal<ProfileData>();

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
		parentFrame.addChild(currentFrame);

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
				collectMethodCallCounts(profileData.methodCalls);
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

	private void collectMethodCallCounts(Map<String, Count> methodCalls) {
		for (Entry<String, Count> entry : methodCalls.entrySet()) {
			Count globalEntry = globalMap.get(entry.getKey());
			synchronized (globalEntry) {
				globalEntry.count += entry.getValue().count;
				globalEntry.totalTime += entry.getValue().totalTime;
			}
		}
	}
}
