package org.sagebionetworks.authutil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.http.HttpStatus;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CrowdAuthUtil {
	private static final Logger log = Logger.getLogger(CrowdAuthUtil.class.getName());

	// restore the 'final' designations after the 0.12->0.13 migration is complete
	private static  String CROWD_URL = StackConfiguration.getCrowdEndpoint(); // e.g. https://ec2-50-16-158-220.compute-1.amazonaws.com:8443
	private static  String API_APPLICATION_KEY = StackConfiguration.getCrowdAPIApplicationKey();

	private static String apiApplication;
	
	static {
		HttpClientHelper.setGlobalConnectionTimeout(DefaultHttpClientSingleton.getInstance(), 5000);
		HttpClientHelper.setGlobalSocketTimeout(DefaultHttpClientSingleton.getInstance(), 10000);
		
		// read values from the properties file
        Properties props = new Properties();
        InputStream is = CrowdAuthUtil.class.getClassLoader().getResourceAsStream("authutil.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

        apiApplication = props.getProperty("org.sagebionetworks.crowdApplication");
	}
	

	private static final String msg1 = 
		"<authentication-context>"+
		"<username>";
		
	private static final String msg2 = "</username><password>";
	
	private static final String msg3 = "</password>";
	
	// used for validation and also for re-validation
	private static final String msg4 = "<validation-factors>"+
	"</validation-factors>";
	
	private static final String msg5 =  "</authentication-context>";
		
		
	public static String getFromXML(String xPath, byte[] xml) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			return xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)));
	}
	
	public static List<String> getMultiFromXML(String xPath, byte[] xml) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nl = (NodeList) xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)), XPathConstants.NODESET);
			List<String> ans = new ArrayList<String>();
			for (int i=0; i<nl.getLength(); i++) ans.add(nl.item(i).getTextContent());
			return ans;
	}
	
	public static Map<String,String> getHeaders() {
		Map<String,String> ans = new HashMap<String,String>();
		ans.put("Accept", "application/xml");
		ans.put("Content-Type", "application/xml");
		String authString=apiApplication+":"+API_APPLICATION_KEY;
		ans.put("Authorization", "Basic "+new String(Base64.encodeBase64(authString.getBytes()))); 
		return ans;
	}
	
	public static String urlPrefix() {
		return CROWD_URL+"/crowd/rest/usermanagement/latest";
	}
	
	public static byte[] executeRequest(String requestURL, 
			String requestMethod, 
			String requestContent,
			HttpStatus expectedRc, 
			String failureReason) throws AuthenticationException {
		try {
			HttpResponse response = null;
			try {
			    response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), requestURL,
					requestMethod, requestContent,
					getHeaders());
			    if(expectedRc.value() != response.getStatusLine().getStatusCode()) {
					throw new AuthenticationException(response.getStatusLine().getStatusCode(), failureReason, null);			    	
			    }
				byte[] respBody = (readInputStream(response.getEntity().getContent())).getBytes();
				return respBody;
			} catch (HttpClientHelperException hche) {
				throw new AuthenticationException(hche.getHttpStatus(), failureReason, hche);
			} 
			
		} catch (IOException e) {
			throw new AuthenticationException(500, failureReason, e);
		}
	}
	
	public static void executeRequestNoResponseBody(String requestURL, 
			String requestMethod, 
			String requestContent,
			HttpStatus expectedRc, 
			String failureReason) throws AuthenticationException {
		int rc = 500;
		try {
			HttpResponse response = null;
			try {
			    response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), requestURL,
					requestMethod, requestContent,
					getHeaders());
			    if(expectedRc.value() != response.getStatusLine().getStatusCode()) {
					throw new AuthenticationException(response.getStatusLine().getStatusCode(), failureReason, null);			    	
			    }
				return;
			} catch (HttpClientHelperException hche) {
				throw new AuthenticationException(hche.getHttpStatus(), failureReason, hche);
			} 
		} catch (IOException e) {
			throw new AuthenticationException(rc, failureReason, e);
		}
	}
	
	/**
	 * Authenticates a user/password combination, returning a session token if valid
	 * 
	 * The option validatePassword=false is available for SSO applications:  A service
	 * which has independently validated the user may use this variation to get a session token
	 * for the named user.  Note:  In this case the password must not be omitted, according to Atlassian.  
	 * For more info, see   http://jira.atlassian.com/browse/CWD-2152
	 */
	public static Session authenticate(User creds, boolean validatePassword) throws AuthenticationException, IOException, XPathExpressionException {
		byte[] sessionXML = null;
		{
			sessionXML = executeRequest(urlPrefix()+"/session?validate-password="+validatePassword, 
					"POST", 
					msg1+creds.getEmail()+msg2+creds.getPassword()+msg3+msg4+msg5+"\n",
					HttpStatus.CREATED, 
					"Unable to authenticate");
		}

		String token = getFromXML("/session/token", sessionXML);
		{
			sessionXML = executeRequest(urlPrefix()+"/user?username="+creds.getEmail(), 
					"GET", 
					"",
					HttpStatus.OK, 
					"Authenticated user "+creds.getEmail()+
					" but subsequentially could not retrieve attributes from server. \n");

		}
		String displayName = getFromXML("/user/display-name", sessionXML);
		return new Session(token, displayName);
	}
	
	public static String revalidate(String sessionToken) throws AuthenticationException, IOException, XPathExpressionException {
		log.info("Revalidating: "+sessionToken);
		byte[] sessionXML = executeRequest(urlPrefix()+"/session/"+sessionToken,
				"POST",
				msg4+"\n",
				HttpStatus.OK,
				"Unable to validate session.");
		
		return getFromXML("/session/user/@name", sessionXML);
	}
	
	public static void deauthenticate(String token) throws AuthenticationException, IOException {
		executeRequestNoResponseBody(urlPrefix()+"/session/"+token,
				"DELETE",
				"",
				HttpStatus.NO_CONTENT, 
				"Unable to invalidate session.");
	}
	
	private static String userXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?>\n"+
		  "<user name='"+user.getEmail()+"' expand='attributes'>\n"+
		  "\t<first-name>"+user.getFirstName()+"</first-name>\n"+
		  "\t<last-name>"+user.getLastName()+"</last-name>\n"+
		  "\t<display-name>"+user.getDisplayName()+"</display-name>\n"+
		  "\t<email>"+user.getEmail()+"</email>\n"+
		  "\t<active>true</active>\n"+
		  (user.getPassword()==null ? "" : 
			  "\t<password>"+
			  "<value>"+user.getPassword()+"</value>"+
			  "</password>\n"
		  )+
		  "</user>\n";
	}
	
	
	private static String userPasswordXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?><password><value>"+
			user.getPassword()+"</value></password>";
	}
	
	public static void createUser(User user) throws AuthenticationException, IOException {
		// input:  userid, pw, email, fname, lname, display name
		// POST /user
		executeRequest(urlPrefix()+"/user", "POST", userXML(user)+"\n", HttpStatus.CREATED, "Unable to create user.");
		
		// set created date
		Map<String,Collection<String>> attributes = new HashMap<String,Collection<String>>();
		attributes.put(AuthorizationConstants.CREATION_DATE_FIELD, Arrays.asList(new String[]{DateTime.now().toString()}));
		setUserAttributes(user.getEmail(), attributes);
	}
	
	public static User getUser(String userId) throws IOException {
		byte[] sessionXML = null;
		try {
			sessionXML = executeRequest(urlPrefix()+"/user?username="+userId, "GET", "", HttpStatus.OK, "Unable to get "+userId+".");
		} catch (AuthenticationException e) {
			throw new RuntimeException(e.getRespStatus()+" "+e.getMessage());
		}

		try {
			User user = new User();
			user.setEmail(getFromXML("/user/email", sessionXML));
			user.setFirstName(getFromXML("/user/first-name", sessionXML));
			user.setLastName(getFromXML("/user/last-name", sessionXML));
			user.setDisplayName(getFromXML("/user/display-name", sessionXML));
			return user;
		} catch (XPathExpressionException xee) {
			throw new RuntimeException(xee);
		}
	}
	
	public static void deleteUser(String userEmail) throws AuthenticationException, IOException {
		executeRequestNoResponseBody(urlPrefix()+"/user?username="+userEmail, "DELETE", "", HttpStatus.NO_CONTENT, "Unable to delete "+userEmail);
	}

	/**
	 * Update user attributes (not password).
	 */
	public static void updateUser(User user) throws AuthenticationException, IOException {
					
			// Atlassian documentation says it will return 200 (OK) but it actually returns 204 (NO CONTENT)
			executeRequestNoResponseBody(urlPrefix()+"/user?username="+user.getEmail(), 
					"PUT", 
					userXML(user)+"\n", 
					HttpStatus.NO_CONTENT, 
					"Unable to update user.");
	}
	
	/**
	 * Update password
	 */
	public static void updatePassword(User user) throws AuthenticationException, IOException {
			executeRequestNoResponseBody(urlPrefix()+"/user/password?username="+user.getEmail(),
					"PUT", userPasswordXML(user)+"\n", HttpStatus.NO_CONTENT, "Unable to update user password.");
	}
	
	public static void sendResetPWEmail(User user) throws AuthenticationException, IOException {
		// POST /user/mail/password?username=USERNAME
		executeRequestNoResponseBody(urlPrefix()+"/user/mail/password?username="+user.getEmail(),
				"POST",
				"",
				HttpStatus.NO_CONTENT, "Unable to send reset-password message.");
	}
		
	// Note, this seems to be 'idempotent', i.e. you CAN add a user to a group which the user is already in
	public static void addUserToGroup(String group, String userId) throws AuthenticationException, IOException {
		executeRequest(urlPrefix()+"/group/user/direct?groupname="+group, "POST", 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?> <user name=\""+userId+"\"/>\n",
				HttpStatus.CREATED, "Unable to add user "+userId+" to group "+group+".");
	}
	
	public static Collection<String> getUsersInGroup(String group) throws AuthenticationException, IOException {
		byte[] sessionXML =	executeRequest(urlPrefix()+"/group/user/direct?groupname="+group,
				"GET", "",
				HttpStatus.OK, "Unable to get users in "+group+".");
		try {
			return getMultiFromXML("users/user/@name", sessionXML);
		} catch (XPathExpressionException xee) {
			throw new AuthenticationException(500, "Server Error", xee);
		}
	}
	
	public static boolean userExists(String userId) throws IOException {
		throw new RuntimeException("Not yet implemented.");
		// uri: /user?username=USERNAME (GET), 404 if user can't be found
	}
	
	public static Map<String,Collection<String>> getUserAttributes(String userId/*, Collection<String> attributes*/) throws IOException, NotFoundException {
		byte[] sessionXML =	null;
		try {
			sessionXML = executeRequest(urlPrefix()+"/user?expand=attributes&username="+userId, 
					"GET", "",
					HttpStatus.OK, "Unable to get attributes for "+userId+".");
		} catch (AuthenticationException e) {
			throw new NotFoundException(e.getRespStatus()+" "+e.getMessage());
		}

		try {
			Collection<String> attributes = getMultiFromXML("/user/attributes/attribute/@name", sessionXML);
			Map<String,Collection<String>> ans = new HashMap<String,Collection<String>>();
			for (String attribute: attributes) {
				Collection<String> values = getMultiFromXML("/user/attributes/attribute[@name=\""+attribute+"\"]/values/value", sessionXML);
				ans.put(attribute, values);
			}
			return ans;
		} catch (XPathExpressionException xee) {
			throw new RuntimeException(xee);
		}
	}
	
	public static String encodeAttributesAsXML(Map<String,Collection<String>> attributes) {
		StringBuilder sb = new StringBuilder();
		sb.append("<attributes>");
		for (String a : attributes.keySet()) {
			sb.append("<attribute name=\""+a+"\"><values>");
			for (String v : attributes.get(a)) {
				sb.append("<value>"+v+"</value>");
			}
			sb.append("</values></attribute>");
		}
		sb.append("</attributes>");
		return sb.toString();
	}
	
	/**
	 * NOTE:
	 * The behavior of Crowd is:
	 * (1) Setting attributes is ADDITIVE, in that if you POST a bunch of attribute/value pairs using NEW attributes, 
	 * they are added to the existing pairs for the user, they don't overwrite the existing values;
	 * (2) BUT if the attributes are already used for the user, the value is overwritten (even though Crowd supports 
	 * multiple values for a given attribute!)
	 * 
	 */
	public static void setUserAttributes(String userId, Map<String,Collection<String>> attributes) throws IOException {
		try {
			executeRequestNoResponseBody(urlPrefix()+"/user/attribute?username="+userId, 
					"POST", 
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+encodeAttributesAsXML(attributes)+"\n",
					HttpStatus.NO_CONTENT, "Unable to set attributes for "+userId+".");
		} catch (AuthenticationException e) {
			throw new RuntimeException(e.getRespStatus()+" "+e.getMessage());
		}

	}
	
	public static void deleteUserAttribute(String userId, String attribute) throws IOException {
		try {
			executeRequestNoResponseBody(urlPrefix()+"/user/attribute?username="+userId+"&attributename="+attribute, 
					"DELETE", 
					"",
					HttpStatus.NO_CONTENT, "Unable to delete attribute "+attribute+" for "+userId+".");
		} catch (AuthenticationException e) {
			throw new RuntimeException(e.getRespStatus()+" "+e.getMessage());
		}

	}
	
	
	
	public static Collection<String> getUsersGroups(String userId) throws IOException, NotFoundException {
		byte[] sessionXML = null;
		try {
			sessionXML = executeRequest(urlPrefix()+"/user/group/direct?username="+userId,
					"GET", "",
					HttpStatus.OK, "Unable to get groups for "+userId+".");
		} catch (AuthenticationException e) {
			throw new RuntimeException(e.getRespStatus()+" "+e.getMessage());
		}

		try {
			Collection<String> ans = getMultiFromXML("/groups/group/@name", sessionXML);
			return ans;
		} catch (XPathExpressionException xee) {
			throw new RuntimeException(xee);
		}
	}
	
	public static String readInputStream(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		int i=-1;
		do {
			i = is.read();
			if (i>0) sb.append((char)i);
		} while (i>0);
		return sb.toString().trim();
	}
	
	/**
	 * This is an alternate solution to the one below.  Note:   It's for 'HttpsHRLConnection', not the Apache HttpClient
	 */
	public static void acceptAllCertificatesNOTUSED() {
		Security.addProvider( new MyProvider() );
		Security.setProperty("ssl.TrustManagerFactory.algorithm", "TrustAllCertificates");
		
		// from http://stackoverflow.com/questions/2186543/java-secure-webservice
		HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String urlHostName, SSLSession session) {
		        if (!urlHostName.equals(session.getPeerHost())) System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
		        return true;
		    }
		};

		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	
	/**
	 * This was the version being used, before going to the Apache HttpClient
	 */
	public static void acceptAllCertificates() {
		// from http://www.exampledepot.com/egs/javax.net.ssl/trustall.html
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
		    new X509TrustManager() {
		        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return null;
		        }
		        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		        }
		        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		        }
		    }
		};

		// Install the all-trusting trust manager
		try {
		    // SSLContext sc = SSLContext.getInstance("SSL");
		    SSLContext sc = SSLContext.getInstance("TLS");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom()); // this ref says, the 3rd arg should be null http://javaskeleton.blogspot.com/2010/07/avoiding-peer-not-authenticated-with.html
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//		    SSLSocketFactory ssf = new SSLSocketFactory(sc); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// from http://stackoverflow.com/questions/2186543/java-secure-webservice
		HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String urlHostName, SSLSession session) {
		        if (!urlHostName.equals(session.getPeerHost())) System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
		        return true;
		    }
		};

		HttpsURLConnection.setDefaultHostnameVerifier(hv);

	}
	
	
	public static boolean isAdmin(String userName) throws NotFoundException {
		try {
			return getUsersGroups(userName).contains(AuthorizationConstants.ADMIN_GROUP_NAME);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
