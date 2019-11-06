package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

/**
 * A utilitiy for deaing with URL redirects.
 * 
 * @author jmhill
 *
 */
public class RedirectUtils {

	private static final String LOCATION_HEADER = "Location";
	
	/**
	 * We either redirect the response to the passed URL or return the URL as plain text.
	 * @param redirect If null then the URL will be redirected.  To get the URL returned as plain text without a redirect an redirect must equal Boolean.FALSE.
	 * @param redirectUrl
	 * @param response
	 * @throws IOException
	 */
	public static void handleRedirect(Boolean redirect, String redirectUrl, HttpServletResponse response) throws IOException {
		// Redirect by default
		if(redirect == null){
			redirect = Boolean.TRUE;
		}
		if(Boolean.TRUE.equals(redirect)){
			// Standard redirect
			response.addHeader(LOCATION_HEADER, redirectUrl);
			response.setStatus(HttpStatus.TEMPORARY_REDIRECT.value());
		}else{
			// Return the redirect url instead of redirecting.
			response.setStatus(HttpStatus.OK.value());
			response.setContentType("text/plain");
			response.getWriter().write(redirectUrl);
			response.getWriter().flush();
		}
	}
}
