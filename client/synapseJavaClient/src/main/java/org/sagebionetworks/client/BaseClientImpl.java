package org.sagebionetworks.client;

import static org.sagebionetworks.client.Method.DELETE;
import static org.sagebionetworks.client.Method.GET;
import static org.sagebionetworks.client.Method.POST;
import static org.sagebionetworks.client.Method.PUT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.protocol.HTTP;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseServiceUnavailable;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;
import org.sagebionetworks.downloadtools.FileUtils;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.utils.MD5ChecksumHelper;

/**
 * Low-level Java Client API for REST APIs
 */
public class BaseClientImpl implements BaseClient {
	private static final String DEFAULT_AUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	private static final String DEFAULT_REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";
	private static final String DEFAULT_FILE_ENDPOINT = "https://repo-prod.prod.sagebase.org/file/v1";

	private static final String SYNAPSE_ENCODING_CHARSET = "UTF-8";
	protected static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_JSON_CHARSET_UTF8 = APPLICATION_JSON+"; charset="+SYNAPSE_ENCODING_CHARSET;

	protected static final String APPLICATION_JWT = "application/jwt";
	
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE = "Content-Type";
	protected static final String ACCEPT = "Accept";
	private static final String SESSION_TOKEN_HEADER = "sessionToken";
	private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
	private static final String USER_AGENT = "User-Agent";
	private static final String SESSION_ID_COOKIE = "sessionID";

	public static final int MAX_RETRY_SERVICE_UNAVAILABLE_COUNT = 5;

	private SimpleHttpClient simpleHttpClient;

	private String userAgent;
	private String username;
	private String apiKey;
	private String repoEndpoint;
	private String authEndpoint;
	private String fileEndpoint;

	private String authorizationHeader;
	
	private boolean acceptsTermsOfUse;
	
	//cached value that is derived from repoEndpoint
	String repoEndpointBaseDomain;

	private Map<String, String> defaultGETDELETEHeaders;
	private Map<String, String> defaultPOSTPUTHeaders;

	public BaseClientImpl(String userAgent) {
		this(userAgent, null);
	}

	public BaseClientImpl(String userAgent, SimpleHttpClientConfig config) {
		this.userAgent = userAgent;
		this.simpleHttpClient = new SimpleHttpClientImpl(config);

		this.authEndpoint = DEFAULT_AUTH_ENDPOINT;
		setRepositoryEndpoint(DEFAULT_REPO_ENDPOINT);
		this.fileEndpoint = DEFAULT_FILE_ENDPOINT;
		this.defaultGETDELETEHeaders = new HashMap<String, String>();
		this.defaultGETDELETEHeaders.put(ACCEPT, APPLICATION_JSON_CHARSET_UTF8);
		this.defaultPOSTPUTHeaders = new HashMap<String, String>();
		this.defaultPOSTPUTHeaders.putAll(defaultGETDELETEHeaders);
		this.defaultPOSTPUTHeaders.put(CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF8);
	}

	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 'User-Agent':'Synapse-Java-Client/<version_number>' Addition User-Agent information can be appended to this
	 * string by calling this method.
	 * 
	 * @param toAppend
	 */
	@Override
	public void appendUserAgent(String toAppend) {
		ValidateArgument.required(toAppend, "toAppend");
		// Only append if it is not already there
		if (this.userAgent.indexOf(toAppend) < 0) {
			this.userAgent = this.userAgent + " " + toAppend;
		}
	}

	@Deprecated
	/**
	 * @category Authentication
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	public LoginResponse login(LoginRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUsername(), "LoginRequest.username");
		ValidateArgument.required(request.getPassword(), "LoginRequest.password");
		LoginResponse response = postJSONEntity(authEndpoint, "/login", request, LoginResponse.class);
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, response.getSessionToken());
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, response.getSessionToken());
		acceptsTermsOfUse = response.getAcceptsTermsOfUse();
		return response;
	}

	/**
	 * @category Authentication
	 * @param request
	 * @return
	 * @throws SynapseException
	 */
	public LoginResponse loginForAccessToken(LoginRequest request) throws SynapseException {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getUsername(), "LoginRequest.username");
		ValidateArgument.required(request.getPassword(), "LoginRequest.password");
		LoginResponse response = postJSONEntity(authEndpoint, "/login2", request, LoginResponse.class);
		setBearerAuthorizationToken(response.getAccessToken());
		acceptsTermsOfUse = response.getAcceptsTermsOfUse();
		return response;
	}
	
	@Override
	public void setAcceptsTermsOfUse(boolean b) {
		acceptsTermsOfUse = b;
	}
	
	protected boolean acceptsTermsOfUse() {
		return acceptsTermsOfUse;
	}

	@Deprecated
	/**
	 * @category Authentication
	 * @throws SynapseException
	 */
	public void logout() throws SynapseException {
		if (defaultGETDELETEHeaders.containsKey(SESSION_TOKEN_HEADER)) {
			deleteUri(authEndpoint, "/session");
		}
		defaultGETDELETEHeaders.remove(SESSION_TOKEN_HEADER);
		defaultPOSTPUTHeaders.remove(SESSION_TOKEN_HEADER);
	}

	public void logoutForAccessToken() throws SynapseException {
		removeAuthorizationHeader();
	}

	//================================================================================
	// Setters and Getters
	//================================================================================
	
	@Deprecated
	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 */
	@Override
	public void setSessionToken(String sessionToken) {
		defaultGETDELETEHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
		defaultPOSTPUTHeaders.put(SESSION_TOKEN_HEADER, sessionToken);
	}


	@Deprecated
	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	@Override
	public String getCurrentSessionToken() {
		return this.defaultPOSTPUTHeaders.get(SESSION_TOKEN_HEADER);
	}

	/**
	 * Set a uname and password as a Basic Authorization header.
	 * This should be used exclusively of the Synapse session token
	 * or any other authorization scheme.
	 * @param uname
	 * @param password
	 */
	@Override
	public void setBasicAuthorizationCredentials(String uname, String password) {
		String basicAuthCredentials = ClientUtils.createBasicAuthorizationHeader(uname, password);
		this.authorizationHeader=basicAuthCredentials;
		defaultGETDELETEHeaders.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, basicAuthCredentials);
		defaultPOSTPUTHeaders.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, basicAuthCredentials);
	}
	
	/**
	 * Set a bearer authorization token.
	 * This should be used exclusively of the Synapse session token
	 * or any other authorization scheme.
	 * @param bearerToken
	 */
	@Override
	public void setBearerAuthorizationToken(String bearerToken) {
		String bearerTokenHeader = AuthorizationConstants.BEARER_TOKEN_HEADER+bearerToken;
		this.authorizationHeader=bearerTokenHeader;
		defaultGETDELETEHeaders.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, bearerTokenHeader);
		defaultPOSTPUTHeaders.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, bearerTokenHeader);
	}

	protected String getAuthorizationHeader() {
		return authorizationHeader;
	}
	
	@Override
	public String getAccessToken() {
		if (authorizationHeader==null || !authorizationHeader.startsWith(AuthorizationConstants.BEARER_TOKEN_HEADER))
			throw new IllegalStateException("Missing bearer token header.");
		return authorizationHeader.substring(AuthorizationConstants.BEARER_TOKEN_HEADER.length());
	}
	
	/**
	 * Remove the Authorization Header
	 */
	@Override
	public void removeAuthorizationHeader() {
		this.authorizationHeader=null;
		defaultGETDELETEHeaders.remove(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		defaultPOSTPUTHeaders.remove(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
	}
	
	@Override
	public String getRepoEndpoint() {
		return this.repoEndpoint;
	}

	@Override
	public void setRepositoryEndpoint(String repoEndpoint) {

		try {
			URL url = new URL(repoEndpoint);
			this.repoEndpointBaseDomain = url.getHost();

			this.repoEndpoint = repoEndpoint;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("repoEndpoint is malformed", e);
		}
	}

	@Override
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}

	@Override
	public String getAuthEndpoint() {
		return authEndpoint;
	}

	@Override
	public void setFileEndpoint(String fileEndpoint) {
		this.fileEndpoint = fileEndpoint;
	}

	@Override
	public String getFileEndpoint() {
		return this.fileEndpoint;
	}

	@Override
	public String getUserName() {
		return this.username;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getApiKey() {
		return this.apiKey;
	}

	@Override
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @category Authentication
	 * @throws SynapseException
	 */
	@Override
	public void invalidateApiKey() throws SynapseException {
		deleteUri(authEndpoint, "/secretKey");
		this.apiKey = null;
	}

	/**
	 * 
	 * @param ipAddress
	 */
	@Override
	public void setUserIpAddress(String ipAddress){
		ValidateArgument.required(ipAddress, "ipAddress");
		//verify that it is a proper IP address
		if( !( InetAddressUtils.isIPv4Address(ipAddress) || InetAddressUtils.isIPv6Address(ipAddress) ) ){
			throw new IllegalArgumentException("The provided ipAddress:" + ipAddress + " is not a standard IP address.");
		}
		defaultGETDELETEHeaders.put(X_FORWARDED_FOR_HEADER, ipAddress);
		defaultPOSTPUTHeaders.put(X_FORWARDED_FOR_HEADER, ipAddress);
	}

	@Override
	public void setSessionId(String sessionId){
		simpleHttpClient.addCookie(this.repoEndpointBaseDomain, SESSION_ID_COOKIE, sessionId);
	}


	@Override
	public String getSessionId(){
		return simpleHttpClient.getFirstCookieValue(this.repoEndpointBaseDomain, SESSION_ID_COOKIE);
	}

	protected String getUserAgent() {
		return this.userAgent;
	}

	/**
	 * @category Authentication
	 * @throws SynapseException
	 */
	@Deprecated
	protected void revalidateSession() throws SynapseException {
		Session session = new Session();
		String currentSessionToken = getCurrentSessionToken();
		if (currentSessionToken==null) throw new 
			SynapseClientException("You must log in before revalidating the session.");
		session.setSessionToken(currentSessionToken);
		try {
			voidPut(authEndpoint, "/session", session);
		} catch (SynapseForbiddenException e) {
			throw new SynapseTermsOfUseException(e.getMessage());
		}
	}


	//================================================================================
	// Upload & Download related helping functions
	//================================================================================

	/**
	 * Put the contents of the passed file to the passed URL.
	 * 
	 * @category Upload & Download
	 * @param url
	 * @param file
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected String putFileToURL(URL url, File file, String contentType) throws SynapseException {
		ValidateArgument.required(url, "url");
		ValidateArgument.required(file, "file");
		ValidateArgument.required(contentType, "contentType");
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url.toString());
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(HTTP.CONTENT_TYPE, contentType);
		request.setHeaders(headers);
		try {
			SimpleHttpResponse response = simpleHttpClient.putFile(request, file);
			if (!ClientUtils.is200sStatusCode(response.getStatusCode())) {
				throw new UnknownSynapseServerException(response.getStatusCode(), 
						response.getStatusReason()
						+ " for " + url + " File: " + file.getName());
			}
			return response.getContent();
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Download the file at the given URL.
	 * 
	 * @category Upload & Download
	 * @param endpoint
	 * @param uri
	 * @param gunzip unzip if zipped
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws SynapseException 
	 */
	protected String downloadFileToString(String endpoint, String uri, boolean gunzip)
			throws ClientProtocolException, IOException, FileNotFoundException, SynapseException {
		ValidateArgument.required(endpoint, "endpoint");
		ValidateArgument.required(uri, "uri");
		File file = File.createTempFile("file", null);
		Charset charset = downloadFromSynapse(endpoint+uri, null, file);
		try (InputStream inputStream = new FileInputStream(file)){
			return FileUtils.readStreamAsString(inputStream, charset, gunzip);
		} finally {
			if (file != null) {
				file.delete();
			}
		}
	}

	/**
	 * @category Upload & Download
	 * @param url
	 * @param md5
	 * @param destinationFile
	 * @return the character set used to encode the downloaded file
	 * @throws SynapseException
	 */
	protected Charset downloadFromSynapse(String url, String md5, File destinationFile)
			throws SynapseException {
		ValidateArgument.required(url, "url");
		ValidateArgument.required(destinationFile, "destinationFile");

		try {
			// step 1: get redirect URL
			String redirUrl = getStringDirect(url, "");
			// step 2: download file
			SimpleHttpRequest request = new SimpleHttpRequest();
			request.setUri(redirUrl);
			SimpleHttpResponse response = simpleHttpClient.getFile(request, destinationFile);
			ClientUtils.convertResponseBodyToJSONAndThrowException(response);
			// Check that the md5s match, if applicable
			if (null != md5) {
				String localMd5 = MD5ChecksumHelper.getMD5Checksum(destinationFile.getAbsolutePath());
				if (!localMd5.equals(md5)) {
					throw new SynapseClientException(
							"md5 of downloaded file does not match the one in Synapse "
									+ destinationFile);
				}
			}
			Charset charset = ClientUtils.getCharacterSetFromResponse(response);
			
			return charset;
		} catch (ClientProtocolException e) {
			throw new SynapseClientException(e);
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	//================================================================================
	// Helpers that perform request and return JSONObject
	//================================================================================
	
	protected void validateContentType(SimpleHttpResponse response, String expectedContentType) throws SynapseClientException {
		// If Synapse returns null there is no content-type header, so check content length
		// and if equals zero then don't check content type.
		Header contentLengthHeader = response.getFirstHeader(CONTENT_LENGTH);
		if (contentLengthHeader!=null) {
			Long contentLength = Long.parseLong(contentLengthHeader.getValue());
			if (contentLength==0) return;
		}
		Header contentTypeHeader = response.getFirstHeader(CONTENT_TYPE);
		if (contentTypeHeader==null) throw new SynapseClientException("Missing "+CONTENT_TYPE+" header.");
		String actualContentType = contentTypeHeader.getValue();
		if (!actualContentType.toLowerCase().startsWith(expectedContentType.toLowerCase())) {
			throw new SynapseClientException("Expected "+expectedContentType+" but received "+actualContentType);
		}
	}

	/**
	 * Get a JSONObject
	 * 
	 * @category JSONObject Requests
	 */
	protected JSONObject getJson(String endpoint, String uri) throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(
				endpoint, uri, GET, null, defaultGETDELETEHeaders, null);
		validateContentType(response, APPLICATION_JSON);
		return ClientUtils.convertResponseBodyToJSONAndThrowException(response);
	}

	/**
	 * Create any JSONObject
	 * 
	 * @category JSONObject Requests
	 * @param endpoint
	 * @param uri
	 * @param userAgent
	 * @param parameters
	 * @return
	 * @throws SynapseException
	 */
	protected JSONObject postJson(String endpoint, String uri, String jsonString,
			Map<String, String> parameters) throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(endpoint, uri,
				POST, jsonString, defaultPOSTPUTHeaders, parameters);
		return ClientUtils.convertResponseBodyToJSONAndThrowException(response);
	}

	/**
	 * Update any JSONObject
	 * 
	 * @category JSONObject Requests
	 * @param endpoint
	 * @param uri
	 * @param jsonToPut
	 * @return
	 * @throws SynapseException
	 */
	protected JSONObject putJson(String endpoint, String uri, String jsonToPut)
			throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(endpoint, uri,
				PUT, jsonToPut, defaultPOSTPUTHeaders, null);
		return ClientUtils.convertResponseBodyToJSONAndThrowException(response);
	}

	/**
	 * Call Update on any URI
	 * 
	 * @category JSONObject Requests
	 */
	protected void putUri(String endpoint, String uri) throws SynapseException {
		putJson(endpoint, uri, null);
	}

	//================================================================================
	// Helpers that perform request and return response content as String
	//================================================================================

	/**
	 * This is used to get a response which is a simple string (not encoded as JSON)
	 * 
	 * @category String Requests
	 * @param endpoint
	 * @param uri
	 * @return
	 * @throws IOException
	 * @throws SynapseException
	 */
	protected String getStringDirect(String endpoint, String uri)
			throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(
				endpoint, uri, GET, null, defaultGETDELETEHeaders, null);
		ClientUtils.checkStatusCodeAndThrowException(response);
		return response.getContent();
	}

	/**
	 * @category String Requests
	 * @param endpoint
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws SynapseException
	 */
	protected URL getUrl(String endpoint, String uri) throws SynapseException {
		try {
			return new URL(getStringDirect(endpoint, uri));
		} catch (IOException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * @category String Requests
	 * @param endpoint
	 * @param uri
	 * @param data
	 * @return
	 * @throws SynapseException
	 */
	protected String postStringDirect(String endpoint, String uri, String data)
			throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(
				endpoint, uri, POST, data, defaultPOSTPUTHeaders, null);
		ClientUtils.checkStatusCodeAndThrowException(response);
		return response.getContent();
	}

	/**
	 * Call Delete on any URI
	 * 
	 * @category String Requests
	 */
	protected void deleteUri(String endpoint, String uri) throws SynapseException {
		deleteUri(endpoint, uri, null);
	}

	/**
	 * @category String Requests
	 * @param endpoint
	 * @param uri
	 * @param parameters
	 * @throws SynapseException
	 */
	protected void deleteUri(String endpoint, String uri, Map<String, String> parameters)
			throws SynapseException {
		SimpleHttpResponse response = signAndDispatchSynapseRequest(
				endpoint, uri, DELETE, null, defaultGETDELETEHeaders, parameters);
		ClientUtils.checkStatusCodeAndThrowException(response);
	}

	/**
	 * Asymmetrical put where the request and response may not be the same JSONEntity.
	 * 
	 * @category JSONEntity Requests
	 * @param endpoint
	 * @param url
	 * @param requestBody
	 * @param returnClass
	 * @throws SynapseException
	 */
	protected <T extends JSONEntity> T putJSONEntity(String endpoint, String url,
			JSONEntity requestBody, Class<? extends T> returnClass) throws SynapseException {
		ValidateArgument.required(returnClass, "returnClass");
		try {
			String jsonString = null;
			if(requestBody != null){
				jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			}
			JSONObject responseBody = putJson(endpoint, url, jsonString);
			return EntityFactory.createEntityFromJSONObject(responseBody, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Asymmetrical put where a response JSONEntity is not expected.
	 * 
	 * @category JSONEntity Requests
	 * @param endpoint
	 * @param url
	 * @param requestBody
	 * @throws SynapseException
	 */
	protected void voidPut(String endpoint, String url, JSONEntity requestBody) throws SynapseException {
		String jsonString = null;
		try {
			if(requestBody != null){
				jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			}
			SimpleHttpResponse response = signAndDispatchSynapseRequest(endpoint,
					url, PUT, jsonString, defaultPOSTPUTHeaders, null);
			ClientUtils.checkStatusCodeAndThrowException(response);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * @category JSONEntity Requests
	 * @param endpoint
	 * @param url
	 * @param requestBody
	 * @param returnClass
	 * @return
	 * @throws SynapseException
	 */
	protected <T extends JSONEntity> T postJSONEntity(String endpoint, String url,
			JSONEntity requestBody, Class<? extends T> returnClass) throws SynapseException {
		ValidateArgument.required(returnClass, "returnClass");
		try {
			String jsonString = null;
			if (requestBody != null) {
				jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			}
			JSONObject responseBody = postJson(endpoint, url, jsonString, null);
			return EntityFactory.createEntityFromJSONObject(responseBody, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Asymmetrical post where a response JSONEntity is not expected.
	 * 
	 * @category JSONEntity Requests
	 * @param endpoint
	 * @param url
	 * @param requestBody
	 * @param returnClass
	 * @param params
	 * @param errorHandler
	 * @throws SynapseException
	 */
	protected void voidPost(String endpoint, String url, JSONEntity requestBody,
			Map<String, String> params) throws SynapseException {
		try {
			String jsonString = null;
			if (requestBody != null) {
				jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			}
			SimpleHttpResponse response = signAndDispatchSynapseRequest(endpoint,
					url, POST, jsonString, defaultPOSTPUTHeaders, params);
			ClientUtils.checkStatusCodeAndThrowException(response);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get a JSONEntity
	 * @category JSONEntity Requests
	 */
	protected <T extends JSONEntity> T getJSONEntity(String endpoint, String uri,
			Class<? extends T> returnClass) throws SynapseException {
		ValidateArgument.required(returnClass, "returnClass");
		try {
			JSONObject jsonObject = getJson(endpoint, uri);
			if (jsonObject == null) {
				return null;
			}
			return (T) EntityFactory.createEntityFromJSONObject(jsonObject, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get PaginatedResults of JSONEntity
	 * @category JSONEntity Requests
	 */
	protected <T extends JSONEntity> PaginatedResults<T> getPaginatedResults(
			String endpoint, String uri, Class<? extends T> returnClass)
					throws SynapseException {
		ValidateArgument.required(returnClass, "returnClass");

		JSONObject jsonObject = getJson(endpoint, uri);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
		try {
			return PaginatedResults.createFromJSONObjectAdapter(adapter, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get PaginatedResults of JSONEntity with a request body
	 * @category JSONEntity Requests
	 */
	protected <T extends JSONEntity> PaginatedResults<T> getPaginatedResults(
			String endpoint, String uri, JSONEntity requestBody, Class<? extends T> returnClass)
					throws SynapseException {
		ValidateArgument.required(requestBody, "requestBody");
		ValidateArgument.required(returnClass, "returnClass");
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			JSONObject jsonObject = postJson(endpoint, uri, jsonString, null);
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
			return PaginatedResults.createFromJSONObjectAdapter(adapter, returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get List of JSONEntity
	 * @category JSONEntity Requests
	 */
	protected <T extends JSONEntity> List<T> getListOfJSONEntity(String endpoint,
			String uri, Class<? extends T> returnClass) throws SynapseException {
		ValidateArgument.required(returnClass, "returnClass");
		try {
			JSONObject jsonObject = getJson(endpoint, uri);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(jsonObject), returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Get List of JSONEntity with a request body
	 * @category JSONEntity Requests
	 */
	protected <T extends JSONEntity> List<T> getListOfJSONEntity(String endpoint,
			String uri, JSONEntity requestBody, Class<? extends T> returnClass)
					throws SynapseException {
		ValidateArgument.required(requestBody, "requestBody");
		ValidateArgument.required(returnClass, "returnClass");
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(requestBody);
			JSONObject jsonObject = postJson(endpoint, uri, jsonString, null);
			return ListWrapper.unwrap(new JSONObjectAdapterImpl(jsonObject), returnClass);
		} catch (JSONObjectAdapterException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * @category JSONEntity Requests
	 * @param uri
	 * @return
	 * @throws SynapseException
	 */
	protected boolean getBooleanResult(String endpoint, String uri) throws SynapseException {
		try {
			JSONObject jsonObj = getJson(endpoint, uri);
			Boolean booleanResult = null;
			try {
				booleanResult = jsonObj.getBoolean("result");
			} catch (NullPointerException e) {
				throw new SynapseClientException(jsonObj.toString(), e);
			}
			return booleanResult;
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * @param url
	 * @param headers
	 * @throws SynapseClientException
	 */
	protected void addDigitalSignature(String url, Map<String, String> headers) throws SynapseClientException {
		ValidateArgument.required(url, "url");
		ValidateArgument.required(headers, "headers");
		String timeStamp = (new DateTime()).toString();
		String uriRawPath = null; 
		try {
			uriRawPath = (new URI(url)).getRawPath(); // chop off the query, if any
		} catch (URISyntaxException e) {
			throw new SynapseClientException(e);
		}
		String signature = HMACUtils.generateHMACSHA1Signature(username, uriRawPath, timeStamp, apiKey);
		headers.put(AuthorizationConstants.USER_ID_HEADER, username);
		headers.put(AuthorizationConstants.SIGNATURE_TIMESTAMP, timeStamp);
		headers.put(AuthorizationConstants.SIGNATURE, signature);
	}

	/**
	 * @param endpoint
	 * @param uri
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @param userAgent
	 * @param parameters
	 * @param errorHandler
	 * @return
	 * @throws SynapseException
	 */
	protected SimpleHttpResponse signAndDispatchSynapseRequest(String endpoint,
			String uri, Method requestMethod, String requestContent,
			Map<String, String> requestHeaders, Map<String, String> parameters)
					throws SynapseException {
		Map<String, String> modHeaders = new HashMap<String, String>(requestHeaders);
		modHeaders.put(USER_AGENT, userAgent);
		if (apiKey!=null) {
			addDigitalSignature(endpoint + uri, modHeaders);
		}
		return dispatchSynapseRequest(endpoint, uri, requestMethod, requestContent, modHeaders, parameters);
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
	protected SimpleHttpResponse dispatchSynapseRequest(String endpoint, String uri,
			Method requestMethod, String requestContent,
			Map<String, String> requestHeaders, Map<String, String> parameters)
					throws SynapseException {

		// remove session token if it is null
		if(requestHeaders.containsKey(SESSION_TOKEN_HEADER) && requestHeaders.get(SESSION_TOKEN_HEADER) == null) {
			requestHeaders.remove(SESSION_TOKEN_HEADER);
		}

		String requestUrl = ClientUtils.createRequestUrl(endpoint, uri, parameters);
		return performRequestWithRetry(requestUrl, requestMethod, requestContent, requestHeaders);
	}

	/**
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 */
	protected SimpleHttpResponse performRequestWithRetry(final String requestUrl,
			final Method requestMethod, final String requestContent,
			final Map<String, String> requestHeaders) throws SynapseException{
		try {
			return TimeUtils.waitForExponentialMaxRetry(MAX_RETRY_SERVICE_UNAVAILABLE_COUNT, 1000, new Callable<SimpleHttpResponse>() {
				@Override
				public SimpleHttpResponse call() throws Exception {
					try {
						SimpleHttpResponse response = ClientUtils.performRequest(simpleHttpClient, requestUrl, requestMethod, requestContent, requestHeaders);
						int statusCode = response.getStatusCode();
						if (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
							throw new RetryException(new SynapseServiceUnavailable(response.getContent()));
						}
						return response;
					} catch (SocketTimeoutException ste) {
						throw new RetryException(new SynapseServiceUnavailable(ste));
					}
				}
			});
		} catch (RetryException e) {
			throw (SynapseServiceUnavailable) e.getCause();
		} catch (Exception e) {
			throw new SynapseClientException("Failed to perform request.", e);
		}
	}

	// for test
	protected void setSimpleHttpClient(SimpleHttpClient client) {
		this.simpleHttpClient = client;
	}
}