package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * This filter will enforce a size limit on all InputStreams from HTTP requests.
 * 
 * If reading from an InputStream of the request exceeds the limit then a 'Payload Too Large' (413)
 * will be returned to the caller. 
 *
 */
public class RequestSizeThrottleFilter implements Filter {
		
	/**
	 * Current limit is 2 MB.
	 */
	long maximumInputStreamBytes = 1024*1024*2;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Class[] proxiedInterfaces = new Class[] {
				HttpServletRequest.class,
				ServletRequest.class
		};
		// Wrap the request in a proxy to capture calls to get input streams.
		ServletRequest requestProxy = (ServletRequest) Proxy.newProxyInstance(ServletRequest.class.getClassLoader(),proxiedInterfaces , new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				// Invoke the method on the real request
				Object results = method.invoke(request, args);
				// Wrap any ServletInputStream in a proxy.
				if(results instanceof ServletInputStream) {
					// wrap the results in a proxy that will limit the bytes read from the proxy.
					results = new ThrottlingProxyInputStream((ServletInputStream) results, maximumInputStreamBytes);
				}
				return results;
			}});
		// pass the proxied response to the chain.
		chain.doFilter(requestProxy, response);
	}

	@Override
	public void destroy() {

	}

}
