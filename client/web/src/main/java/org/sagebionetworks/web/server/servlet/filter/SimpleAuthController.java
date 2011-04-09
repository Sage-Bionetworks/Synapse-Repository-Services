package org.sagebionetworks.web.server.servlet.filter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This controller implements a simplistic authorization scheme. It sets a
 * cookie indicating that the browser is authorized to access the servlet. For
 * demo's being done on other people's computer, a session cookie is used so
 * that it is deleted when the user exits her browser. For employees, set a
 * permanent cookie so that you can set it once and forget about it.
 *  
 * <pre>
 * ~/platform/trunk/lib/simpleAuth>curl -i http://localhost:8080/repo/v1/dataset
 * HTTP/1.1 403 Forbidden
 * Server: Apache-Coyote/1.1
 * WWW-Authenticate: authenticate simpleAuth
 * Content-Type: text/html;charset=utf-8
 * Content-Length: 964
 * Date: Mon, 04 Apr 2011 02:12:13 GMT
 * 
 * <html><head><title>Apache Tomcat/6.0.26 - Error report</title></head>
 * <body><h1>HTTP Status 403 - </h1><HR size="1" noshade="noshade">
 * <p><b>type</b> Status report</p><p><b>message</b> <u></u></p>
 * <p><b>description</b> <u>Access to the specified resource () has been forbidden.</u></p>
 * <HR size="1" noshade="noshade"><h3>Apache Tomcat/6.0.26</h3></body></html>
 * 
 * ~/platform/trunk/lib/simpleAuth>curl -i http://localhost:8080/repo/v1/simpleauth?scheme=permanent
 * HTTP/1.1 200 OK
 * Server: Apache-Coyote/1.1
 * Set-Cookie: SIMPLEAUTH=quickAndDirty; Expires=Sat, 01-Oct-2011 02:12:37 GMT
 * Content-Type: application/json
 * Transfer-Encoding: chunked
 * Date: Mon, 04 Apr 2011 02:12:37 GMT
 * 
 * "Success!  This cookie will stick around for a long time."
 * 
 * ~/platform/trunk/lib/simpleAuth>curl -i http://localhost:8080/repo/v1/simpleauth?scheme=demo
 * HTTP/1.1 200 OK
 * Server: Apache-Coyote/1.1
 * Set-Cookie: SIMPLEAUTH=quickAndDirty
 * Content-Type: application/json
 * Transfer-Encoding: chunked
 * Date: Mon, 04 Apr 2011 02:12:47 GMT
 * 
 * "Success!  This cookie will go away when you exit the browser."
 * 
 * 
 * ~/platform/trunk/lib/simpleAuth>curl --cookie SIMPLEAUTH=quickAndDirty -i http://localhost:8080/repo/v1/dataset
 * HTTP/1.1 200 OK
 * Server: Apache-Coyote/1.1
 * Content-Type: application/json
 * Transfer-Encoding: chunked
 * Date: Mon, 04 Apr 2011 02:12:57 GMT
 * 
 * {"results":[],"totalNumberOfResults":0,"paging":{}}
 * </pre>
 * 
 * @author deflaux
 * 
 */
@Controller
public class SimpleAuthController {

	/**
	 * the cookie name
	 */
	public static final String SIMPLE_AUTH_COOKIE_NAME = "SIMPLEAUTH";
	/**
	 * the cookie value
	 */
	public static final String SIMPLE_AUTH_COOKIE_VALUE = "quickAndDirty";
	/**
	 * the uri suffix that will come after the servlet prefix in which this controller is installed
	 */
	public static final String SIMPLE_AUTH_URI = "/simpleauth";
	/**
	 * the parameter name for what type of cookie to set
	 */
	public static final String SIMPLE_AUTH_PARAM = "scheme";
	/**
	 * the parameter value for session cookies
	 */
	public static final String SESSION_SCHEME = "demo";
	/**
	 * the parameter value for permanent cookies
	 */
	public static final String PERMANENT_SCHEME = "permanent";

	/**
	 * @param scheme
	 * @param response
	 * @return a string
	 * @throws ServletException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = SIMPLE_AUTH_URI, method = RequestMethod.GET)
	public @ResponseBody
	String setAuth(@RequestParam(value = SIMPLE_AUTH_PARAM) String scheme,
			HttpServletResponse response) throws ServletException {

		if (PERMANENT_SCHEME.equals(scheme)) {
			Cookie simpleAuthCookie = new Cookie(SIMPLE_AUTH_COOKIE_NAME,
					SIMPLE_AUTH_COOKIE_VALUE);
			simpleAuthCookie.setMaxAge(60 * 60 * 24 * 180); // 180 days
			response.addCookie(simpleAuthCookie);
			return "Success!  This cookie will stick around for a long time.";
		} else if (SESSION_SCHEME.equals(scheme)) {
			Cookie simpleAuthCookie = new Cookie(SIMPLE_AUTH_COOKIE_NAME,
					SIMPLE_AUTH_COOKIE_VALUE);
			response.addCookie(simpleAuthCookie);
			return "Success!  This cookie will go away when you exit the browser.";
		} else {
			throw new ServletException("invalid value for scheme");
		}
	}
}

