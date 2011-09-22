package org.sagebionetworks.authutil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpStatus;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CrowdAuthUtil {
	private static final Logger log = Logger.getLogger(CrowdAuthUtil.class.getName());

	private String crowdUrl; // e.g. https://ec2-50-16-158-220.compute-1.amazonaws.com:8443
	private String apiApplication;
	private String apiApplicationKey;

	public CrowdAuthUtil() {
		// get the values from system properties, if available
		crowdUrl = StackConfiguration.getCrowdEndpoint();
		
		
		apiApplicationKey  = StackConfiguration.getCrowdAPIApplicationKey(); 
		
		// !!!!!!! temporary, for testing !!!!!!!!!
//		crowdUrl = "http://localhost:8095";
//		apiApplicationKey = "platform-pw";
		
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
	
	public void setHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("Accept", "application/xml");
		conn.setRequestProperty("Content-Type", "application/xml");
		String authString=apiApplication+":"+apiApplicationKey;
		conn.setRequestProperty("Authorization", "Basic "+new String(Base64.encodeBase64(authString.getBytes()))); 
	}
	
	public static void setBody(HttpURLConnection conn, String body) {
		conn.setDoOutput(true);
		try {
	        Writer wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(body+"\n");
			wr.flush();
			wr.close();				
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	public String urlPrefix() {
		return crowdUrl+"/crowd/rest/usermanagement/latest";
	}
	
	public byte[] executeRequest(HttpURLConnection conn, HttpStatus expectedRc, String failureReason) throws AuthenticationException {
		try {
			int rc = conn.getResponseCode();
			if (expectedRc.value()==rc) {
				byte[] respBody = (readInputStream((InputStream)conn.getContent())).getBytes();
				return respBody;
			} else {
				byte[] respBody = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(rc, failureReason, new Exception(new String(respBody)));
			}
		} catch (IOException e) {
			throw new AuthenticationException(500, failureReason, null);
		}
	}
	
	public void executeRequestNoResponseBody(HttpURLConnection conn, HttpStatus expectedRc, String failureReason) throws AuthenticationException {
		int rc = 500;
		try {
			rc = conn.getResponseCode();
			if (expectedRc.value()==rc) {
				return;
			} else {
				byte[] respBody = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(rc, failureReason, new Exception(new String(respBody)));
			}
		} catch (IOException e) {
			throw new AuthenticationException(rc, failureReason, null);
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
	public Session authenticate(User creds, boolean validatePassword) throws AuthenticationException, IOException, XPathExpressionException {
		byte[] sessionXML = null;
		{
			URL url = new URL(urlPrefix()+"/session?validate-password="+validatePassword);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			setHeaders(conn);
			setBody(conn, msg1+creds.getEmail()+msg2+creds.getPassword()+msg3+msg4+msg5+"\n");
//			long start = System.currentTimeMillis();
			sessionXML = executeRequest(conn, HttpStatus.CREATED, "Unable to authenticate");
//			System.out.println("\tAuthentiation took "+(System.currentTimeMillis()-start)+" ms.");
		}

		String token = getFromXML("/session/token", sessionXML);
		{
			URL url = new URL(urlPrefix()+"/user?username="+creds.getEmail());
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			setHeaders(conn);
//			long start = System.currentTimeMillis();
			sessionXML = executeRequest(conn, HttpStatus.OK, "Authenticated user "+creds.getEmail()+
					" but subsequentially could not retrieve attributes from server. \n");
//			System.out.println("\tGetting display name took "+(System.currentTimeMillis()-start)+" ms.");

		}
		String displayName = getFromXML("/user/display-name", sessionXML);
		return new Session(token, displayName);
	}
	
	public String revalidate(String sessionToken) throws AuthenticationException, IOException, XPathExpressionException {
		URL url = new URL(urlPrefix()+"/session/"+sessionToken);
		
		log.info("Revalidating: "+sessionToken);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		setBody(conn,msg4+"\n");
		byte[] sessionXML = executeRequest(conn, HttpStatus.OK, "Unable to validate session.");
		
		return getFromXML("/session/user/@name", sessionXML);
	}
	
	public void deauthenticate(String token) throws AuthenticationException, IOException {
		URL url = new URL(urlPrefix()+"/session/"+token);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("DELETE");
		setHeaders(conn);
		executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to invalidate session.");
	}
	
	private static String userXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?>\n"+
		  "<user name='"+user.getEmail()+"' expand='attributes'>\n"+
		  "\t<first-name>"+user.getFirstName()+"</first-name>\n"+
		  "\t<last-name>"+user.getLastName()+"</last-name>\n"+
		  "\t<display-name>"+user.getDisplayName()+"</display-name>\n"+
		  "\t<email>"+user.getEmail()+"</email>\n"+
		  "\t<active>true</active>\n"+
//		  "\t<attributes>"+
//		  "<link rel='self' href='link_to_user_attributes'/>"+
//		  "</attributes>\n"+
		  (user.getPassword()==null ? "" : 
			  "\t<password>"+
//			  "<link rel='edit' href='link_to_user_password'/>"+
			  "<value>"+user.getPassword()+"</value>"+
			  "</password>\n"
		  )+
		  "</user>\n";
	}
	
	
	private static String userPasswordXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?><password><value>"+
			user.getPassword()+"</value></password>";
	}
	
	public void createUser(User user) throws AuthenticationException, IOException {
		// input:  userid, pw, email, fname, lname, display name
		// POST /user
		URL url = new URL(urlPrefix()+"/user");
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		//log.info("Request body for 'createUser':"+userXML(user));
		setBody(conn, userXML(user)+"\n");
		executeRequest(conn, HttpStatus.CREATED, "Unable to create user.");
		
		// set created date
		Map<String,Collection<String>> attributes = new HashMap<String,Collection<String>>();
		attributes.put(AuthUtilConstants.CREATION_DATE_FIELD, Arrays.asList(new String[]{DateTime.now().toString()}));
		setUserAttributes(user.getEmail(), attributes);
	}
	
	public User getUser(String userId) throws IOException {
		URL url = new URL(urlPrefix()+"/user?username="+userId);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		
		byte[] sessionXML = null;
		try {
			sessionXML = executeRequest(conn, HttpStatus.OK, "Unable to get "+userId+".");
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
	
	public void deleteUser(User user) throws AuthenticationException, IOException {
		URL url = new URL(urlPrefix()+"/user?username="+user.getEmail());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("DELETE");
		setHeaders(conn);
		executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to delete "+user.getEmail());
	}

	/**
	 * Update user attributes (not password).
	 */
	public void updateUser(User user) throws AuthenticationException, IOException {
		
			URL url = new URL(urlPrefix()+"/user?username="+user.getEmail());
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("PUT");
			setHeaders(conn);
			setBody(conn, userXML(user)+"\n");
			
			// Atlassian documentation says it will return 200 (OK) but it actually returns 204 (NO CONTENT)
			executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to update user.");
	}
	
	/**
	 * Update password
	 */
	public void updatePassword(User user) throws AuthenticationException, IOException {
			URL url = new URL(urlPrefix()+"/user/password?username="+user.getEmail());
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("PUT");
			setHeaders(conn);
			setBody(conn, userPasswordXML(user)+"\n");
			
			executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to update user password.");
	}
	
	public void sendResetPWEmail(User user) throws AuthenticationException, IOException {
		// POST /user/mail/password?username=USERNAME
		URL url = new URL(urlPrefix()+"/user/mail/password?username="+user.getEmail());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to send reset-password message.");
	}
		
	// Note, this seems to be 'idempotent', i.e. you CAN add a user to a group which the user is already in
	public void addUserToGroup(String group, String userId) throws AuthenticationException, IOException {
		URL url = new URL(urlPrefix()+"/group/user/direct?groupname="+group);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		setBody(conn, "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <user name=\""+userId+"\"/>\n");
		executeRequest(conn, HttpStatus.CREATED, "Unable to add user "+userId+" to group "+group+".");
	}
	
	public Collection<String> getUsersInGroup(String group) throws AuthenticationException, IOException {
		URL url = new URL(urlPrefix()+"/group/user/direct?groupname="+group);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		byte[] sessionXML =	executeRequest(conn, HttpStatus.OK, "Unable to get users in "+group+".");
		try {
			return getMultiFromXML("users/user/@name", sessionXML);
		} catch (XPathExpressionException xee) {
			throw new AuthenticationException(500, "Server Error", xee);
		}
	}
	
	public boolean userExists(String userId) throws IOException {
		throw new RuntimeException("Not yet implemented.");
		// uri: /user?username=USERNAME (GET), 404 if user can't be found
	}
	
	public Map<String,Collection<String>> getUserAttributes(String userId/*, Collection<String> attributes*/) throws IOException, NotFoundException {
		URL url = new URL(urlPrefix()+"/user?expand=attributes&username="+userId);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		byte[] sessionXML =	null;
		try {
			sessionXML = executeRequest(conn, HttpStatus.OK, "Unable to get attributes for "+userId+".");
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
	
	public void setUserAttributes(String userId, Map<String,Collection<String>> attributes) throws IOException {
		URL url = new URL(urlPrefix()+"/user/attribute?username="+userId);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		setBody(conn, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+encodeAttributesAsXML(attributes)+"\n");
		
		try {
			executeRequestNoResponseBody(conn, HttpStatus.NO_CONTENT, "Unable to set attributes for "+userId+".");
		} catch (AuthenticationException e) {
			throw new RuntimeException(e.getRespStatus()+" "+e.getMessage());
		}

	}
	
	public Collection<String> getUsersGroups(String userId) throws IOException, NotFoundException {
		URL url = new URL(urlPrefix()+"/user/group/direct?username="+userId);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		byte[] sessionXML = null;
		try {
			sessionXML = executeRequest(conn, HttpStatus.OK, "Unable to get groups for "+userId+".");
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
	 * This is an alternate solution to the one above.
	 */
	public static void acceptAllCertificates() {
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
	 * This is the version currently being used.
	 */
	public static void acceptAllCertificates2() {
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
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
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
}
