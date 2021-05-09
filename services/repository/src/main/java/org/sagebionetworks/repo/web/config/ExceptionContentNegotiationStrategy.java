package org.sagebionetworks.repo.web.config;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * A custom {@link ContentNegotiationStrategy} used solely for exception resolution. This is due to https://sagebionetworks.jira.com/browse/PLFM-6435:
 * <br>
 * When an exception is raised our exception handler will catch and turn the exception into an error object that is returned by the backend that needs to
 * be serialized. This is handled by the {@link ExceptionHandlerExceptionResolver}. The exception resolver will try to perform content negotiation according to 
 * a given strategy and previously the default {@link HeaderContentNegotiationStrategy} was used to decide an accetable representation for the client.
 * <br>
 * This is problematic because the client could have specified a random non supported accept header and the content negotiation would fail with another exception.
 * <br>
 * This version of the negotiation strategy simply extends the default header based one to avoid failing when the accept header is not supported, since at this point
 * we are dealing already with an exception and some error should be returned in one of the supported representations.
 *
 * @author Marco Marasca
 *
 */
public class ExceptionContentNegotiationStrategy extends HeaderContentNegotiationStrategy {
	
	private List<MediaType> supportedMediaTypes;
	
	public ExceptionContentNegotiationStrategy(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {
		List<MediaType> mediaTypes;
		
		try {
			mediaTypes = super.resolveMediaTypes(webRequest);
		} catch (HttpMediaTypeNotAcceptableException ex) {
			return supportedMediaTypes;
		}
		
		for (MediaType type : mediaTypes) {
			for (MediaType supportedType : supportedMediaTypes) {
				if (type.isCompatibleWith(supportedType)) {
					return mediaTypes;
				}
			}
		}
		
		return supportedMediaTypes;
	}

}
