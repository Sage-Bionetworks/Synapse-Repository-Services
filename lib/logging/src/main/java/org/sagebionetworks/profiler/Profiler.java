package org.sagebionetworks.profiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This is a Profiler that logs the results.
 * 
 * @author jmhill
 *
 */
@Aspect
public class Profiler {

	
	static private Log log = LogFactory.getLog(Profiler.class);
	
	// Each thread gets its own stack.
	static Map<Long, Stack<Frame>> MAP = Collections
			.synchronizedMap(new HashMap<Long, Stack<Frame>>());
	
	private List<ProfileHandler> handlers = null;


	public List<ProfileHandler> getHandlers() {
		return handlers;
	}

	/**
	 * Injected via Spring.
	 * @param handlers
	 */
	public void setHandlers(List<ProfileHandler> handlers) {
		this.handlers = handlers;
	}

	/**
	 * Should we even profile.
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
	@Around("execution(* org.sagebionetworks..*.*(..))")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {

		// Do nothing if loggin is not on
		if (!shouldCaptureData(pjp.getArgs())) {
			// Just proceed if logging is off.
			return pjp.proceed();
		}
		Signature signatrue = pjp.getSignature();


		// Method method = invocation.getMethod();
		String methodName = signatrue.getName();
		Class declaring = signatrue.getDeclaringType();
		Long threadId = Thread.currentThread().getId();
		Stack<Frame> stack = null;
		if (declaring != null) {
			// We are at the end so print the frame
			synchronized (MAP) {
				stack = MAP.get(threadId);
				if (stack == null) {
					stack = new Stack<Frame>();
					MAP.put(threadId, stack);
				}
			}
			// Push a new frame onto the stack.
			StringBuilder builder = new StringBuilder();
			builder.append(declaring.getName());
			builder.append(".");
			builder.append(methodName);
			long startTime = System.nanoTime();
			Frame newFrame = new Frame(startTime, builder.toString());
			stack.push(newFrame);
		}

		// Now start the method.
		Object returnValue = null;
		try {
//			long start = System.nanoTime();
			returnValue = pjp.proceed();
//			long elapse = System.nanoTime() - start;
//			long mili = elapse/1000000;
//			log.info(mili);
		} catch (Throwable e) {
			// When this occurs we need to clear the stack
			if (stack != null) {
				stack.clear();
			}
			throw e;
		}

		// After the method is done.
		if (declaring != null) {
			// To get around unit test failure, check stack
			if ((stack != null) && (stack.size() > 0)) {
				// Push a new frame onto the stack.
				Frame current = stack.pop();
				current.setEnd(System.nanoTime());
				// Is there anything else on the stack?
				if (stack.size() > 0) {
					Frame peek = stack.peek();
					peek.addChild(current);
				} else {
					// This should get replace with a logger.
					doFireProfile(current);
				}
			}
		}
		return returnValue;
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
}
