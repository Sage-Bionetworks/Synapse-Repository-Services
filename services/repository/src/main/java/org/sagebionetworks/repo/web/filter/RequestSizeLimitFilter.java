package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Filter that enforces a maximum limit on the size of the HTTP request.
 *
 */
public class RequestSizeLimitFilter implements Filter {
	
	long maximumInputStreamBytes;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Class[] proxiedInterfaces = new Class[] {
				HttpServletRequest.class,
				ServletRequest.class
		};
		ServletRequest requestProxy = (ServletRequest) Proxy.newProxyInstance(ServletRequest.class.getClassLoader(),proxiedInterfaces , new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				// Invoke the method on the real request
				Object results = method.invoke(request, args);
				// Wrap any input stream in a proxy.
				if(results instanceof InputStream) {
					// wrap the results in a proxy that will limit the bytes read from the proxy.
					results = new SizeLimitingInputStream((InputStream) results, maximumInputStreamBytes);
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
