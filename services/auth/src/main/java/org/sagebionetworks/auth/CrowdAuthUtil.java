package org.sagebionetworks.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.http.HttpStatus;
import org.xml.sax.InputSource;

//import com.google.appengine.api.urlfetch.HTTPHeader;
//import com.google.appengine.api.urlfetch.HTTPMethod;
//import com.google.appengine.api.urlfetch.HTTPRequest;
//import com.google.appengine.api.urlfetch.HTTPResponse;
//import com.google.appengine.api.urlfetch.URLFetchService;
//import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class CrowdAuthUtil {
//	private static URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
	private static final Logger log = Logger.getLogger(CrowdAuthUtil.class.getName());

	private String protocol; // http or https
	private String host; // the Crowd host
	private int port; // the Crowd port
	
	/**
	 * 
	 * @param host the Crowd host (name or IP address)
	 * @param port the Crowd port
	 */
	public CrowdAuthUtil(String protocol, String host, int port) {
		this.protocol=protocol;
		this.host=host;
		this.port=port;
	}

	private static final String CLIENT = "platform";
	private static final String CLIENT_KEY = "platform-pw";

	private static final String msg1 = //"<?xml version='1.0' encoding='UTF-8'?>"+
		"<authentication-context>"+
		"<username>";
		
		private static final String msg2 = "</username><password>";
		
		private static final String msg3 = "</password>";
		
		// used for validation and also for re-validation
		private static final String msg4 = "<validation-factors>"+
		"<validation-factor>"+
		"<name>remote_address</name>"+
		"<value>140.107.179.234</value>"+
		"</validation-factor>"+
		"</validation-factors>";
		
		private static final String msg5 =  "</authentication-context>";
		
		
	public static String getFromXML(String xPath, byte[] xml) throws XPathExpressionException {
			XPath xpath = XPathFactory.newInstance().newXPath();
			return xpath.evaluate(xPath, new InputSource(new ByteArrayInputStream(xml)));
	}

//	public static void setHeaders(HTTPRequest request) {
//		request.setHeader(new HTTPHeader("Accept", "application/xml"));
//		request.setHeader(new HTTPHeader("Content-Type", "application/xml"));
//		String authString=CLIENT+":"+CLIENT_KEY;
//		request.setHeader(new HTTPHeader("Authorization", "Basic "+Base64.encodeBytes(authString.getBytes())));	
//	}
	
	public static void setHeaders(HttpURLConnection conn) {
		conn.setRequestProperty("Accept", "application/xml");
		conn.setRequestProperty("Content-Type", "application/xml");
		String authString=CLIENT+":"+CLIENT_KEY;
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
	
//	private static final boolean USE_FETCH_SERVICE = false;
	
	public Session authenticate(User creds) throws AuthenticationException, IOException, XPathExpressionException {
		byte[] sessionXML = null;
		int rc = 0;
		{
			URL url = new URL(urlPrefix()+"/session?validate-password=true");
//			if (USE_FETCH_SERVICE) {
//				HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
//				setHeaders(request);
//				request.setPayload((msg1+creds.getUserId()+msg2+creds.getPw()+msg3+"\n").getBytes());
//				HTTPResponse response = urlFetchService.fetch(request);
//				sessionXML = response.getContent();
//				rc = response.getResponseCode();
//			} else {
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
//			}
		}

		if (HttpStatus.CREATED.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to authenticate", 
					new Exception(new String(sessionXML)));
		}
		String token = getFromXML("/session/token", sessionXML);
		{
			URL url = new URL(urlPrefix()+"/user?username="+creds.getUserId());
//			if (USE_FETCH_SERVICE) {
//				HTTPRequest request = new HTTPRequest(url, HTTPMethod.GET);
//				setHeaders(request);
//				HTTPResponse response = urlFetchService.fetch(request);
//				sessionXML = response.getContent();
//				rc = response.getResponseCode();
//			} else {
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
//			}

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
//		if (USE_FETCH_SERVICE) {
//			HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
//			setHeaders(request);
//			HTTPResponse response = urlFetchService.fetch(request);
//			sessionXML = response.getContent();
//			rc = response.getResponseCode();
//		} else {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			setHeaders(conn);
			setBody(conn,msg4+"\n");
			try {
				rc = conn.getResponseCode();
				sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
			} catch (IOException e) {
				sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(500, "Server Error", 
						new Exception(new String(sessionXML)));
			}
//		}

		if (HttpStatus.OK.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to validate session.", 
					new Exception(new String(sessionXML)));
		}
		
		return getFromXML("/session/user/@name", sessionXML);

	}
	
	public void deauthenticate(String token) throws AuthenticationException, IOException {
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/session/"+token);
//		if (USE_FETCH_SERVICE) {
//			HTTPRequest request = new HTTPRequest(url, HTTPMethod.DELETE);
//			setHeaders(request);
//			HTTPResponse response = urlFetchService.fetch(request);
//			sessionXML = response.getContent();
//			rc = response.getResponseCode();
//		} else {
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
//		}

		if (HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to invalidate session.", null);
		}
	}
	
	private static String userXML(User user) {
		return "<?xml version='1.0' encoding='UTF-8'?>"+
		  "<user name='"+user.getUserId()+"' expand='attributes'>"+
		  "<first-name>"+user.getFirstName()+"</first-name>"+
		  "<last-name>"+user.getLastName()+"</last-name>"+
		  "<display-name>"+user.getDisplayName()+"</display-name>"+
		  "<email>"+user.getEmail()+"</email>"+
		  "<active>true</active>"+
		  "<attributes>"+
		  "<link rel='self' href='link_to_user_attributes'/>"+
		  "</attributes>"+
		  "<password>"+
		  "<link rel='edit' href='link_to_user_password'/>"+
		  "<value>"+user.getPassword()+"</value>"+
		  "</password>"+
		  "</user>";
	}
	
	// TODO Reject if email is already used
	public void createUser(User user) throws AuthenticationException, IOException {
		// input:  userid, pw, email, fname, lname, display name
		// POST /user
		byte[] sessionXML = null;
		int rc = 0;
		URL url = new URL(urlPrefix()+"/user");
//		if (USE_FETCH_SERVICE) {
//			HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
//			setHeaders(request);
//			request.setPayload((userXML(user)+"\n").getBytes());
//			HTTPResponse response = urlFetchService.fetch(request);
//			sessionXML = response.getContent();
//			rc = response.getResponseCode();
//		} else {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			setHeaders(conn);
			//log.info("Request body for 'createUser':"+userXML(user));
			setBody(conn, userXML(user)+"\n");
			try {
				rc = conn.getResponseCode();
				sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
			} catch (IOException e) {
				sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(500, "Server Error", 
						new Exception(new String(sessionXML)));
			}
//		}

		if (HttpStatus.CREATED.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to create user.", 
					new Exception(new String(sessionXML)));
		}
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
			throw new AuthenticationException(rc, "Unable to delete user.", null);
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
//		if (USE_FETCH_SERVICE) {
//			HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST);
//			setHeaders(request);
//			HTTPResponse response = urlFetchService.fetch(request);
//			sessionXML = response.getContent();
//			rc = response.getResponseCode();
//		} else {
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			setHeaders(conn);
			try {
				rc = conn.getResponseCode();
				sessionXML = (readInputStream((InputStream)conn.getContent())).getBytes();
			} catch (IOException e) {
				sessionXML = (readInputStream((InputStream)conn.getErrorStream())).getBytes();
				throw new AuthenticationException(500, "Server Error", 
						new Exception(new String(sessionXML)));
			}
//		}

		if (HttpStatus.NO_CONTENT.value()!=rc) {
			throw new AuthenticationException(rc, "Unable to send reset-password message.", 
					new Exception(new String(sessionXML)));
		}
	}
	
	public void getUsersGroups() throws IOException {
		// TODO GET /user/group/direct?username=USERNAME
		// Optional start-index and max-results query params 
	}
	
	public void addUserToGroup() throws IOException {
		// TODO POST /user/group/direct?username=USERNAME
	}
	
	public void removeUserFromGroup() throws IOException {
		// TODO DELETE /user/group/direct?username=USERNAME&groupname=GROUPNAME
	}
	
	private static String readInputStream(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		int i=-1;
		do {
			i = is.read();
			if (i>0) sb.append((char)i);
		} while (i>0);
		return sb.toString().trim();
	}
	
	public static void setUpSSLForTesting() {
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
	
	public static void main(String[] args) throws Exception {
		setUpSSLForTesting();

//		String protocol = "http";
//		String host = "ec2-50-17-17-19.compute-1.amazonaws.com";
//		int port = 8095;
		String protocol = "https";
		String host = "ec2-50-16-158-220.compute-1.amazonaws.com";
		int port = 8443;
		CrowdAuthUtil cau = new CrowdAuthUtil(protocol, host, port);
		User user = new User();
		user.setUserId("demouser");
		user.setPassword("demouser-pw");
		Session session = cau.authenticate(user);
		System.out.println(session);
	}


}
