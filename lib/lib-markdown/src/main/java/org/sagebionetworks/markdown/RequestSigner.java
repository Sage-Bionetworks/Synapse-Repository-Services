package org.sagebionetworks.markdown;

import java.net.URI;
import java.util.Map;

/**
 * Signs HTTP requests with AWS Signature Version 4.
 */
public interface RequestSigner {

	/**
	 * Signs an HTTP request and returns the signed headers.
	 *
	 * @param uri the request URI
	 * @param payload the request payload as a byte array
	 * @return a map of signed headers (header name → header value)
	 */
	Map<String, String> sign(URI uri, byte[] payload);
}
