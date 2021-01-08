package org.sagebionetworks.repo.manager.file.multipart;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartRequestHandlerProviderImpl implements MultipartRequestHandlerProvider {

	private Map<Class<? extends MultipartRequest>, MultipartRequestHandler<? extends MultipartRequest>> handlersMap;
	
	@Autowired
	public MultipartRequestHandlerProviderImpl(List<MultipartRequestHandler<? extends MultipartRequest>> handlers) {
		if (handlers == null || handlers.isEmpty()) {
			throw new IllegalStateException("No multipart request handler found on the classpath.");
		}
		
		handlersMap = handlers.stream()
				.collect(
					Collectors.toMap(MultipartRequestHandler::getRequestClass, Function.identity())
				);
	}
	
	@Override
	public MultipartRequestHandler<? extends MultipartRequest> getHandlerForType(MultiPartRequestType requestType) {
		ValidateArgument.required(requestType, "The requestType");
		return getHandlerForClass(requestType.getRequestType());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends MultipartRequest> MultipartRequestHandler<T> getHandlerForClass(Class<T> requestClass) {
		ValidateArgument.required(requestClass, "The requestClass");
		MultipartRequestHandler<? extends MultipartRequest> handler = handlersMap.get(requestClass);
		if (handler == null) {
			throw new IllegalArgumentException("Unsupported request type: " + requestClass.getName());
		}
		return (MultipartRequestHandler<T>) handler;
	}
	
}
