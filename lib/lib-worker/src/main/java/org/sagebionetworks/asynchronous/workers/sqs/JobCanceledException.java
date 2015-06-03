package org.sagebionetworks.asynchronous.workers.sqs;

public class JobCanceledException extends RuntimeException {
	private static final long serialVersionUID = 702027841349569661L;

	public JobCanceledException() {
		super("Canceled");
	}
}

