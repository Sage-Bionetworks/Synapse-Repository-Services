package org.sagebionetworks.profiler;

import java.util.List;
import java.util.Stack;

public class ProfilerFrameStackManager {
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

	public void startProfiling(String methodName){
		Stack<Frame> parentFramesStack = threadFrameStack.get();

		Frame currentFrame = getCurrentFrame(methodName);

		//add the current frame to the stack before proceeding
		parentFramesStack.push(currentFrame);
	}

	public void endProfiling(String methodName, long elapsedTimeMillis){
		Stack<Frame> parentFramesStack = threadFrameStack.get();

		// now that the method finished pop the current frame off the stack
		Frame currentFrame = parentFramesStack.pop();

		if(!currentFrame.getName().equals(methodName)){
			throw new IllegalArgumentException("Expected to end profiling on " + currentFrame.getName() + " but got " + methodName);
		}

		currentFrame.addElapsedTime(elapsedTimeMillis);

		// If this is the first frame, log the profiling data
		if (parentFramesStack.isEmpty()) {
			doFireProfile(currentFrame);
		}
	}

	/**
	 * Should we even profile.
	 *
	 * @return
	 */
	boolean shouldCaptureData() {
		if (handlers == null) {
			return false;
		}
		for (ProfileHandler handler : this.handlers) {
			if (handler.shouldCaptureProfile()) {
				return true;
			}
		}
		return false;
	}

	//NOTE: this method could be static but it makes it impossible to Mock its behavior
	Frame getCurrentFrame(String methodName) {
		Stack<Frame> parentFramesStack = threadFrameStack.get();
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
				if (handler.shouldCaptureProfile()) {
					handler.fireProfile(frame);
				}
			}
		}
	}
}
