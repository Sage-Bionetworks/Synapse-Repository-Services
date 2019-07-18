package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * This intercepter checks the status of the repository before allowing any request to go through.
 * 
 * @author jmhill
 *
 */
public class StackStatusInterceptor implements HandlerInterceptor {
	
	@Autowired
	StackStatusDao stackStatusDao;

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		
		// We want any admin call to go through no matter what the state of the stack.
		// This ensures that we can change the state of the stack even when DOWN.
		// If this was not in place, then once a stack was down, there would be no way
		// to bring it up again using the web-services.
		String prefix = request.getRequestURI();
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
		
		// Get the current stack status
		StatusEnum status = stackStatusDao.getCurrentStatus();
		if(StatusEnum.READ_WRITE == status){
			return true;
		}
		if (StatusEnum.READ_ONLY == status || StatusEnum.DOWN == status) {
			StackStatus full = stackStatusDao.getFullCurrentStatus();
			String msg = "Synapse is down for maintenance.";
			if ((full.getCurrentMessage() != null) && (! full.getCurrentMessage().isEmpty())) {
				msg = full.getCurrentMessage();
			}
			throw new ServiceUnavailableException(msg);
		}else{
			throw new IllegalStateException("Unknown Synapse status: "+status);
		}
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// We only care about the preHandle() for this case.
		
	}

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// We only care about the preHandle() for this case.
		
	}


}
