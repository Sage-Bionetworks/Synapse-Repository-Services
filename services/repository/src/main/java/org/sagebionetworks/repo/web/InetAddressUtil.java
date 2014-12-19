package org.sagebionetworks.repo.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * This Util class may help capture the IP address and host name from an HttpServletRequest.
 * Our access_record log currently failed to capture the X-Forwarded_for field in the request's header.
 * One assumption is that we lost track of the X-Forwarded-For while trying to capture the userId. 
 * Adding this Util may not help since the IP address in request.getRemoteAddr() could be the ELB's or 
 * the server's IP address.
 * 
 * Source of the code:
 * http://r.va.gg/2011/07/handling-x-forwarded-for-in-java-and-tomcat.html
 */
public class InetAddressUtil {
    private static final String IP_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";
    private static final String PRIVATE_IP_ADDRESS_REGEX = "(^127\\.0\\.0\\.1)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)";
    private static Pattern IP_ADDRESS_PATTERN = null;
    private static Pattern PRIVATE_IP_ADDRESS_PATTERN = null;

    private static String findNonPrivateIpAddress(String s) {
        if (IP_ADDRESS_PATTERN == null) {
            IP_ADDRESS_PATTERN = Pattern.compile(IP_ADDRESS_REGEX);
            PRIVATE_IP_ADDRESS_PATTERN = Pattern.compile(PRIVATE_IP_ADDRESS_REGEX);
        }
        Matcher matcher = IP_ADDRESS_PATTERN.matcher(s);
        while (matcher.find()) {
            if (!PRIVATE_IP_ADDRESS_PATTERN.matcher(matcher.group(0)).find())
                return matcher.group(0);
            matcher.region(matcher.end(), s.length());
        }
        return null;
    }

    public static String getAddressFromRequest(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && (forwardedFor = findNonPrivateIpAddress(forwardedFor)) != null)
            return forwardedFor;
        return request.getRemoteAddr();
    }

    public static String getHostnameFromRequest(HttpServletRequest request) {
        String addr = getAddressFromRequest(request);
        try {
            return Inet4Address.getByName(addr).getHostName();
        } catch (Exception e) {
        }
        return addr;
    }

    public static InetAddress getInet4AddressFromRequest(HttpServletRequest request) throws UnknownHostException {
        return Inet4Address.getByName(getAddressFromRequest(request));
    }
}
