package org.sagebionetworks.simpleHttpClient;

import java.util.List;

/**
 * This object represents a simple HttpResponse.
 * 
 * A SimpleHttpResponse only keeps information about the status code, status reason, and the content of the response.
 * 
 * This should only be used for responses whose content fits in memory.
 * 
 * @author kimyentruong
 *
 */
public class SimpleHttpResponse {

	private int statusCode;
	private String statusReason;
	private String content;
	private List<Header> headers;

	SimpleHttpResponse(int statusCode, String statusReason, String content, List<Header> headers){
		this.statusCode = statusCode;
		this.statusReason = statusReason;
		this.content = content;
		this.headers = headers;
	}

	public int getStatusCode() {
		return statusCode;
	}
	public String getStatusReason() {
		return statusReason;
	}
	public String getContent() {
		return content;
	}
	public Header getFirstHeader(final String name) {
		if (this.headers == null) {
			return null;
		}
		for (Header header : headers) {
			if (header.getName().equalsIgnoreCase(name)) {
				return header;
			}
		}
		return null;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + statusCode;
		result = prime * result + ((statusReason == null) ? 0 : statusReason.hashCode());
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
		SimpleHttpResponse other = (SimpleHttpResponse) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (statusCode != other.statusCode)
			return false;
		if (statusReason == null) {
			if (other.statusReason != null)
				return false;
		} else if (!statusReason.equals(other.statusReason))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SimpleHttpResponse [statusCode=" + statusCode + ", statusReason=" + statusReason + ", content="
				+ content + ", headers=" + headers + "]";
	}
}
