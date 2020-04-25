package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.UrlHelpers;

/**
 * This is an filter that converts a response to JSONP.
 * Any services that exposes JSONP is vulnerable to Cross-site request forgery ( @see <a href="http://www.squarefree.com/securitytips/web-developers.html#CSRF">CSRF</a> )
 * Therefore, we will only return JSONP for public data.  If a 'sessionToken' is provided with a JSOP request an exception will be thrown.
 * 
 * Note: This filter was provided by:  <a href="http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy/#1">Tutorial: Implementing a Servlet Filter for JSONP callback with Spring's DelegatingFilterProxy</a>
 *
 */
public class JsonpCallbackFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	public static final String CHARACTER_ENCODING = "UTF-8";
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		@SuppressWarnings("unchecked")
		Map<String, String[]> parms = httpRequest.getParameterMap();

		if(parms.containsKey(UrlHelpers.REQUEST_CALLBACK_JSONP)) {
			// Reject any request with header data
			String sessionToken = httpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
			if(sessionToken != null){
				throw new ServletException("JSONP callbacks are only allowed on public methods with no 'sessionToken'");
			}
			String accessToken = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
			if(accessToken != null){
				throw new ServletException("JSONP callbacks are only allowed on public methods with no authorization.");
			}
			OutputStream out = httpResponse.getOutputStream();

			GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);
			// Pass it along.
			chain.doFilter(request, wrapper);

			out.write(new String(parms.get(UrlHelpers.REQUEST_CALLBACK_JSONP)[0] + "(").getBytes(CHARACTER_ENCODING));
			
			String wrapperCharacterEncoding = wrapper.getCharacterEncoding();
			if (wrapperCharacterEncoding==null) wrapperCharacterEncoding="ISO-8859-1";
			byte[] wrapperBytes = wrapper.getData();
			String wrapperString = new String(wrapperBytes, wrapperCharacterEncoding);
			out.write(wrapperString.getBytes(CHARACTER_ENCODING));
			
			out.write(new String(");").getBytes(CHARACTER_ENCODING));

			wrapper.setContentType("text/javascript;charset="+CHARACTER_ENCODING);

			out.close();
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
