package org.sagebionetworks.doi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sagebionetworks.repo.model.doi.Doi;

public class EzidAsyncClient implements DoiClient {

	public EzidAsyncClient(EzidAsyncCallback createCallback) {
		if (createCallback == null) {
			throw new IllegalArgumentException("Callback handle must not be null.");
		}
		this.createCallback = createCallback;
	}

	/**
	 * This is implemented for now as a blocking call to get the results.
	 * Ideally we would get the results from the async callback.
	 */
	@Override
	public EzidDoi get(String doi, Doi dto) {
		return ezidClient.get(doi, dto);
	}

	@Override
	public void create(final EzidDoi doi) {
		executor.submit(new Runnable () {
			@Override
			public void run() {
				try {
					ezidClient.create(doi);
					createCallback.onSuccess(doi);
				} catch (Exception e) {
					createCallback.onError(doi, e);
				}
			}});
	}

	@Override
	public void update(final EzidDoi doi) {
		executor.submit(new Runnable () {
			@Override
			public void run() {
				try {
					ezidClient.update(doi);
					createCallback.onSuccess(doi);
				} catch (Exception e) {
					createCallback.onError(doi, e);
				}
			}});
	}

	// If the thread pool is to have more than 1 thread,
	// the blocking client must also use a pool of connections.
	// The blocking client currently uses SingleClientConnManager.
	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	private final EzidClient ezidClient = new EzidClient();
	private final EzidAsyncCallback createCallback;
}
