package org.sagebionetworks.client.upload;

import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.file.AddPartResponse;

@FunctionalInterface
public interface PartCallableFactory {

	/**
	 * Abstraction for creating an new Callable for each part request.
	 * 
	 * @param request
	 * @return
	 */
	public Callable<AddPartResponse> createCallable(FilePartRequest request);
}
