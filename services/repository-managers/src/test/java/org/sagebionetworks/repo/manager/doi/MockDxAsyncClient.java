package org.sagebionetworks.repo.manager.doi;

import org.sagebionetworks.doi.DxAsyncCallback;
import org.sagebionetworks.doi.DxAsyncClient;
import org.sagebionetworks.doi.EzidDoi;

public class MockDxAsyncClient extends DxAsyncClient {

	private final long delay;

	MockDxAsyncClient(long delay) {
		this.delay = delay;
	}

	@Override
	public void resolve(final EzidDoi ezidDoi, final DxAsyncCallback callback) {
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
