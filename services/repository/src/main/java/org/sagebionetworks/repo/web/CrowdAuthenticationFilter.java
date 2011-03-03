package org.sagebionetworks.repo.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.sagebionetworks.repo.web.Base64;
import org.sagebionetworks.repo.web.ModParamHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.xml.sax.InputSource;

public class CrowdAuthenticationFilter implements Filter {
	private String crowdProtocol; // http or https
	private String crowdServer;
	private int crowdPort;
	private boolean allowAnonymous = false;
	
	@Override
	public void destroy() {
	}
	
	public static final String USER_ID = "userId";
	
	private static void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// reject
		resp.setStatus(401);
		resp.setHeader("WWW-Authenticate", "authenticate Crowd");
		//String reqUri = req.getRequestURI();
		String contextPath = req.getContextPath();
		// TODO correctly construct the authentication path
		resp.setHeader("Crowd-Authentication-Service", contextPath+"/repo/v1/session");
		resp.getWriter().println("The session token provided was invalid or expired.");
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
				userId = revalidate(sessionToken);
			} catch (Exception xee) {
				reject(req, (HttpServletResponse)servletResponse);
				// TODO log the exception 
//				throw new RuntimeException(xee);
			}
		}
		if (userId!=null) {
			// pass along, including the user id
			@SuppressWarnings("unchecked")
			Map<String,String[]> modParams = new HashMap<String,String[]>(req.getParameterMap());
			modParams.put(USER_ID, new String[]{userId});
			HttpServletRequest modRqst = new ModParamHttpServletRequest(req, modParams);
			filterChain.doFilter(modRqst, servletResponse);
		} else if (allowAnonymous) {
			// proceed anonymously
			filterChain.doFilter(req, servletResponse);
		} else {
			reject(req, (HttpServletResponse)servletResponse);
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
	}

	//-----------------------------------------------------------------------------------------
	// TODO The following code is cut-and-pasted from CrowdAuthUtil and should be factored
	// into a common library
	
	private static final String CLIENT = "platform";
	private static final String CLIENT_KEY = "platform-pw";

	public static String getFromXML(String xPath, byte[] xml) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		return xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)));
	}
	
	public String urlPrefix() {
		return crowdProtocol+"://"+crowdServer+":"+crowdPort+"/crowd/rest/usermanagement/latest";
	}
	
	public static void setHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("Accept", "application/xml");
		conn.setRequestProperty("Content-Type", "application/xml");
		String authString=CLIENT+":"+CLIENT_KEY;
		conn.setRequestProperty("Authorization", "Basic "+Base64.encodeBytes(authString.getBytes())); 
	}
	
		
	public String revalidate(String sessionToken) throws IOException, XPathExpressionException {
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/session/"+sessionToken);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		rc = conn.getResponseCode();
		sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();

		if (HttpStatus.OK.value()!=rc) {
			throw new RuntimeException(new String(sessionXML));
		}

		return getFromXML("/session/user/@name", sessionXML);

	}
	
	private static String readInputStream(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		int i=-1;
		do {
			i = is.read();
			if (i>0) sb.append((char)i);
		} while (i>0);
		return sb.toString().trim();
	}
	
	

}

