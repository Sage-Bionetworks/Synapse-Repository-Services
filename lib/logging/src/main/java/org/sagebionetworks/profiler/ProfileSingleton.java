package org.sagebionetworks.profiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProfileSingleton {
	
	private static Map<Long, Boolean> THREAD_SHOULD_PROFILE_MAP = Collections.synchronizedMap(new HashMap<Long, Boolean>());
	
	private static Map<Long, Frame> THREAD_FRAME_MAP = Collections.synchronizedMap(new HashMap<Long, Frame>());
	
	/**
	 * This is called by the filter.
	 * @param shouldPofile
	 */
	public static void setProfile(boolean shouldPofile){
		// Should this thread profile?
		THREAD_SHOULD_PROFILE_MAP.put(Thread.currentThread().getId(), shouldPofile);
	}
	
	/**
	 * This is called by the profiler.
	 * @return
	 */
	public static boolean shouldProfile(){
		Boolean value = THREAD_SHOULD_PROFILE_MAP.get(Thread.currentThread().getId());
		if(value == null) return false;
		return value.booleanValue();
	}
	
	/**
	 * Set the frame data for this thread.
	 * @param frame
	 */
	public static void setFrame(Frame frame){
		THREAD_FRAME_MAP.put(Thread.currentThread().getId(), frame);
	}
	
	/**
	 * Get the frame for this thread.
	 * @return
	 */
	public static Frame getFrame(){
		return THREAD_FRAME_MAP.get(Thread.currentThread().getId());
	}

}
