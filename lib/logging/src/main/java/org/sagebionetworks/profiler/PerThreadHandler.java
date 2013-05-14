package org.sagebionetworks.profiler;

public class PerThreadHandler implements ProfileHandler {

	@Override
	public boolean shouldCaptureProfile(Object[] args) {
		return ProfileSingleton.shouldProfile();
	}

	@Override
	public void fireProfile(Frame data) {
		ProfileSingleton.setFrame(data);
	}

}
