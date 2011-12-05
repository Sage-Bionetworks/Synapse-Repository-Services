package org.sagebionetworks.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * This is the start of a Java client for SWF.  
 * The valuable part at this time is the implementation
 * of the digital signature.
 * 
 */
public class SWFManagement {
	
	private static final String SIGNATURE_METHOD = "HmacSHA1";
	private static final String SIGNATURE_VERSION = "2";
	private static final String VERSION = "2010-03-10";
	private static final String PROTOCOL = "http://"; // TODO pass in as a param
	private static final String HOST_NAME = "flow.us-east-1.amazonaws.com"; // TODO pass in as a param
	private static final String REQUEST_URI = "/";
	private static final String NAMESPACE = "325565585839"; // TODO pass in as a param
	
	private String awsAccessKeyID  = null;
	private String awsAccessKey  = null;
	
	public void setAwsAccessKeyID(String s) {awsAccessKeyID=s;}
	public void setAwsAccessKey(String s) {awsAccessKey=s;}
	
	public static String paramsToString(Map<String,String> params) {
		StringBuilder sb = new StringBuilder();
		boolean firsttime=true;
		for (String key : params.keySet()) {
			if (firsttime) {
				firsttime=false;
			} else {
				sb.append("&");
			}
			sb.append(urlEncode(key, true)+"="+urlEncode(params.get(key), true));
		}
		return sb.toString();	
	}
	
	/*
	 * From p. 91 of AWSSimpleWorkflow_DeveloperGuide.pdf
		StringToSign = HTTPVerb + "\n" +
		ValueOfHostHeaderInLowercase + "\n" +
		HTTPRequestURI + "\n" +
		CanonicalizedQueryString <from the preceding step>
	 */
	
	public static String computeBase64EncodedHMACSHA1Signature(
			String httpVerb,
			String hostHeader,
			String requestURI,
			Map<String,String> queryMap,
			String base64EncodedSecretKey
			) {
			StringBuilder stringToSign = new StringBuilder();
			stringToSign.append(httpVerb.toUpperCase()+ "\n");
			stringToSign.append(hostHeader.toLowerCase()+"\n");
			stringToSign.append(requestURI+"\n");
			
			
			/*
			 1. Create the canonicalized query string that you need later in this procedure:
				a. Sort the UTF-8 query string components by parameter name with natural byte ordering.
				The parameters can come from the GET URI or from the POST body (when Content-Type is
				application/x-www-form-urlencoded).
				b. URL encode the parameter name and values according to the following rules:
				Do not URL encode any of the unreserved characters that RFC 3986 defines.
				These unreserved characters are A-Z, a-z, 0-9, hyphen ( - ), underscore ( _ ), period ( . ), and
				tilde ( ~ ).
				API Version 2010-03-10
				90
				Amazon Simple Workflow Developer Guide
				Authenticating Query Requests
				Percent encode all other characters with %XY, where X and Y are hex characters 0-9 and
				uppercase A-F.
				Percent encode extended UTF-8 characters in the form %XY%ZA....
				Percent encode the space character as %20 (and not +, as common encoding schemes do).
			 */
			Object[] sorted = queryMap.keySet().toArray();
			Arrays.sort(sorted, new Comparator<Object>() {
				public int compare(Object s1, Object s2) {
					// throws exception if not both args are non-null Strings
					byte[] b1 = ((String)s1).getBytes();
					byte[] b2 = ((String)s2).getBytes();
					for (int i=0; i<b1.length || i<b2.length; i++) {
						if (i>=b1.length) return -1; // identical string but b2 has an additional suffix, so s1<s2
						if (i>=b2.length) return 1; // identical string but b1 has an additional suffix, so s1>s2
						if (b1[i]<b2[i]) return -1; // identical up to this byte, but now b1<b2
						if (b1[i]>b2[i]) return 1;  // identical up to this byte, but now b2>b1
						// b1[i]==b2[i], so proceed to the next byte
					}
					// same length, all bytes equal
					return 0;
				}
			});
			boolean firsttime = true;
			for (int i=0; i<sorted.length; i++) {
				String key = (String)sorted[i];
				if (firsttime) {firsttime=false;} else {stringToSign.append("&");}
				stringToSign.append(urlEncode(key, true)+"="+urlEncode(queryMap.get(key), true));
			}
			
			byte[] base64EncodedSig = HMACUtils.generateHMACSHA1SignatureFromRawKey(stringToSign.toString(), base64EncodedSecretKey.getBytes());
		return new String(base64EncodedSig);
	}
	
	public static String urlEncode(String s, boolean percentEncodeSpaces) {
		try {
			String encoded = URLEncoder.encode(s, "UTF-8");
			String ans = encoded;
			if (percentEncodeSpaces) ans=ans.replaceAll("\\+", "%20");
			return ans;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public HttpResponse performSWFRequest(String requestMethod, 
			String workflowName, 
			String action, 
			Map<String,String> requestParams) 
		throws ClientProtocolException, HttpClientHelperException, IOException {
		// take the passed in, request specific params and add generic params
		Map<String,String> params = new HashMap<String,String>(requestParams);
		params.put("Action", action);
		params.put("SignatureMethod", SIGNATURE_METHOD);
		params.put("SignatureVersion", SIGNATURE_VERSION);
		params.put("Version", VERSION);
		DateTime dt = new DateTime();
		String timeStamp = dt.toString();
		params.put("Timestamp", timeStamp);
		params.put("Namespace", NAMESPACE);
		params.put("workflowName", workflowName);
		
		params.put("AWSAccessKeyId", awsAccessKeyID);
		
		Map<String, String> requestHeaders = new HashMap<String,String>();
		requestHeaders.put("Host", HOST_NAME);
		requestHeaders.put("Accept", "application/json");
		
		String signature = computeBase64EncodedHMACSHA1Signature(
				requestMethod,
				requestHeaders.get("Host"),
				REQUEST_URI,
				params,
				awsAccessKey
				);
		params.put("Signature", signature);

		String requestContent = null;
		
		return HttpClientHelper.performRequest(PROTOCOL+HOST_NAME+REQUEST_URI+"?"+paramsToString(params),
				requestMethod, requestContent, requestHeaders);

	}
		
	public void cancelWorkflowInstance(String instance) {
	}
	
}
