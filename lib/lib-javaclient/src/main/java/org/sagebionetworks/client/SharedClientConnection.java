package org.sagebionetworks.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Low-level Java Client API for Synapse REST APIs
 */
public class SharedClientConnection {
	
	private static final String SYNAPSE_ENCODING_CHARSET = "UTF-8";

	public static interface ErrorHandler {
		void handleError(int code, String responseBody) throws SynapseException;
	}

	public static final String USER_AGENT = "User-Agent";

	private static final Logger log = LogManager.getLogger(SharedClientConnection.class.getName());

	private static final int JSON_INDENT = 2;
	public static final int MAX_RETRY_SERVICE_UNAVAILABLE_COUNT = 5;
	protected static final String DEFAULT_AUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String REQUEST_PROFILE_DATA = "profile_request";

	protected String authEndpoint;

	protected Map<String, String> defaultGETDELETEHeaders;
	protected Map<String, String> defaultPOSTPUTHeaders;

	private JSONObject profileData;
	private boolean requestProfile;
	private HttpClientProvider clientProvider;

	private String userName;
	private String apiKey;
	private DomainType domain = DomainType.SYNAPSE;
	protected boolean retryRequestIfServiceUnavailable;

	/**
	 * Default constructor uses the default repository and auth services
	 * endpoints.
	 */
	public SharedClientConnection() {
		// Use the default implementations
		this(new HttpClientProviderImpl());
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider 
	 * @param dataUploader 
	 */
	public SharedClientConnection(HttpClientProvider clientProvider) {
		if (clientProvider == null)
			throw new IllegalArgumentException("HttpClientProvider cannot be null");

		setAuthEndpoint(DEFAULT_AUTH_ENDPOINT);

		defaultGETDELETEHeaders = new HashMap<String, String>();
		defaultGETDELETEHeaders.put("Accept", "application/json; charset="+SYNAPSE_ENCODING_CHARSET);

		defaultPOSTPUTHeaders = new HashMap<String, String>();
		defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		defaultPOSTPUTHeaders.put("Content-Type", "application/json; charset="+SYNAPSE_ENCODING_CHARSET);
		
		this.clientProvider = clientProvider;
		clientProvider.setGlobalConnectionTimeout(ServiceConstants.DEFAULT_CONNECT_TIMEOUT_MSEC);
		clientProvider.setGlobalSocketTimeout(ServiceConstants.DEFAULT_SOCKET_TIMEOUT_MSEC);
		
		requestProfile = false;
		//by default, retry if we get a 503
		retryRequestIfServiceUnavailable = true;
	}
	
	public SharedClientConnection(DomainType domain) {
		this(new HttpClientProviderImpl(), domain);
	}

	/**
	 * Will use the provided client provider and data uploader.
	 * 
	 * @param clientProvider 
	 * @param dataUploader 
	 */
	public SharedClientConnection(HttpClientProvider clientProvider, DomainType domain) {
		this(clientProvider);
		this.domain = domain;
	}
	
	
	/**
	 * Use this method to override the default implementation of {@link HttpClientProvider}
	 * @param clientProvider
	 */
	public void setHttpClientProvider(HttpClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}
	
	/**
	 * @param authEndpoint
	 *            the authEndpoint to set
	 */
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	/**
	 * @param request
	 */
	public void setRequestProfile(boolean request) {
		this.requestProfile = request;
	}

	/**
	 * @return JSONObject
	 */
	public JSONObject getProfileData() {
		return this.profileData;
	}
	
	public void setRetryRequestIfServiceUnavailable(
			boolean retryRequestIfServiceUnavailable) {
		this.retryRequestIfServiceUnavailable = retryRequestIfServiceUnavailable;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	/**
	 * 
	 * @param domain set the domain under which this client's users are operating
	 */
	public void setDomain(DomainType domain) {
		this.domain = domain;
	}

	/**
	 * Log into Synapse
	 * 
	 * @return A session token
	 */
	public Session login(String username, String password, String userAgent) throws SynapseException {
		LoginCredentials loginRequest = new LoginCredentials();
		loginRequest.setEmail(username);
		loginRequest.setPassword(password);
		
		Session session;
		try {
			JSONObject obj = createAuthEntity("/session", EntityFactory.createJSONObjectForEntity(loginRequest), userAgent);
			session = EntityFactory.createEntityFromJSONObject(obj, Session.class);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
		
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, session.getSessionToken());
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, session.getSessionToken());
		
		return session;		
	}

	public void logout(String userAgent) throws SynapseException {
		deleteUri(authEndpoint, "/session", userAgent);
		defaultGETDELETEHeaders.remove(SESSION_TOKEN_HEADER);
		defaultPOSTPUTHeaders.remove(SESSION_TOKEN_HEADER);
	}
	
	public boolean revalidateSession(String userAgent) throws SynapseException {
		Session session = new Session();
		String currentSessionToken = getCurrentSessionToken();
		if (currentSessionToken==null) throw new 
			SynapseClientException("You must log in before revalidating the session.");
		session.setSessionToken(currentSessionToken);
		try {
			putAuthEntity("/session", EntityFactory.createJSONObjectForEntity(session), userAgent);
		} catch (SynapseForbiddenException e) {
			throw new SynapseTermsOfUseException(e.getMessage());
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
		return true;
	}
	
	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 * @param sessionToken
	 */
	public void setSessionToken(String sessionToken) {
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
	}

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken() {
		return defaultPOSTPUTHeaders.get(SESSION_TOKEN_HEADER);
	}

	public void invalidateApiKey(String userAgent) throws SynapseException {
		deleteUri(authEndpoint, "/secretKey", userAgent);
		this.apiKey = null;
	}

	/**
	 * Put the contents of the passed file to the passed URL.
	 * 
	 * @param url
	 * @param file
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public String putFileToURL(URL url, File file, String contentType) throws SynapseException {
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (file == null)
				throw new IllegalArgumentException("File cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			// There must not be any headers added or Amazon will return a 403.
			// Therefore, we must clear the content type.
			@SuppressWarnings("deprecation")
			org.apache.http.entity.FileEntity fe = new org.apache.http.entity.FileEntity(file, contentType);
			httppost.setEntity(fe);
			HttpResponse response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseServerException(code, "Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url + " File: " + file.getName());
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}

	}
	
	/**
	 * Put the contents of the passed byte array to the passed URL, associating the given content type
	 * 
	 * @param url
	 * @param content the byte array to upload
	 * @throws SynapseException
	 */
	public String putBytesToURL(URL url, byte[] content, String contentType) throws SynapseException {
		try {
			if (url == null)
				throw new IllegalArgumentException("URL cannot be null");
			if (content == null)
				throw new IllegalArgumentException("content cannot be null");
			HttpPut httppost = new HttpPut(url.toString());
			ByteArrayEntity se = new ByteArrayEntity(content);
			httppost.setHeader("content-type", contentType);
			httppost.setEntity(se);
			HttpResponse response = clientProvider.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code > 299) {
				throw new SynapseServerException(code, "Response code: " + code + " " + response.getStatusLine().getReasonPhrase()
						+ " for " + url);
			}
			return EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}

	}
	
	private static final String ERROR_REASON_TAG = "reason";
	
	private static boolean isOKStatusCode(int statusCode) {
		return statusCode>=200 && statusCode<300;
	}
	
	private static void convertHttpResponseToException(int statusCode, String responseBody) throws SynapseException {
		if (isOKStatusCode(statusCode)) return;
		JSONObject results = null;
		try {
			results = new JSONObject(responseBody);
		} catch (JSONException e) {
			throw new SynapseClientException("responseBody: <<"+responseBody+">>", e);
		}
		convertHttpResponseToException(statusCode, results);
	}
	
	private static void convertHttpResponseToException(int statusCode, JSONObject responseBody) throws SynapseException {
		if (isOKStatusCode(statusCode)) return;
		String reasonStr = null;
		if (responseBody!=null) {
			try {
				reasonStr = responseBody.getString(ERROR_REASON_TAG);
			} catch (JSONException e) {
				throw new SynapseClientException(e);
			}
		}
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new SynapseUnauthorizedException(reasonStr);
		} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
			throw new SynapseForbiddenException(reasonStr);
		} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
			throw new SynapseNotFoundException(reasonStr);
		} else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
			throw new SynapseBadRequestException(reasonStr);
		} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
			throw new SynapseConflictingUpdateException(reasonStr);
		}  else {
			throw new SynapseServerException(statusCode, reasonStr);
		}
	}

	/**
	 * Download the file at the given URL.
	 * 
	 * @param uri
	 * @param userAgent
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SynapseException 
	 */
	public String downloadZippedFileString(String endpoint, String uri, String userAgent) throws ClientProtocolException, IOException,
			FileNotFoundException, SynapseException {
		HttpGet get = new HttpGet(endpoint+uri);
		setHeaders(get, defaultGETDELETEHeaders, userAgent);
		// Add the header that sets the content type and the boundary
		HttpResponse response = clientProvider.execute(get);
		HttpEntity entity = response.getEntity();
		int statusCode = response.getStatusLine().getStatusCode();
		if (!isOKStatusCode(statusCode)) {
			// we only want to read the input stream in case of an error
			String responseBody = (null != entity) ? EntityUtils.toString(entity) : null;
			convertHttpResponseToException(statusCode, responseBody);
		}
		return FileUtils.readCompressedStreamAsString(entity.getContent());
	}

	public File downloadFromSynapse(String url, String md5,
				File destinationFile, String userAgent) throws SynapseException {
		if (null == url) {
			throw new IllegalArgumentException("must provide path");
		}
		
		Map<String, String> modHeaders = new HashMap<String, String>(defaultGETDELETEHeaders);
		// remove session token if it is null
		if(modHeaders.containsKey(SESSION_TOKEN_HEADER) && modHeaders.get(SESSION_TOKEN_HEADER) == null) {
			modHeaders.remove(SESSION_TOKEN_HEADER);
		}
		modHeaders.put(USER_AGENT, userAgent);
		if (apiKey!=null) {
			addDigitalSignature(url, modHeaders);
		}
		try {
			clientProvider.downloadFile(url, destinationFile.getAbsolutePath(), modHeaders);
			// Check that the md5s match, if applicable
			if (null != md5) {
				String localMd5 = MD5ChecksumHelper
						.getMD5Checksum(destinationFile.getAbsolutePath());
				if (!localMd5.equals(md5)) {
					throw new SynapseClientException(
							"md5 of downloaded file does not match the one in Synapse"
									+ destinationFile);
				}
			}

			return destinationFile;
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		} catch (HttpClientHelperException e) {
			throw new SynapseServerException(e.getHttpStatus(), e);
		}
	}

	/******************** Mid Level Authorization Service APIs ********************/

	/**
	 * Create a new login, etc ...
	 * @param userAgent 
	 * 
	 * @return the newly created entity
	 */
	private JSONObject createAuthEntity(String uri, JSONObject entity, String userAgent) throws SynapseException {
		return postJson(authEndpoint, uri, entity.toString(), userAgent, null, null);
	}

	private JSONObject putAuthEntity(String uri, JSONObject entity, String userAgent)
			throws SynapseException {
		if (null == authEndpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		if (null == entity) {
			throw new IllegalArgumentException("must provide entity");
		}

		return putJson(authEndpoint, uri, entity.toString(), userAgent);
	}

	public String getDataDirect(String endpoint, String uri) throws SynapseException {
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		try {
			HttpResponse response = clientProvider.performRequest(endpoint + uri, "GET", null, null);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode!=HttpStatus.SC_OK) throw new SynapseServerException(statusCode);
			return EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}
	
	public static String getCharacterSetFromRequest(HttpRequestBase request) {
		Header contentTypeHeader = request.getFirstHeader("Content-Type");
		if (contentTypeHeader==null) return null;
		ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
		return contentType.getCharset().name();
	}

	public String postStringDirect(String endpoint, String uri, String data, String userAgent) throws SynapseException {
		URIBuilder builder = null;
		try {
			builder = new URIBuilder(endpoint + uri);
			builder.addParameter(AuthorizationConstants.DOMAIN_PARAM, domain.name());	
		} catch (URISyntaxException e) {
			throw new SynapseClientException(e);
		}
		try {
			HttpPost post = new HttpPost(builder.toString());
			setHeaders(post, defaultPOSTPUTHeaders, userAgent);
			StringEntity stringEntity = new StringEntity(data, getCharacterSetFromRequest(post));
			post.setEntity(stringEntity);
			HttpResponse response = clientProvider.execute(post);
			String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
			convertHttpResponseToException(response.getStatusLine().getStatusCode(), responseBody);
			return responseBody;
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	/******************** Low Level APIs ********************/

	/**
	 * Create any JSONEntity
	 * 
	 * @param endpoint
	 * @param uri
	 * @param userAgent
	 * @param parameters
	 * @return
	 * @throws SynapseException
	 */
	public JSONObject postJson(String endpoint, String uri, String jsonString, String userAgent, Map<String, String> parameters)
			throws SynapseException {
		return postJson(endpoint, uri, jsonString, userAgent, parameters, null);
	}

	/**
	 * Create any JSONEntity
	 * 
	 * @param endpoint
	 * @param uri
	 * @param userAgent
	 * @param parameters
	 * @return
	 * @throws SynapseException
	 */
	public JSONObject postJson(String endpoint, String uri, String jsonString, String userAgent, Map<String, String> parameters,
			ErrorHandler errorHandler) throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "POST", jsonString, defaultPOSTPUTHeaders, userAgent,
				parameters, errorHandler);
		return jsonObject;
	}

	/**
	 * Update any JSONEntity
	 * @param endpoint
	 * @param uri
	 * @param userAgent 
	 * @param entity
	 * @return
	 * @throws SynapseException
	 */
	public JSONObject putJson(String endpoint, String uri, String jsonToPut, String userAgent)
			throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "PUT", jsonToPut, defaultPOSTPUTHeaders, userAgent, null, null);
		return jsonObject;
	}
		
	/**
	 * Get a JSONEntity
	 * @param userAgent 
	 */
	protected JSONObject getJson(String endpoint, String uri, String userAgent) throws SynapseException {
		return getJson(endpoint, uri, userAgent, null);
	}

	/**
	 * Get a JSONEntity
	 * 
	 * @param userAgent
	 */
	protected JSONObject getJson(String endpoint, String uri, String userAgent, ErrorHandler errorHandler) throws SynapseException {
		if (null == endpoint) {
			throw new IllegalArgumentException("must provide endpoint");
		}
		if (null == uri) {
			throw new IllegalArgumentException("must provide uri");
		}
		JSONObject jsonObject = signAndDispatchSynapseRequest(endpoint, uri, "GET", null, defaultGETDELETEHeaders, userAgent, null,
				errorHandler);
		return jsonObject;
	}

	/**
	 * Call Create on any URI
	 * @param userAgent 
	 */
	public JSONObject postUri(String endpoint, String uri, String userAgent) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		return signAndDispatchSynapseRequest(endpoint, uri, "POST", null, defaultPOSTPUTHeaders, userAgent, null, null);
	}

	/**
	 * Call Delete on any URI
	 * @param userAgent 
	 */
	public void deleteUri(String endpoint, String uri, String userAgent) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null, defaultGETDELETEHeaders, userAgent, null, null);
	}
	
	public void deleteUri(String endpoint, String uri, String userAgent, Map<String, String> parameters) throws SynapseException {
		if (null == uri) throw new IllegalArgumentException("must provide uri");		
		signAndDispatchSynapseRequest(endpoint, uri, "DELETE", null, defaultGETDELETEHeaders, userAgent, parameters, null);
	}
	
	private void addDigitalSignature(String url, Map<String, String> modHeaders) throws SynapseClientException {
		String timeStamp = (new DateTime()).toString();
		String uriRawPath = null; 
		try {
			uriRawPath = (new URI(url)).getRawPath(); // chop off the query, if any
		} catch (URISyntaxException e) {
			throw new SynapseClientException(e);
		}
	    String signature = HMACUtils.generateHMACSHA1Signature(userName, uriRawPath, timeStamp, apiKey);
	    modHeaders.put(AuthorizationConstants.USER_ID_HEADER, userName);
	    modHeaders.put(AuthorizationConstants.SIGNATURE_TIMESTAMP, timeStamp);
	    modHeaders.put(AuthorizationConstants.SIGNATURE, signature);
	}
	
	protected JSONObject signAndDispatchSynapseRequest(String endpoint, String uri, String requestMethod, String requestContent,
			Map<String, String> requestHeaders, String userAgent, Map<String, String> parameters, ErrorHandler errorHandler)
			throws SynapseException {
		Map<String, String> modHeaders = new HashMap<String, String>(requestHeaders);
		modHeaders.put(USER_AGENT, userAgent);
		
		if (apiKey!=null) {
			addDigitalSignature(endpoint + uri, modHeaders);
		} 
		return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, modHeaders, parameters, errorHandler);
	}

	protected String createRequestUrl(String endpoint, String uri, Map<String,String> parameters) throws SynapseClientException {
		// At least one test calls the dispatch method directly, so verify again that the origin client has been set
		URL requestUrl = null;
		URIBuilder builder = new URIBuilder();
		try {
			URL parsedEndpoint = new URL(endpoint);
			String endpointPrefix = parsedEndpoint.getPath();
			String endpointLocation = endpoint.substring(0, endpoint.length() - endpointPrefix.length());

			requestUrl = (uri.startsWith(endpointPrefix)) ? new URL(endpointLocation + uri) : new URL(endpoint + uri);
			
			builder = new URIBuilder(requestUrl.toURI());
			if (parameters != null) {
				for (Map.Entry<String,String> entry : parameters.entrySet()) {
					builder.addParameter(entry.getKey(), entry.getValue());
				}
			} else {
				builder.addParameter(AuthorizationConstants.DOMAIN_PARAM, domain.name());
			}
			
		} catch(MalformedURLException mue) {
			throw new SynapseClientException("Invalid URI: <<"+builder.toString()+">>", mue);
		} catch(URISyntaxException use) {
			throw new SynapseClientException("Invalid URI: <<"+builder.toString()+">>", use);
		}
		return builder.toString();
	}
	
	/**
	 * Convert exceptions emanating from the service to
	 * Synapse[User|Service]Exception but let all other types of exceptions
	 * bubble up as usual
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 */
	protected JSONObject dispatchSynapseRequest(String endpoint, String uri, String requestMethod, String requestContent,
			Map<String, String> requestHeaders, Map<String, String> parameters, ErrorHandler errorHandler) throws SynapseException {
		if (requestProfile && !requestMethod.equals("DELETE")) {
			requestHeaders.put(REQUEST_PROFILE_DATA, "true");
		} else {
			if (requestHeaders.containsKey(REQUEST_PROFILE_DATA))
				requestHeaders.remove(REQUEST_PROFILE_DATA);
		}
		
		// remove session token if it is null
		if(requestHeaders.containsKey(SESSION_TOKEN_HEADER) && requestHeaders.get(SESSION_TOKEN_HEADER) == null) {
			requestHeaders.remove(SESSION_TOKEN_HEADER);
		}
		
		String requestUrl = null;
		String responseBody = null;
		JSONObject results = null;
		int statusCode = 0;
		try {
			requestUrl = createRequestUrl(endpoint, uri, parameters);
			HttpResponse response;
			if (retryRequestIfServiceUnavailable) {
				response = performRequestWithRetry(requestUrl, requestMethod, requestContent, requestHeaders);
			} else {
				response = performRequest(requestUrl, requestMethod, requestContent, requestHeaders);
			}
			statusCode = response.getStatusLine().getStatusCode();
			HttpEntity responseEntity = response.getEntity();
			responseBody = (null != responseEntity) ? EntityUtils
					.toString(responseEntity) : null;
			if (errorHandler != null) {
				errorHandler.handleError(statusCode, responseBody);
			}
		} catch (SynapseServerException sse) {
			throw sse;
		} catch (Exception e) {
			throw new SynapseClientException(e);
		}
		
		if (null != responseBody && responseBody.length()>0) {
			String resultsStringForLogging = null;
			try {
				results = new JSONObject(responseBody);
				resultsStringForLogging = results.toString(JSON_INDENT);
			} catch (JSONException jsone) {
				throw new SynapseClientException("responseBody: <<"+responseBody+">>", jsone);
			}
			if (log.isDebugEnabled()) {
				if(authEndpoint.equals(endpoint)) {
					log.debug(requestMethod + " " + requestUrl + " : (not logging auth request details)");
				} else {
					log.debug(requestMethod + " " + requestUrl + " : " + resultsStringForLogging);
				}
			}
		}
		
		convertHttpResponseToException(statusCode, results);

		return results;
	}

	public HttpResponse performRequest(String requestUrl, String requestMethod, String requestContent, Map<String, String> requestHeaders) throws ClientProtocolException, IOException {
		return clientProvider.performRequest(requestUrl, requestMethod, requestContent, requestHeaders);
	}
	
	public HttpResponse performRequestWithRetry(final String requestUrl, final String requestMethod, final String requestContent, final Map<String, String> requestHeaders) throws Exception {
		try {
			return TimeUtils.waitForExponentialMaxRetry(MAX_RETRY_SERVICE_UNAVAILABLE_COUNT, 1000, new Callable<HttpResponse>() {
				@Override
				public HttpResponse call() throws Exception {
					HttpResponse response = clientProvider.performRequest(requestUrl, requestMethod, requestContent, requestHeaders);
					//if 503, then we can retry
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
						HttpEntity responseEntity = response.getEntity();
						String responseBody = (null != responseEntity) ? EntityUtils
								.toString(responseEntity) : null;
						throw new RetryException(new SynapseServerException(statusCode, responseBody));
					}
						
					return response;
							}
			});
		} catch (RetryException e) {
			throw (SynapseServerException) e.getCause();
		}
	}
	
	public String getDirect(String endpoint, String uri, String userAgent) throws IOException, SynapseException {
		HttpGet get = new HttpGet(endpoint + uri);
		setHeaders(get, defaultGETDELETEHeaders, userAgent);
		HttpResponse response = clientProvider.execute(get);
		String responseBody = (null != response.getEntity()) ? EntityUtils.toString(response.getEntity()) : null;
		convertHttpResponseToException(response.getStatusLine().getStatusCode(), responseBody);
		return responseBody;
	}

	private void setHeaders(HttpRequestBase request, Map<String, String> headers, String userAgent) {
		for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
			request.setHeader(headerEntry.getKey(), headerEntry.getValue());
		}
		request.setHeader(USER_AGENT, userAgent);
	}
}

