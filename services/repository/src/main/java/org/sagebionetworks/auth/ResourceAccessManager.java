package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StringEncrypter;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.User;
import org.sagebionetworks.repo.web.NotFoundException;


public class ResourceAccessManager {

	public static void setUserAnnotation(String userName, String attribute,
			Collection<String> values) {
		Map<String, Collection<String>> annotations = new HashMap<String, Collection<String>>();
		annotations.put(attribute, values);
		try {
			CrowdAuthUtil.setUserAttributes(userName, annotations);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Collection<String> getUserAnnotation(String userName,
			String attribute) throws NotFoundException {
		try {
			Map<String, Collection<String>> annotations = CrowdAuthUtil.getUserAttributes(userName);
			return annotations.get(attribute);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void deleteUserAnnotation(String userName, String attribute) {
		try {
			CrowdAuthUtil.deleteUserAttribute(userName, attribute);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final String RESOURCE_ACCESS_PREFIX = "org.sagebionetworks.ResourceAccess.";

	public static void createResourceAccess(String userName, String resourceName, String userData) {
		String attribute = RESOURCE_ACCESS_PREFIX+resourceName;
		Collection<String> values = Arrays.asList(new String[]{userData});
		setUserAnnotation(userName, attribute, values);
	}

	public static String readResourceAccess(String userName, String resourceName) throws NotFoundException {
		String attribute = RESOURCE_ACCESS_PREFIX+resourceName;
		Collection<String> values = getUserAnnotation(userName, attribute);
		if (values==null) throw new NotFoundException("No resource-access for user "+userName+" and resource "+resourceName);
		if (values.size()!=1) 
			throw new IllegalStateException("Expected one value but found "+values.size()+
				" for user "+userName+" and attribute "+attribute+": "+values);
		return values.iterator().next();
	}
	
	/**
	 * 
	 * Same as 'create'
	 */
	public static void updateResourceAccess(String userName, String resourceName, String userData) {
		String attribute = RESOURCE_ACCESS_PREFIX+resourceName;
		Collection<String> values = Arrays.asList(new String[]{userData});
		setUserAnnotation(userName, attribute, values);
	}

	public static void deleteResourceAccess(String userName, String resourceName) {
		String attribute = RESOURCE_ACCESS_PREFIX+resourceName;
		deleteUserAnnotation(userName, attribute);
	}
	
	// since Crowd session tokens don't use underscores, it's safe to use them as separators
	public static final String separator = "_";
	
	// base64 encoding can have +,/,= 
	// URLs may have $-_.+!*'(),
	// but URLEncoder encodes all but ".", "-", "*", and "_"
	// so we change / to - and = to .
	public static String base64ToURLSafe(String s) {
		s = s.replace('/', '-');
		s = s.replace('=', '.');
		return s;
	}
	
	public static String urlSafeToBase64(String s) {
		s = s.replace('-', '/');
		s = s.replace('.', '=');
		return s;
	}
	
	private static final String encryptionKey = StackConfiguration.getEncryptionKey();
	
	public static String createResourceAccessToken(String sessionToken, String resourceName) {
		return createResourceAccessToken(sessionToken, resourceName, encryptionKey);
	}
	
	// for unit testing
	public static String createResourceAccessToken(String sessionToken, String resourceName, String encryptionKey) {
		StringEncrypter se = new StringEncrypter(encryptionKey);
		String unencryptedString = sessionToken+separator+resourceName;
		return base64ToURLSafe(se.encrypt(unencryptedString));
	}
	
	public static String extractSessionToken(String resourceAccessToken) {
		StringEncrypter se = new StringEncrypter(encryptionKey);
		String unencryptedString = se.decrypt(urlSafeToBase64(resourceAccessToken));
		int i = unencryptedString.indexOf(separator);
		if (i<0) throw new IllegalArgumentException("Bad resource access token.");
		return unencryptedString.substring(0, i);
	}

	public static String extractResourceName(String resourceAccessToken) {
		StringEncrypter se = new StringEncrypter(encryptionKey);
		String unencryptedString = se.decrypt(urlSafeToBase64(resourceAccessToken));
		int i = unencryptedString.indexOf(separator);
		if (i<0) throw new IllegalArgumentException("Bad resource access token.");
		return unencryptedString.substring(i+separator.length());
	}
	
	/**
	 * Gets a regular Synapse session token for a user (i.e. 'logs them in')
	 * Should only be called after authenticating the user.
	 */
	public static String getSessionTokenFromUserName(String userName) throws AuthenticationException {
		User creds = new User();
		creds.setEmail(userName);
		try {
			Session session = CrowdAuthUtil.authenticate(creds, /*validatePassword*/false);
			return session.getSessionToken();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param sessionToken the ResourceAccess session token
	 */
	public static String getUserNameFromSessionToken(String sessionToken) throws AuthenticationException {
		try {
			return CrowdAuthUtil.revalidate(sessionToken);
		} catch (AuthenticationException ae) {
			throw ae;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
}
