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
	
	private String crowdProtocol; // http or https
	private String crowdServer;
	private int crowdPort;
	private boolean allowAnonymous = false;
	
	@Override
	public void destroy() {
	}
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// reject
		resp.setStatus(401);
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
//		String contextPath = req.getContextPath();
		resp.getWriter().println("The session token provided was missing, invalid or expired.");
	}

	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {

		// If token present, ask Crowd to validate and get user id
		HttpServletRequest req = (HttpServletRequest)servletRqst;
		String sessionToken = req.getHeader("sessionToken");
		String userId = null;
		if (null!=sessionToken) {
			// validate against crowd
			try {
				CrowdAuthUtil cau = new CrowdAuthUtil(crowdProtocol, crowdServer, crowdPort);
				userId = cau.revalidate(sessionToken);
			} catch (Exception xee) {
				reject(req, (HttpServletResponse)servletResponse);
				log.log(Level.WARNING, "invalid session token", xee);
				return;
			}
		}
		if (userId!=null) {
			// pass along, including the user id
			@SuppressWarnings("unchecked")
			Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
			modParams.put(AuthUtilConstants.USER_ID_PARAM, new String[]{userId});
			HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
			filterChain.doFilter(modRqst, servletResponse);
		} else if (allowAnonymous) {
			// proceed anonymously
			filterChain.doFilter(req, servletResponse);
		} else {
			reject(req, (HttpServletResponse)servletResponse);
			return;
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		@SuppressWarnings("unchecked")
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	String paramValue = filterConfig.getInitParameter(paramName);
           	if ("crowd-protocol".equalsIgnoreCase(paramName)) crowdProtocol = paramValue;
           	if ("crowd-host".equalsIgnoreCase(paramName)) crowdServer = paramValue;
           	if ("crowd-port".equalsIgnoreCase(paramName)) crowdPort = Integer.parseInt(paramValue);
           	if ("allow-anonymous".equalsIgnoreCase(paramName)) allowAnonymous = Boolean.parseBoolean(paramValue);
        }
        
        CrowdAuthUtil.acceptAllCertificates();
  	}

		
}

