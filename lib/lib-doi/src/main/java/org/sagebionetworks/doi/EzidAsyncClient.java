package org.sagebionetworks.doi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EzidAsyncClient implements DoiClient {

	public EzidAsyncClient(EzidAsyncCallback createCallback) {
		if (createCallback == null) {
			throw new IllegalArgumentException("Callback handle must not be null.");
		}
		callback = createCallback;
	}

	@Override
	public void create(final EzidDoi doi) {
		executor.submit(new Callable<EzidDoi> () {
			@Override
			public EzidDoi call() {
				try {
					ezidClient.create(doi);
					callback.onSuccess(doi);
					return doi;
				} catch (Exception e) {
					callback.onError(doi, e);
					return doi;
				}
			}});
	}

	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	private final EzidClient ezidClient = new EzidClient();
	private EzidAsyncCallback callback;
}
