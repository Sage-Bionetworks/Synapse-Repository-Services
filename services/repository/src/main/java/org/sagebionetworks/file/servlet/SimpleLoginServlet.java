package org.sagebionetworks.file.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.User;

/**
 * This is a simple servlet that will login
 * 
 * @author John
 * 
 */
public class SimpleLoginServlet implements Servlet {

	static private Log log = LogFactory.getLog(SimpleLoginServlet.class);

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public ServletConfig getServletConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(ServletConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void service(ServletRequest req, ServletResponse res)	throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		try{
			// Get the username and password
			String username = request.getParameter("username");
			if(username == null) throw new IllegalArgumentException("'username' is a required parameter");
			String password = request.getParameter("password");
			if(password == null) throw new IllegalArgumentException("'password' is a required parameter");
			// Login the user
			User user = new User();
			user.setPassword(password);
			user.setEmail(username);
			Session session = CrowdAuthUtil.authenticate(user, true);
			// Add it as a cookie
			Cookie cookie = new Cookie(AuthorizationConstants.SESSION_TOKEN_COOKIE_NAME, session.getSessionToken());
			cookie.setPath("/");
			cookie.setMaxAge(1000*60*1);
			response.addCookie(cookie);
			// Send them back.
			response.sendRedirect("/services-file/index.html");
		}catch(Throwable e){
			log.error("Login error", e);
			response.setStatus(400);
			response.getWriter().append(e.getMessage());
		}
	}

}
