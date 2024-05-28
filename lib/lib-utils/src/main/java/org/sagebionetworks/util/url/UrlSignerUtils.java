package org.sagebionetworks.util.url;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class UrlSignerUtils {

	public static final String MSG_SIGNATURE_MISSING = "Signature is missing";
	public static final String MSG_URL_EXPIRED = "Pre-signed URL has expired";
	public static final String MSG_SIGNATURE_DOES_NOT_MATCH = "The pre-signed URL signature does not match";
	/**
	 * The parameter name used for an HMAC signature.
	 */
	public static final String HMAC_SIGNATURE = "hmacSignature";
	/**
	 * The parameter name used for the expiration.
	 */
	public static final String EXPIRATION = "expiration";
	public static final String HMAC_SHA1 = "HmacSHA1";
	public static final String UTF_8 = "UTF-8";
	public static final String SHA_256 = "SHA-256";
	

	/**
	 * @see
	 * {@link #generateSignature(HttpMethod, UrlData, String, String)}
	 * @param method
	 * @param url
	 * @param signatureParameterName
	 * @param credentials
	 * @return
	 * @throws MalformedURLException
	 */
	public static String generateSignature(HttpMethod method, String url, String credentials) throws MalformedURLException {
		UrlData urlData = new UrlData(url);
		return generateSignature(method, urlData, credentials);
	}


	/**
	 * Generate the keyed-hash message authentication code (HMAC) of the given parameters.
	 * This method will first combine the parameters into a canonical form. The SHA-256 hash of the
	 * canonical form will then be used to create the signature using Hmac-SHA1 and the given credential string.
	 * 
	 * @param method The HTTP method used.
	 * @param url The URL to sign.
	 * @param signatureParameterName The name of the signature parameter if 
	 * @param credentials
	 * @return
	 * @throws MalformedURLException
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateSignature(HttpMethod method, UrlData urlData, String credentials) throws MalformedURLException {
		// First parse the URL
		String canonicalForm = makeS3CanonicalString(method, urlData);
		// Sign the hash.
		byte[] signature = sign(canonicalForm, credentials);
		// hex encode the signature.
		return Hex.encodeHexString(signature);
	}
	
	/**
	 * Generate a pre-signed URL given a method, expiration, and the URL to sign.
	 * 
	 * @param method The HTTP method the URL is expected to used with.
	 * @param url The URL to sign.
	 * @param expiration Optional.  When included an expiration parameter will be added before signing.
	 * @param credentials The credentials to be used to sign the URL.
	 * @return
	 * @throws MalformedURLException
	 */
	public static URL generatePreSignedURL(HttpMethod method, String url,
			Date expiration, String credentials) throws MalformedURLException {
		if(url == null){
			throw new IllegalArgumentException("URL cannot be null");
		}
		// parse the url.
		UrlData urlData = new UrlData(url);
		if(expiration != null){
			// add an expiration date
			urlData.getQueryParameters().put(EXPIRATION, ""+expiration.getTime());
		}
		// Generate the signature of the for the 
		String signature = generateSignature(method, urlData, credentials);
		// Add the signature to the URL
		urlData.getQueryParameters().put(HMAC_SIGNATURE, signature);
		return urlData.toURL();
	}
	
	/**
	 * Create the Canonical form of the given parameters. 
	 * @param method
	 * @param url
	 * @param signatureName
	 * @return
	 * @throws MalformedURLException
	 */
	public static String makeS3CanonicalString(HttpMethod method, String url) throws MalformedURLException {
		UrlData urlData = new UrlData(url);
		return makeS3CanonicalString(method, urlData);
	}
	
	/**
	 * Create the Canonical form of the given parameters. 
	 * @param method
	 * @param url Only the host, path, and query parameters contribute to the canonical form.
	 * @param signatureParameter
	 * @return
	 * @throws MalformedURLException
	 */
	public static String makeS3CanonicalString(HttpMethod method, UrlData urlData) throws MalformedURLException{
		if(method == null){
			throw new IllegalArgumentException("Method cannot be null");
		}
		// First parse the URL
		TreeMap<String, String> sortedTree = new TreeMap<String, String>(urlData.getQueryParameters());
		// The signature parameter must not be included in the signature.
		sortedTree.remove(HMAC_SIGNATURE);
		// Build up the string from the parts
		StringBuilder builder = new StringBuilder();
		builder.append(method.toString());
		builder.append(" ");
		builder.append(urlData.getHost());
		builder.append(" ");
		builder.append(urlData.getPath());
		if(!sortedTree.isEmpty()){
			builder.append("?");
			// Add the query parameters in alphabetical order
			int count = 0;
			Iterator<String> it = sortedTree.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				String value = sortedTree.get(key);
				if(count > 0){
					builder.append("&");
				}
				builder.append(key).append("=").append(value);
				count++;
			}
		}
		return builder.toString();
	}


	/**
	 * Generate a keyed-hash message authentication code (HMAC) using HMAC-SHA1
	 * 
	 * @param data
	 * @param keyString
	 * @return
	 */
	public static byte[] sign(String dataToSign, String credentials) {
		if(dataToSign == null){
			throw new IllegalArgumentException("Data cannot be null");
		}
		if(credentials == null){
			throw new IllegalArgumentException("Credentials cannot be null");
		}
		try {
			byte[] data = dataToSign.getBytes(UTF_8);
			byte[] key = credentials.getBytes(UTF_8);
			Mac mac = Mac.getInstance(HMAC_SHA1);
			mac.init(new SecretKeySpec(key, HMAC_SHA1));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}


	/**
	 * Validate the passed pre-signed URL.
	 * 
	 * @param method
	 * @param url
	 * @param credentials
	 * @throws MalformedURLException 
	 * @throws SignatureExpiredException 
	 */
	public static String validatePresignedURL(HttpMethod method, String url, String credentials) throws MalformedURLException, SignatureMismatchException, SignatureExpiredException{
		UrlData urlData = new UrlData(url);
		LinkedHashMap<String, String> parameters = urlData.getQueryParameters();
		String signature = parameters.get(HMAC_SIGNATURE);
		if(signature == null){
			throw new SignatureMismatchException(MSG_SIGNATURE_MISSING);
		}
		// Calculate the signature of the passed url
		String calculatedSignature = generateSignature(method, urlData, credentials);
		if(!calculatedSignature.equals(signature)){
			throw new SignatureMismatchException(MSG_SIGNATURE_DOES_NOT_MATCH);
		}
		// Is the URL expired?
		String expiresString = parameters.get(EXPIRATION);
		if(expiresString != null){
			try {
				long expires = Long.parseLong(expiresString);
				long now = System.currentTimeMillis();
				if(now > expires){
					throw new SignatureExpiredException(MSG_URL_EXPIRED, signature);
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Unknown format of "+EXPIRATION+" parameter: "+expiresString);
			}
		}
		// return the valid signature
		return signature;
 	}
}
