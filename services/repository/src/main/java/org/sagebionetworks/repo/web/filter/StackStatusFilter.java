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

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This intercepter checks the status of the repository before allowing any request to go through.
 *
 * @author bhoff
 *
 */
@Component("stackStatusFilter")
public class StackStatusFilter implements Filter {
		
	@Autowired
	private StackStatusDao stackStatusDao;
	
	public static boolean isBypassUri(String prefix) {
		// We want any admin call to go through no matter what the state of the stack.
		// This ensures that we can change the state of the stack even when DOWN.
		// If this was not in place, then once a stack was down, there would be no way
		// to bring it up again using the web-services.
		if(prefix != null && prefix.contains(UrlHelpers.ADMIN)){
			return true;
		}
		// We want all migration to go through not matter what.
		if(prefix != null && prefix.contains(UrlHelpers.MIGRATION)){
			return true;
		}
		// We want all health check to go through not matter what.
		if(prefix != null && prefix.contains(UrlHelpers.VERSION)){
			return true;
		}
		if(prefix != null && prefix.contains(UrlHelpers.STACK_STATUS)) {
			return true;
		}
		return false;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		
		if (!isBypassUri(httpRequest.getRequestURI())) {
			// Get the current stack status
			StatusEnum status = stackStatusDao.getCurrentStatus();
			if (StatusEnum.READ_ONLY == status || StatusEnum.DOWN == status) {
				StackStatus full = stackStatusDao.getFullCurrentStatus();
				String msg = "Synapse is down for maintenance.";
				if ((full.getCurrentMessage() != null) && (! full.getCurrentMessage().isEmpty())) {
					msg = full.getCurrentMessage();
				}
				HttpAuthUtil.rejectWithErrorResponse(httpResponse, msg, HttpStatus.SERVICE_UNAVAILABLE);
				return; // do NOT call chain.doFilter()
			} else if (StatusEnum.READ_WRITE==status) {
				// fall through to 'chain.doFilter'
			} else {
				throw new IllegalStateException("Unknown stack status: "+status);
			}
		}

		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
