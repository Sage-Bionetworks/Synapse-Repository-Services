package org.sagebionetworks.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * @author deflaux
 * 
 */
public class HttpClientHelper {

	/**
	 * Download the content to a file or stream if it is larger than this length
	 */
	public static final int MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH = 1024 * 1024;

	private static final int DEFAULT_CONNECT_TIMEOUT_MSEC = 5000;
	private static final int DEFAULT_SOCKET_TIMEOUT_MSEC = 20000;

	// Note: Having this 'password' in plaintext is OK because (1) it's a well
	// known default for key stores,
	// and (2) the keystore (below) contains only public certificates.
	private static final String DEFAULT_JAVA_KEYSTORE_PW = "changeit";
	private static final String KEYSTORE_NAME = "HttpClientHelperPublicCertsOnly.jks";

	/**
	 * Create a new HTTP client connection factory.
	 * 
	 * @param verifySSLCertificates
	 * @return the HTTP client connection factory
	 */
	public static HttpClient createNewClient(boolean verifySSLCertificates) {
		try {
			ThreadSafeClientConnManager connectionManager = createClientConnectionManager(verifySSLCertificates);
			HttpParams clientParams = new BasicHttpParams();
			clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
					DEFAULT_CONNECT_TIMEOUT_MSEC);
			clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT,
					DEFAULT_SOCKET_TIMEOUT_MSEC);

			return new DefaultHttpClient(connectionManager, clientParams);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param verifySSLCertificates
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
	public static ThreadSafeClientConnManager createClientConnectionManager(
			boolean verifySSLCertificates) throws NoSuchAlgorithmException,
			KeyStoreException, CertificateException, IOException,
			KeyManagementException {
		SSLContext ctx = null;
		X509HostnameVerifier hostNameVarifier = null;
		// Should certificates be checked.
		if (verifySSLCertificates) {
			ctx = createSecureSSLContext();
			hostNameVarifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
		} else {
			// This will allow any certificate.
			ctx = createEasySSLContext();
			hostNameVarifier = new AcceptAnyHostName();
		}

		SchemeSocketFactory ssf = new SSLSocketFactory(ctx,
				hostNameVarifier);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, ssf));
		// schemeRegistry.register(new Scheme("https", 8443, ssf));

		// This is the maximum number connections to a single route
		// This was increased from 2 to 100 to address SWC-
		int maxConnectionsPerRoute = 100;
		// TODO its unclear how to set a default for the timeout in
		// milliseconds
		// used when retrieving an
		// instance of ManagedClientConnection from the
		// ClientConnectionManager
		// since parameters are now deprecated for connection managers.
		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(
				schemeRegistry,
				-1,// max connection lifetime, <=0 implies "infinity"
				TimeUnit.MILLISECONDS,
				new ConnPerRouteBean(maxConnectionsPerRoute));
		return connectionManager;
	}

	/**
	 * The resulting SSLContext will validate
	 * 
	 * @return the SSLContext with validation
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
	public static SSLContext createSecureSSLContext()
			throws NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException, KeyManagementException {
		// from
		// http://www.coderanch.com/t/372437/java/java/javax-net-ssl-keyStore-system
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream keystoreStream = HttpClientHelper.class.getClassLoader()
				.getResourceAsStream(KEYSTORE_NAME);
		keystore.load(keystoreStream, DEFAULT_JAVA_KEYSTORE_PW.toCharArray());
		trustManagerFactory.init(keystore);
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		SSLContext ctx = SSLContext.getInstance("TLS"); // was SSL
		ctx.init(null, trustManagers, null);
		return ctx;
	}

	/**
	 * The resulting SSLContext will allow any certificate.
	 * 
	 * @return the SSLContext that will allow any certificate
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
	public static SSLContext createEasySSLContext()
			throws NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException, KeyManagementException {
		SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null,
				new TrustManager[] { new AcceptAnyCertificatManager() }, null);
		return sslcontext;
	}

	/**
	 * A trust manager that accepts all certificates.
	 * 
	 * @author jmhill
	 * 
	 */
	private static class AcceptAnyCertificatManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// Oh, I am easy!
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// Oh, I am easy!
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	};

	/**
	 * Accepts any host name.
	 * 
	 * @author jmhill
	 * 
	 */
	private static class AcceptAnyHostName implements X509HostnameVerifier {

		@Override
		public boolean verify(String arg0, SSLSession arg1) {
			return true;
		}

		@Override
		public void verify(String host, SSLSocket ssl) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public void verify(String host, X509Certificate cert)
				throws SSLException {
			// TODO Auto-generated method stub

		}

		@Override
		public void verify(String host, String[] cns, String[] subjectAlts)
				throws SSLException {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Get the Connection timeout on the passed client
	 * 
	 * @param client
	 * @param milliseconds
	 */
	public static void setGlobalConnectionTimeout(HttpClient client,
			int milliseconds) {
		client.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, milliseconds);
	}

	/**
	 * Set the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout
	 * for waiting for data or, put differently, a maximum period inactivity
	 * between two consecutive data packets). A timeout value of zero is
	 * interpreted as an infinite timeout. This will change the configuration
	 * for all requests.
	 * 
	 * @param client
	 * 
	 * @param milliseconds
	 */
	public static void setGlobalSocketTimeout(HttpClient client,
			int milliseconds) {
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				milliseconds);
	}

	/**
	 * Perform a request using the provided Client.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return the response object
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static HttpResponse performRequest(HttpClient client,
			String requestUrl, String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		HttpEntity requestEntity = null;
		if (null != requestContent) {
			requestEntity = new StringEntity(requestContent);
		}

		return performEntityRequest(client, requestUrl, requestMethod,
				requestEntity, requestHeaders);
	}

	/**
	 * Perform a request using the provided Client.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestEntity
	 * @param requestHeaders
	 * @return the response object
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static HttpResponse performEntityRequest(HttpClient client,
			String requestUrl, String requestMethod, HttpEntity requestEntity,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		HttpRequestBase request = null;
		if (requestMethod.equals("GET")) {
			request = new HttpGet(requestUrl);
		} else if (requestMethod.equals("POST")) {
			request = new HttpPost(requestUrl);
			if (null != requestEntity) {
				((HttpEntityEnclosingRequestBase) request)
						.setEntity(requestEntity);
			}
		} else if (requestMethod.equals("PUT")) {
			request = new HttpPut(requestUrl);
			if (null != requestEntity) {
				((HttpEntityEnclosingRequestBase) request)
						.setEntity(requestEntity);
			}
		} else if (requestMethod.equals("DELETE")) {
			request = new HttpDelete(requestUrl);
		}

		if (null != requestHeaders) {
			for (Entry<String, String> header : requestHeaders.entrySet()) {
				request.setHeader(header.getKey(), header.getValue());
			}
		}

		HttpResponse response = client.execute(request);

		if (300 <= response.getStatusLine().getStatusCode()) {
			StringBuilder verboseMessage = new StringBuilder(
					"FAILURE: Got HTTP status "
							+ response.getStatusLine().getStatusCode()
							+ " for " + requestUrl);

			// TODO this potentially prints out headers such as sessionToken to
			// our logs, consider whether this is a good idea
			if ((null != requestHeaders) && (0 < requestHeaders.size())) {
				verboseMessage.append("\nHeaders: ");
				for (Entry<String, String> entry : requestHeaders.entrySet()) {
					verboseMessage.append("\n\t" + entry.getKey() + ": "
							+ entry.getValue());
				}
			}
			if (null != requestEntity) {
				verboseMessage.append("\nRequest Content: " + requestEntity);
			}
			String responseBody = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;
			verboseMessage.append("\nResponse Content: " + responseBody);
			throw new HttpClientHelperException(verboseMessage.toString(), response.getStatusLine().getStatusCode(), responseBody);
		}
		return response;
	}

	/**
	 * Get content as a string using the provided HttpClient.
	 * 
	 * @param client
	 * @param requestUrl
	 * @return the content returned in a string
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static String getContent(final HttpClient client,
			final String requestUrl) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		String responseContent = null;

		HttpResponse response = HttpClientHelper.performRequest(client,
				requestUrl, "GET", null, null);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < entity
					.getContentLength()) {
				throw new HttpClientHelperException("Requested content("
						+ requestUrl + ") is too large("
						+ entity.getContentLength()
						+ "), download it to a file instead", response.getStatusLine().getStatusCode(), responseContent);

			}
			responseContent = EntityUtils.toString(entity);
		}
		return responseContent;
	}

	/**
	 * Get content as a file using the provided HttpClient
	 * 
	 * @param client
	 * @param requestUrl
	 * @param file
	 *            if null a temp file will be created
	 * @return the file
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static File getContent(final HttpClient client,
			final String requestUrl, File file) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		if (null == file) {
			file = File.createTempFile(HttpClientHelper.class.getName(), "tmp");
		}

		HttpResponse response = HttpClientHelper.performRequest(client,
				requestUrl, "GET", null, null);
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileEntity.writeTo(fileOutputStream);
			fileOutputStream.close();
		}
		return file;
	}

	/**
	 * Post content provided as a string using the provided HttpClient.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param requestContent
	 * @param requestHeaders
	 * @return the content returned in a string
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static String postContent(final HttpClient client,
			final String requestUrl, final String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		String responseContent = null;

		HttpResponse response = HttpClientHelper.performRequest(client,
				requestUrl, "POST", requestContent, requestHeaders);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < entity
					.getContentLength()) {
				throw new HttpClientHelperException("Requested content("
						+ requestUrl + ") is too large("
						+ entity.getContentLength()
						+ "), download it to a file instead", response.getStatusLine().getStatusCode(), responseContent);
			}
			responseContent = EntityUtils.toString(entity);
		}
		return responseContent;
	}

	/**
	 * Post content provided as an InputStream using the provided HttpClient.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param stream
	 * @param length
	 * @param requestHeaders
	 * @return the response, if any
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static String postStream(final HttpClient client,
			final String requestUrl, final InputStream stream,
			final long length, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {

		String responseContent = null;

		InputStreamEntity requestEntity = new InputStreamEntity(stream, length);

		HttpResponse response = HttpClientHelper.performEntityRequest(client,
				requestUrl, "POST", requestEntity, requestHeaders);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < entity
					.getContentLength()) {
				throw new HttpClientHelperException("Requested content("
						+ requestUrl + ") is too large("
						+ entity.getContentLength()
						+ "), download it to a file instead", response.getStatusLine().getStatusCode(), responseContent);
			}
			responseContent = EntityUtils.toString(entity);
		}
		return responseContent;
	}

	/**
	 * Put content provided as an InputStream using the provided HttpClient.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param stream
	 * @param length
	 * @param requestHeaders
	 * @return the response, if any
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static String putStream(final HttpClient client,
			final String requestUrl, final InputStream stream,
			final long length, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {

		String responseContent = null;

		InputStreamEntity requestEntity = new InputStreamEntity(stream, length);

		HttpResponse response = HttpClientHelper.performEntityRequest(client,
				requestUrl, "PUT", requestEntity, requestHeaders);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < entity
					.getContentLength()) {
				throw new HttpClientHelperException("Requested content("
						+ requestUrl + ") is too large("
						+ entity.getContentLength()
						+ "), download it to a file instead", response.getStatusLine().getStatusCode(), responseContent);
			}
			responseContent = EntityUtils.toString(entity);
		}
		return responseContent;
	}

	/**
	 * Download to a file using the provided client. Deprecated: use getContent
	 * instead
	 * 
	 * @param client
	 * @param requestUrl
	 * @param filepath
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	@Deprecated
	public static void downloadFile(final HttpClient client,
			final String requestUrl, final String filepath)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {

		HttpResponse response = HttpClientHelper.performRequest(client,
				requestUrl, "GET", null, null);
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			FileOutputStream fileOutputStream = new FileOutputStream(filepath);
			fileEntity.writeTo(fileOutputStream);
			fileOutputStream.close();
		}
	}

	/**
	 * Upload a file using the provided client. This is currently hardcoded to
	 * do a PUT and pretty specific to S3. Deprecated: use putStream instead.
	 * 
	 * @param client
	 * @param requestUrl
	 * @param filepath
	 * @param contentType
	 * @param requestHeaders
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	@Deprecated
	public static void uploadFile(final HttpClient client,
			final String requestUrl, final String filepath,
			final String contentType, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {

		HttpPut put = new HttpPut(requestUrl);

		FileEntity fileEntity = new FileEntity(new File(filepath), contentType);
		put.setEntity(fileEntity);

		if (null != requestHeaders) {
			for (Entry<String, String> header : requestHeaders.entrySet()) {
				put.addHeader(header.getKey(), header.getValue());
			}
		}

		HttpResponse response = client.execute(put);
		if (300 <= response.getStatusLine().getStatusCode()) {
			String errorMessage = "Request(" + requestUrl + ") failed: "
					+ response.getStatusLine().getReasonPhrase();
			HttpEntity responseEntity = response.getEntity();
			String responseContent = EntityUtils.toString(responseEntity);
			if (null != responseEntity) {
				errorMessage += responseContent;
			}
			throw new HttpClientHelperException(errorMessage, response.getStatusLine().getStatusCode(), responseContent);
		}
	}
}
