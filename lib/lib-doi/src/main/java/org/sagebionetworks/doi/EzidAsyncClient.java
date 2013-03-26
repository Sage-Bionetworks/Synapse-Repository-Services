package org.sagebionetworks.doi;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EzidAsyncClient implements DoiClient {

	@Override
	public void create(final EzidMetadata metadata) {
		executor.submit(new Callable<EzidMetadata> () {
			@Override
			public EzidMetadata call() {
				try {
					ezidClient.create(metadata);
					callback.onSuccess(metadata);
				} catch (Exception e) {
					callback.onError(e);
				}
				return null;
			}});
	}

	private final EzidClient ezidClient = new EzidClient();
	private final ExecutorService executor = Executors.newFixedThreadPool(1);;
	private EzidAsyncCallback callback;
}
