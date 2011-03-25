package org.sagebionetworks.authutil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.util.Collection;
import java.util.TreeSet;
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

import org.springframework.http.HttpStatus;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CrowdAuthUtil {
	private static final Logger log = Logger.getLogger(CrowdAuthUtil.class.getName());

	private String protocol; // http or https
	private String host; // the Crowd host
	private int port; // the Crowd port
	
//	   a system parameter for a special userId that's used for integration testing
//	   we need a way to specify a 'back door' userId for integration testing
//	   visible both both the the authentication servlet and the authentication filter
//	   this should not be present in the production deployment
//	   The behavior is as follows
//	  	If passed to the user creation service, there is no confirmation email generated.
//	  	Instead the userId becomes the password.
//	  	If passed as a session token, then no session validation takes place in the 
//	  	authentication filter.  Instead the userId is passed along as a request param.
	private String integrationTestUser;
	
	public String getIntegrationTestUser() {
		return integrationTestUser;
	}

	public void setIntegrationTestUser(String integrationTestUser) {
		this.integrationTestUser = integrationTestUser;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	private static final String msg1 = //"<?xml version='1.0' encoding='UTF-8'?>"+
		"<authentication-context>"+
		"<username>";
		
		private static final String msg2 = "</username><password>";
		
		private static final String msg3 = "</password>";
		
		// used for validation and also for re-validation
		private static final String msg4 = "<validation-factors>"+
//		"<validation-factor>"+
//		"<name>remote_address</name>"+
//		"<value>140.107.179.234</value>"+
//		"</validation-factor>"+
		"</validation-factors>";
		
		private static final String msg5 =  "</authentication-context>";
		
		
		public static String getFromXML(String xPath, byte[] xml) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			return xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)));
	}
	
		public static Collection<String> getMultiFromXML(String xPath, byte[] xml) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nl = (NodeList) xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)), XPathConstants.NODESET);
			Collection<String> ans = new TreeSet<String>();
			for (int i=0; i<nl.getLength(); i++) ans.add(nl.item(i).getTextContent());
			return ans;
	}
	
	public static void setHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("Accept", "application/xml");
		conn.setRequestProperty("Content-Type", "application/xml");
		String authString=AuthUtilConstants.CLIENT+":"+AuthUtilConstants.CLIENT_KEY;
		conn.setRequestProperty("Authorization", "Basic "+Base64.encodeBytes(authString.getBytes())); 
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
		return protocol+"://"+host+":"+port+"/crowd/rest/usermanagement/latest";
	}
	
	
	public Session authenticate(User creds) throws AuthenticationException, IOException, XPathExpressionException {
		byte[] sessionXML = null;
		int rc = 0;
		{
			URL url = new URL(urlPrefix()+"/session?validate-password=true");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			setHeaders(conn);
			setBody(conn, msg1+creds.getUserId()+msg2+creds.getPassword()+msg3+msg4+msg5+"\n");
			try {
				rc = conn.getResponseCode();
				sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
			} catch (IOException e) {
				sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(400, "Unable to authenticate", 
						new Exception(new String(sessionXML)));
			}
		}

		if (HttpStatus.CREATED.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to authenticate", 
					new Exception(new String(sessionXML)));
		}
		String token = getFromXML("/session/token", sessionXML);
		{
			URL url = new URL(urlPrefix()+"/user?username="+creds.getUserId());
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			setHeaders(conn);
			try {
				rc = conn.getResponseCode();
				sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
			} catch (IOException e) {
				sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(500, "Server Error", 
						new Exception(new String(sessionXML)));
			}

		}
		if (HttpStatus.OK.value()!=rc) {
			// this simply shouldn't happen
			throw new IllegalStateException("Authenticated user "+creds.getUserId()+
					" but subsequentially could not retrieve attributes from server. \n"+
					(new String(sessionXML)));
		}
		String displayName = getFromXML("/user/display-name", sessionXML);
		return new Session(token, displayName);
	}
	
	public String revalidate(String sessionToken) throws AuthenticationException, IOException, XPathExpressionException {
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/session/"+sessionToken);
		
		log.info("Revalidating: "+sessionToken);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		setBody(conn,msg4+"\n");
		try {
			rc = conn.getResponseCode();
			sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
		} catch (IOException e) {
			sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
			throw new AuthenticationException(HttpStatus.NOT_FOUND.value(), "Unable to validate session.", 
					new Exception(new String(sessionXML)));
		}

		if (HttpStatus.OK.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to validate session.", 
					new Exception(new String(sessionXML)));
		}
		
		return getFromXML("/session/user/@name", sessionXML);

	}
	
//	/**
//	 * @param sessionToken
//	 * @return userId
//	 * @throws IOException
//	 * @throws XPathExpressionException
//	 */
//	public String revalidate(String sessionToken) throws IOException, XPathExpressionException {
//		log.info("Revalidating: "+sessionToken);
//		byte[] sessionXML = new byte[0];
//		int rc = 0;
//		URL url = new URL(urlPrefix()+"/session/"+sessionToken);
//		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//		conn.setRequestMethod("POST");
//		setHeaders(conn);
//		try {
//			rc = conn.getResponseCode();
//			InputStream is = (InputStream)conn.getContent();
//			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
//		} catch (IOException e) {
//			InputStream is = (InputStream)conn.getErrorStream();
//			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
//			throw new RuntimeException(new String(sessionXML), e);
//		}
//
//		if (HttpStatus.OK.value()!=rc) {
//			throw new RuntimeException(new String(sessionXML));
//		}
//
//		return getFromXML("/session/user/@name", sessionXML);
//
//	}
	
	public void deauthenticate(String token) throws AuthenticationException, IOException {
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/session/"+token);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("DELETE");
		setHeaders(conn);
		try {
			rc = conn.getResponseCode();
		} catch (IOException e) {
			sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
			throw new AuthenticationException(500, "Server Error", 
					new Exception(new String(sessionXML)));
		}

		if (HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to invalidate session.", null);
		}
	}
	
	private static String userXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?>\n"+
		  "<user name='"+user.getUserId()+"' expand='attributes'>\n"+
		  "\t<first-name>"+user.getFirstName()+"</first-name>\n"+
		  "\t<last-name>"+user.getLastName()+"</last-name>\n"+
		  "\t<display-name>"+user.getDisplayName()+"</display-name>\n"+
		  "\t<email>"+user.getEmail()+"</email>\n"+
		  "\t<active>true</active>\n"+
		  "\t<attributes>"+
		  "<link rel='self' href='link_to_user_attributes'/>"+
		  "</attributes>\n"+
		  (user.getPassword()==null ? "" : 
			  "\t<password>"+
			  "<link rel='edit' href='link_to_user_password'/>"+
			  "<value>"+user.getPassword()+"</value>"+
			  "</password>\n"
		  )+
		  "</user>\n";
	}
	
	public void createUser(User user) throws AuthenticationException, IOException {
		// input:  userid, pw, email, fname, lname, display name
		// POST /user
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/user");
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		//log.info("Request body for 'createUser':"+userXML(user));
		setBody(conn, userXML(user)+"\n");
		try {
			rc = conn.getResponseCode();
			InputStream is = (InputStream)conn.getContent();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
		} catch (IOException e) {
			if (rc==0) rc=500;
			InputStream is = (InputStream)conn.getErrorStream();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
			Exception chainedException = null;
			if (sessionXML!=null) chainedException = new Exception(new String(sessionXML));
			throw new AuthenticationException(rc, "Server Error for:\n"+userXML(user), chainedException);
		}

		if (HttpStatus.CREATED.value()!=rc) {
			Exception chainedException = null;
			if (sessionXML!=null) chainedException = new Exception(new String(sessionXML));
			throw new AuthenticationException(rc, "Unable to create user.", chainedException);
		}
		
		addUserToGroup(AuthUtilConstants.PLATFORM_GROUP, user.getUserId());
	}
	
	public void deleteUser(User user) throws AuthenticationException, IOException {
		int rc = 0;
		URL url = new URL(urlPrefix()+"/user?username="+user.getUserId());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("DELETE");
		setHeaders(conn);
		byte[] sessionXML = null;
		try {
			rc = conn.getResponseCode();
		} catch (IOException e) {
			sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
			throw new AuthenticationException(500, "Server Error", 
					new Exception(new String(sessionXML)));
		}

		if (HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to delete "+user.getUserId()+" "+rc, null);
		}
	}

	
	public void updateUser(User user) throws AuthenticationException, IOException {
		int rc = 0;
		URL url = new URL(urlPrefix()+"/user?username="+user.getUserId());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("PUT");
		setHeaders(conn);
		setBody(conn, userXML(user)+"\n");
		byte[] sessionXML = null;
		try {
			rc = conn.getResponseCode();
		} catch (IOException e) {
			sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
			throw new AuthenticationException(500, "Server Error", 
					new Exception(new String(sessionXML)));
		}
		// Atlassian documentation says it will return 200 (OK) but it actually returns 204 (NO CONTENT)
		if (HttpStatus.OK.value()!=rc && HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to update user.", null);
		}
	}
	
	public void sendResetPWEmail(User user) throws AuthenticationException, IOException {
		// POST /user/mail/password?username=USERNAME
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/user/mail/password?username="+user.getUserId());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		try {
			rc = conn.getResponseCode();
			// when successful there IS no input stream!
			// InputStream is = (InputStream)conn.getContent();
			// if (is==null) throw new IOException();
			// sessionXML = (readInputStream(is)).getBytes();
		} catch (IOException e) {
			if (rc==0) rc = 500;
			InputStream es = (InputStream)conn.getErrorStream();
			if (es!=null) sessionXML = (readInputStream(es)).getBytes();
			Exception chainedException = null;
			if (sessionXML!=null) chainedException = new Exception(new String(sessionXML));
			throw new AuthenticationException(rc, "Server Error",  chainedException);
		}

		if (HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to send reset-password message.", 
					new Exception(new String(sessionXML)));
		}
	}
		
	// Note, this seems to be 'idempotent', i.e. you CAN add a user to a group which the user is already in
	public void addUserToGroup(String group, String userId) throws AuthenticationException, IOException {
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/group/user/direct?groupname="+group);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		setHeaders(conn);
		setBody(conn, "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <user name=\""+userId+"\"/>\n");
		try {
			rc = conn.getResponseCode();
			InputStream is = (InputStream)conn.getContent();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
		} catch (IOException e) {
			if (rc==0) rc = 500;
			InputStream es = (InputStream)conn.getErrorStream();
			if (es!=null) sessionXML = (readInputStream(es)).getBytes();
			Exception chainedException = null;
			if (sessionXML!=null) chainedException = new Exception(new String(sessionXML));
			throw new AuthenticationException(rc, "Server Error",  chainedException);
		}

		if (HttpStatus.CREATED.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to add user "+userId+" to group "+group+".", 
					new Exception(new String(sessionXML)));
		}
	}
	
	public Collection<String> getUsersInGroup(String group) throws AuthenticationException, IOException {
		URL url = new URL(urlPrefix()+"/group/user/direct?groupname="+group);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		setHeaders(conn);
		byte[] sessionXML = null;
		int rc = 0;
		try {
			rc = conn.getResponseCode();
			InputStream is = (InputStream)conn.getContent();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
		} catch (IOException e) {
			if (rc==0) rc=500;
			InputStream is = (InputStream)conn.getErrorStream();
			if (is!=null) sessionXML = (readInputStream(is)).getBytes();
			Exception chainedException = null;
			if (sessionXML!=null) chainedException = new Exception(new String(sessionXML));
			throw new AuthenticationException(rc, "Server Error", chainedException);
		}
		try {
			return getMultiFromXML("users/user/@name", sessionXML);
		} catch (XPathExpressionException xee) {
			throw new AuthenticationException(500, "Server Error", xee);
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
	 * 
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
