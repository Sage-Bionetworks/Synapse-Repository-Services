package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.doi.DoiAsyncClient;
import org.sagebionetworks.doi.EzidAsyncCallback;
import org.sagebionetworks.doi.EzidDoi;

public class MockDoiAsyncClient implements DoiAsyncClient {

	private final long delay;

	MockDoiAsyncClient(long delay) {
		this.delay = delay;
	}

	@Override
	public void create(final EzidDoi ezidDoi, final EzidAsyncCallback callback) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					callback.onSuccess(ezidDoi);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		});
		thread.start();
	}

	@Override
	public void update(final EzidDoi ezidDoi, final EzidAsyncCallback callback) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					callback.onSuccess(ezidDoi);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		});
		thread.start();
	}
}
