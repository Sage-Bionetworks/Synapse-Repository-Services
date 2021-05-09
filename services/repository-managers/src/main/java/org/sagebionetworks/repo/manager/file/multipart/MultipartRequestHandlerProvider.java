package org.sagebionetworks.repo.manager.file.multipart;

import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.file.MultipartRequest;

/**
 * {@link MultipartRequestHandler} provider for a given type of request.
 * 
 * @author Marco Marasca
 *
 */
public interface MultipartRequestHandlerProvider {

	MultipartRequestHandler<? extends MultipartRequest> getHandlerForType(MultiPartRequestType requestType);

	<T extends MultipartRequest> MultipartRequestHandler<T> getHandlerForClass(Class<T> requestClass);

}