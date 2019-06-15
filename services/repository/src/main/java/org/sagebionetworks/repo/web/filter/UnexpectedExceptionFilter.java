package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * This filter is our last chance to log any type of unexpected error. Errors
 * that occur at the controller level or below are already well handled. All
 * errors from the controllers or lower are already captured and converted to
 * status codes with error messages, so those exceptions will never be seen
 * here. This filter is designed to capture unexpected errors that occur above
 * the controller in all other filters or interceptors. See PLFM-3204 &
 * PLFM-3205
 * 
 * @author John
 * 
 */
public class UnexpectedExceptionFilter implements Filter {

	private static Log log = LogFactory.getLog(UnexpectedExceptionFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			/*
			 * Exceptions thrown at the controller or lower should already be
			 * converted to error status codes and will not be seen here. Only
			 * unexpected exceptions that are thrown by filters or interceptors
			 * will be seen here. We need to gather as much information as
			 * possible from the request and log it.
			 */
			HttpServletRequestData data = new HttpServletRequestData(
					(HttpServletRequest) request);
			// capture the full stack trace and request data.
			log.error(data.toString(), e);
			/*
			 * All known causes of exceptions at this level are due to load. By
			 * returning 503, clients will back-off.  We assume the server can
			 * recover from these errors.
			 */
			HttpServletResponse res = (HttpServletResponse) response;
			res.setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
			ErrorResponse er = new ErrorResponse();
			er.setReason("Server error, try again later: " + e.getMessage());
			JSONObjectAdapter joa = new JSONObjectAdapterImpl();
			try {
				er.writeToJSONObject(joa);
				res.setContentType(MediaType.APPLICATION_JSON_VALUE);
				res.getWriter().println(joa.toJSONString());
			} catch (JSONObjectAdapterException e2) {
				// give up here, just write constant string
				res.getWriter().println(AuthorizationConstants.REASON_SERVER_ERROR);
			}
		}catch(Error e){
			/*
			 * Errors are far worse than exceptions.
			 */
			HttpServletRequestData data = new HttpServletRequestData(
					(HttpServletRequest) request);
			// capture the full stack trace and request data.
			log.error(data.toString(), e);
			/*
			 * Assume the server will not recover from an error so report generic 500.
			 */
			HttpServletResponse res = (HttpServletResponse) response;
			res.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			res.getWriter().println(AuthorizationConstants.REASON_SERVER_ERROR);
		}
	}

	@Override
	public void destroy() {
	}

}
