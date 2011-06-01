package org.sagebionetworks.authutil;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CrowdAuthenticationFilter implements Filter {
	private static final Logger log = Logger.getLogger(CrowdAuthenticationFilter.class
			.getName());
	
	private boolean allowAnonymous = false;
	private boolean acceptAllCerts = true;
	
	CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();
	
	@Override
	public void destroy() {
	}
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// reject
		resp.setStatus(401);
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
//		String contextPath = req.getContextPath();
		resp.getWriter().println("{\"reason\", \"The session token provided was missing, invalid or expired.\"}");
	}
	
	private boolean usingMockCrowd;
	

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {

		// If token present, ask Crowd to validate and get user id
		HttpServletRequest req = (HttpServletRequest)servletRqst;
		String sessionToken = req.getHeader("sessionToken");
		String userId = null;
		if(usingMockCrowd){
			// Some tests provide a user name.
			userId = req.getParameter(AuthUtilConstants.USER_ID_PARAM);
		}
		if (null!=sessionToken) {
			// validate against crowd
			try {
				if (usingMockCrowd) {
					userId= sessionToken;
				} else {
					userId = crowdAuthUtil.revalidate(sessionToken);
				}
			} catch (Exception xee) {
				reject(req, (HttpServletResponse)servletResponse);
				log.log(Level.WARNING, "invalid session token", xee);
				return;
			}
		}
		if (userId==null && !allowAnonymous) {
			reject(req, (HttpServletResponse)servletResponse);
			return;
		}
		if (userId==null) userId = AuthUtilConstants.ANONYMOUS_USER_ID;

		// pass along, including the user id
		@SuppressWarnings("unchecked")
		Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
		modParams.put(AuthUtilConstants.USER_ID_PARAM, new String[]{userId});
		HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
		filterChain.doFilter(modRqst, servletResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		@SuppressWarnings("unchecked")
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
           	if ("accept-all-certificates".equalsIgnoreCase(paramName)) acceptAllCerts = Boolean.parseBoolean(paramValue);
        }
        
       if (acceptAllCerts) CrowdAuthUtil.acceptAllCertificates2();
       
       
		String implementingClassName = System.getProperty(AuthUtilConstants.USER_DAO_INTEGRATION_TEST_SWITCH);
		if (implementingClassName!=null && implementingClassName.length()>0) {
			usingMockCrowd = true;
		}else{
			usingMockCrowd = false;
		}
  	}
}

