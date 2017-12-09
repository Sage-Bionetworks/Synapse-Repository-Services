package org.sagebionetworks.repo.web.filter;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.servlet.ServletRequest;

/**
 * An InvocationHandler that will enforce the provided size limit
 * on any InputStream returned by 
 *
 */
public class LimitedInputStreamInvocationHandler implements InvocationHandler {
	
	ServletRequest wrappedRequest;
	Long maximumInputStreamBytes;
	
	public LimitedInputStreamInvocationHandler(ServletRequest wrappedRequest, Long maximumInputStreamBytes) {
		super();
		this.wrappedRequest = wrappedRequest;
		this.maximumInputStreamBytes = maximumInputStreamBytes;
	}



	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// Invoke the method on the real object
		Object results = method.invoke(wrappedRequest, args);
		// Wrap any input stream in a proxy.
		if(results instanceof InputStream) {
			// wrap the results in a proxy that will limit the bytes read from the proxy.
			results = new SizeLimitingInputStream((InputStream) results, maximumInputStreamBytes);
		}
		return results;
	}

}
