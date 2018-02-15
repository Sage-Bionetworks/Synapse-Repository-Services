package org.sagebionetworks.profiler;

import java.util.List;
import java.util.Stack;

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
	// Each thread gets its own stack.
	private static ThreadLocal<Stack<Frame>> threadFrameStack = ThreadLocal.withInitial(Stack::new);

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
	boolean shouldCaptureData(Object[] args) {
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

		// Do nothing if logging is not on
		if (!shouldCaptureData(pjp.getArgs())) {
			// Just proceed if logging is off.
			return pjp.proceed();
		}

		Signature signature = pjp.getSignature();
		Class<?> declaring = pjp.getTarget().getClass();
		String methodName = declaring.getName() + "." + signature.getName();

		Stack<Frame> parentFramesStack = threadFrameStack.get();

		Frame currentFrame = getCurrentFrame(methodName, parentFramesStack);

		//add the current frame to the stack before proceeding
		parentFramesStack.push(currentFrame);
		long startTime = System.nanoTime();

		// Now start the method.
		try {
			return pjp.proceed();
		} finally {
			long endTime = System.nanoTime();
			currentFrame.addElapsedTime((endTime - startTime) / 1000000);

			// now that the method finished pop the current frame off the stack
			parentFramesStack.pop();

			// If this is the first frame, log the profiling data
			if (parentFramesStack.isEmpty()) {
				doFireProfile(currentFrame);
			}
		}
	}

	Frame getCurrentFrame(String methodName, Stack<Frame> parentFramesStack) {
		Frame currentFrame;
		if (parentFramesStack.isEmpty()) {
			// There are no parent frames, so create a new frame
			currentFrame = new Frame(methodName);
		} else{
			// A parent frame exists so add it to the parent frame
			Frame parentFrame = parentFramesStack.peek();
			currentFrame = parentFrame.addChildFrameIfAbsent(methodName);
		}
		return currentFrame;
	}

	void doFireProfile(Frame frame) {
		if (handlers != null) {
			for (ProfileHandler handler : this.handlers) {
				if (handler.shouldCaptureProfile(null)) {
					handler.fireProfile(frame);
				}
			}
		}
	}
}
