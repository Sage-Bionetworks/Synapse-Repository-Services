package org.sagebionetworks.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseDeprecatedServiceException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseLockedException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.ErrorResponseCode;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.ValidateArgument;

public class ClientUtils {

	public static final String ERROR_REASON_TAG = "reason";

	/**
	 * Checks to see if the statusCode is in [200,300) range.
	 * 
	 * @param statusCode
	 * @return true if the statusCode is in range [200, 300)
	 */
	public static boolean is200sStatusCode(int statusCode) {
		return statusCode>=200 && statusCode<300;
	}

	/**
	 * Checks to see if the statusCode is in [200,300) range. If it is not,
	 * throws an exception.
	 *
	 * This method is used to check the SimpleHttpResponse that has expected
	 * content in non JSON format.
	 *
	 * @param response
	 * @throws SynapseException
	 */
	public static void checkStatusCodeAndThrowException(SimpleHttpResponse response) throws SynapseException {
		ValidateArgument.required(response, "response");
		final int statusCode = response.getStatusCode();
		if (is200sStatusCode(statusCode)) {
			return;
		}

		throwException(statusCode, response.getContent());

	}

	public static void throwException(int statusCode, String reasonStr) throws SynapseException{
		ErrorResponseCode errorResponseCode;
		String errorMessage;

		try {
			ErrorResponse errorResponse = EntityFactory.createEntityFromJSONString(reasonStr, ErrorResponse.class);
			errorMessage = errorResponse.getReason();
			errorResponseCode = errorResponse.getErrorCode();
		} catch (JSONObjectAdapterException e) {
			//this is fine, just use the original reasonStr
			errorMessage = reasonStr;
			errorResponseCode = null;
		}

		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new SynapseUnauthorizedException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
			throw new SynapseForbiddenException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
			throw new SynapseNotFoundException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
			throw new SynapseBadRequestException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_LOCKED) {
			throw new SynapseLockedException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
			throw new SynapseConflictingUpdateException(errorMessage, errorResponseCode);
		} else if (statusCode == HttpStatus.SC_GONE) {
			throw new SynapseDeprecatedServiceException(errorMessage, errorResponseCode);
		} else if (statusCode == SynapseTooManyRequestsException.TOO_MANY_REQUESTS_STATUS_CODE){
			throw new SynapseTooManyRequestsException(errorMessage, errorResponseCode);
		}else {
			throw new UnknownSynapseServerException(statusCode, errorMessage, null, errorResponseCode);
		}
	}

	/**
	 * Try to convert response body to a JSON object. Then check for status code.
	 * If statusCode is in [200,300) range, returns the JSON object. Otherwise,
	 * throw an exception using the converted JSON object.
	 * 
	 * This method is used to check the SimpleHttpResponse that has expected
	 * content in JSON format.
	 * 
	 * @param response
	 * @return
	 * @throws SynapseException
	 */
	public static JSONObject convertResponseBodyToJSONAndThrowException(SimpleHttpResponse response) throws SynapseException {
		ValidateArgument.required(response, "response");
		JSONObject json;

		if (!is200sStatusCode(response.getStatusCode())) {
			throwException(response.getStatusCode(), response.getContent());
		}

		try {
			return convertStringToJSONObject(response.getContent());
		} catch (JSONException e) {
			/*
			 * 200 status code return for an API that expected a JSON response,
			 * but the response couldn't be converted to JSON
			 */
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Converts a String to a JSON object.
	 * 
	 * @param toConvert
	 * @return
	 * @throws SynapseClientException
	 */
	public static JSONObject convertStringToJSONObject(String toConvert) throws JSONException {
		JSONObject json = null;
		if (null != toConvert && toConvert.length()>0) {
			json = new JSONObject(toConvert);
		}
		return json;
	}

	/**
	 * 
	 * @param response
	 * @return
	 */
	public static Charset getCharacterSetFromResponse(SimpleHttpResponse response) {
		ValidateArgument.required(response, "response");
		Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
		return contentType.getCharset();
	}

	/**
	 * @param endpoint
	 * @param uri
	 * @param parameters
	 * @return
	 * @throws SynapseClientException
	 */
	public static String createRequestUrl(String endpoint, String uri, Map<String,String> parameters) throws SynapseClientException {
		ValidateArgument.required(endpoint, "endpoint");
		ValidateArgument.required(uri, "uri");

		URL requestUrl = null;
		URIBuilder builder = new URIBuilder();
		try {
			URL parsedEndpoint = new URL(endpoint);
			String endpointPrefix = parsedEndpoint.getPath();
			String endpointLocation = endpoint.substring(0, endpoint.length() - endpointPrefix.length());
			if (uri.startsWith(endpointPrefix)) {
				requestUrl = new URL(endpointLocation + uri);
			} else {
				requestUrl = new URL(endpoint + uri);
			}
			builder = new URIBuilder(requestUrl.toURI());
			if (parameters != null) {
				for (Map.Entry<String,String> entry : parameters.entrySet()) {
					builder.addParameter(entry.getKey(), entry.getValue());
				}
			}
		} catch(MalformedURLException mue) {
			throw new SynapseClientException("Invalid URI.", mue);
		} catch(URISyntaxException use) {
			throw new SynapseClientException("Invalid URI.", use);
		}
		return builder.toString();
	}

	/**
	 * 
	 * 
	 * @param simpleHttpClient
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static SimpleHttpResponse performRequest(SimpleHttpClient simpleHttpClient,
			String requestUrl, Method requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException, IOException {
		ValidateArgument.required(simpleHttpClient, "simpleHttpClient");
		ValidateArgument.required(requestUrl, "requestUrl");
		ValidateArgument.required(requestMethod, "requestMethod");

		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(requestUrl);
		request.setHeaders(requestHeaders);
		switch (requestMethod) {
			case GET:
				return simpleHttpClient.get(request);
			case POST:
				return simpleHttpClient.post(request, requestContent);
			case PUT:
				return simpleHttpClient.put(request, requestContent);
			case DELETE:
				return simpleHttpClient.delete(request);
			default: 
				throw new IllegalArgumentException("Unsupported method: "+requestMethod);
		}
	}
	
	
	public static String createBasicAuthorizationHeader(String username, String password) {
		return AuthorizationConstants.BASIC_PREFIX+ (new String(Base64.
				encodeBase64((username + ":" + password).getBytes(Charset.forName("UTF-8")))));
	}
}