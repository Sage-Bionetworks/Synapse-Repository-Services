package org.sagebionetworks.repo.web.filter.throttle;

public class RequestThrottlerCleanupNoOpImpl implements RequestThrottlerCleanup {

	@Override
	public void close() {
		//do nothing
	}
}
