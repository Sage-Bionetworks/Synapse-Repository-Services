package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.util.ValidateArgument;

public class IpAddressUtil {
	public static String X_FORWARDED_FOR = "X-Forwarded-For";
	
	/**
	 * Returns the IP address of remote client.
	 * @param request
	 * @return
	 */
	public static String getIpAddress(HttpServletRequest request){
		ValidateArgument.required(request, "request");
		String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
		return xForwardedFor == null ? request.getRemoteAddr() : xForwardedFor;
	}
	
}
