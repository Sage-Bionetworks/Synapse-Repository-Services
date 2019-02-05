package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

public interface AttemptResultReporter {
	void reportSuccess();
	void reportFailure();
}
