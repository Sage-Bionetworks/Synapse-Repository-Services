package org.sagebionetworks.simpleHttpClient;

import java.util.Map;

/**
 * This object represents a simple HttpRequest.
 * 
 * A SimpleHttpRequest only keeps information about the URI and the headers of the request.
 * 
 * @author kimyentruong
 *
 */
public class SimpleHttpRequest {

	private String uri;
	private Map<String, String> headers;

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleHttpRequest other = (SimpleHttpRequest) obj;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SimpleHttpRequest [uri=" + uri + ", headers=" + headers + "]";
	}
}
