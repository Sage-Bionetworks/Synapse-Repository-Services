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
		if(prefix != null && prefix.indexOf(UrlHelpers.ADMIN) > -1){
			return true;
		}
		// We want all migration to go through not matter what.
		if(prefix != null && prefix.indexOf(UrlHelpers.MIGRATION) > -1){
			return true;
		}
		
		// Get the current stack status
		StatusEnum status = stackStatusDao.getCurrentStatus();
		if(StatusEnum.DOWN == status){
			StackStatus full = stackStatusDao.getFullCurrentStatus();
			throw new ServiceUnavailableException("Synapse is down for maintenance.  Message: "+full.getCurrentMessage());
		}else if(StatusEnum.READ_WRITE == status){
			return true;
		}else if(StatusEnum.READ_ONLY == status){
			// Allow all GETs
			if("GET".equals(request.getMethod())){
				return true;
			}else{
				StackStatus full = stackStatusDao.getFullCurrentStatus();
				throw new ServiceUnavailableException("Synapse is in READ_ONLY mode for maintenance.  Only HTTP GETs are allowed at this time.  Message: "+full.getCurrentMessage());
			}
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
