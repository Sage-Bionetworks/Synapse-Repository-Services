package org.sagebionetworks.profiler;

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
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Filter to determine if a user wants to capture profile data for a single
 * call. If so, the profile data is added to the response header.
 * 
 * @author jmhill
 * 
 */
public class ProfileFilter implements Filter {

	static private Log log = LogFactory.getLog(ProfileFilter.class);

	public static final String KEY_PROFILE_REQUEST = "profile_request";
	// The JSON object for the profile results
	public static final String KEY_PROFILE_RESPONSE_OBJECT = "profile_response_object";
	// The profile print string.
	public static final String KEY_PROFILE_RESPONSE_PRINT = "profile_response_print";

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest requestIn, ServletResponse responseIn,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) requestIn;
		String value = request.getHeader(KEY_PROFILE_REQUEST);
		ProfileSingleton.setProfile(value != null);
		// Let the call go through
		chain.doFilter(requestIn, responseIn);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

}
