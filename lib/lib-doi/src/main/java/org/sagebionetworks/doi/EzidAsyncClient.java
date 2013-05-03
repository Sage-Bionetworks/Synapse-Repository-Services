package org.sagebionetworks.doi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EzidAsyncClient implements DoiAsyncClient {

	@Override
	public void create(final EzidDoi ezidDoi, final EzidAsyncCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("Callback handle must not be null.");
		}
		executor.execute(new Runnable () {
			@Override
			public void run() {
				try {
					ezidClient.create(ezidDoi);
					callback.onSuccess(ezidDoi);
				} catch (Exception e) {
					callback.onError(ezidDoi, e);
				}
			}});
	}

	@Override
	public void update(final EzidDoi doi, final EzidAsyncCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("Callback handle must not be null.");
		}
		executor.execute(new Runnable () {
			@Override
			public void run() {
				try {
					ezidClient.update(doi);
					callback.onSuccess(doi);
				} catch (Exception e) {
					callback.onError(doi, e);
				}
			}});
	}

	// If the thread pool is to have more than 1 thread,
	// the blocking client must also use a pool of connections.
	// The blocking client currently uses SingleClientConnManager.
	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	private final EzidClient ezidClient = new EzidClient();
}
