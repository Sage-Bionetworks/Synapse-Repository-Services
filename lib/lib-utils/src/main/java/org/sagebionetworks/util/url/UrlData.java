package org.sagebionetworks.util.url;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;

/**
 * Data parsed out of a URL.
 * 
 */
public class UrlData {

	String host;
	int port;
	String path;
	String reference;
	String protocol;
	LinkedHashMap<String, String> queryParameters;

	public UrlData(String urlString) throws MalformedURLException {
		if (urlString == null) {
			throw new IllegalArgumentException("UrlString cannot be null");
		}
		URL url = new URL(urlString);
		host = url.getHost();
		path = url.getPath();
		protocol = url.getProtocol();
		reference = url.getRef();
		port = url.getPort();
		queryParameters = parseQueryString(url.getQuery());
	}

	/**
	 * Parse the query string into a map with each parameter key and value.
	 * Note: Values are URL decoded.
	 * 
	 * @param queryString
	 * @return
	 */
	public static LinkedHashMap<String, String> parseQueryString(
			String queryString) {
		LinkedHashMap<String, String> queryParameters = new LinkedHashMap<String, String>(
				0);
		// Parse the query string
		if (queryString != null) {
			String[] pairs = queryString.split("&");
			for (String pair : pairs) {
				String split[] = pair.split("=");
				if (split.length < 1) {
					new MalformedURLException(
							"Unable to read query string pair: " + pair);
				}
				String key = split[0];
				if ("".equals(key)) {
					continue;
				}
				String value = "";
				if (split.length > 1) {
					value = split[1];
				}
				// Parameters can be ULR encoded
				try {
					value = URLDecoder.decode(value, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				queryParameters.put(key, value);
			}
		}
		return queryParameters;
	}

	/**
	 * Convert back to a URL.
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public URL toURL() throws MalformedURLException {
		// To build an URL the path, query and reference must be combined into a
		// file.
		StringBuilder fileBuilder = new StringBuilder();
		if (path != null) {
			fileBuilder.append(path);
		}
		if (queryParameters != null && !queryParameters.isEmpty()) {
			fileBuilder.append("?");
			fileBuilder.append(toQueryString(queryParameters));
		}
		if (reference != null) {
			fileBuilder.append("#");
			fileBuilder.append(reference);
		}
		return new URL(protocol, host, port, fileBuilder.toString());
	}

	/**
	 * Write the query paramters to a query string.
	 * 
	 * @param queryParameters
	 * @return
	 */
	public static String toQueryString(
			LinkedHashMap<String, String> queryParameters) {
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for (String key : queryParameters.keySet()) {
			String value = queryParameters.get(key);
			// values must be url encoded
			try {
				value = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			if (count > 0) {
				builder.append("&");
			}
			builder.append(key);
			builder.append("=");
			builder.append(value);
			count++;
		}
		return builder.toString();
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public LinkedHashMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	public void setQueryParameters(LinkedHashMap<String, String> queryParameters) {
		this.queryParameters = queryParameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + port;
		result = prime * result
				+ ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result
				+ ((queryParameters == null) ? 0 : queryParameters.hashCode());
		result = prime * result
				+ ((reference == null) ? 0 : reference.hashCode());
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
		UrlData other = (UrlData) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (port != other.port)
			return false;
		if (protocol == null) {
			if (other.protocol != null)
				return false;
		} else if (!protocol.equals(other.protocol))
			return false;
		if (queryParameters == null) {
			if (other.queryParameters != null)
				return false;
		} else if (!queryParameters.equals(other.queryParameters))
			return false;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}

}
