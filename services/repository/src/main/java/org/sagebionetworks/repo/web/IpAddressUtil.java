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
		return xForwardedFor == null ? request.getRemoteAddr() : parseRemoteIpFromXForwardedFor(xForwardedFor);
	}
	
	/**
	 * Parses the value of the X-Forwarded-For http header and returns the client's Ip address
	 * Expected format is a comma separated list of IP addresses
	 * @param xForwardedFor
	 * @return
	 */
	public static String parseRemoteIpFromXForwardedFor(String xForwardedFor){
		ValidateArgument.required(xForwardedFor, "XForwardedFor");
		//client IP should be the first value in the comma separated list
		//https://en.wikipedia.org/wiki/X-Forwarded-For
		return xForwardedFor.split(",")[0];
	}
	
}
