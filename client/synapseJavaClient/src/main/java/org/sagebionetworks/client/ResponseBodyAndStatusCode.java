package org.sagebionetworks.client;

public class ResponseBodyAndStatusCode {
	private String responseBody;
	private int statusCode;
	public ResponseBodyAndStatusCode(String responseBody, int statusCode) {
		super();
		this.responseBody = responseBody;
		this.statusCode = statusCode;
	}
	public String getResponseBody() {
		return responseBody;
	}
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((responseBody == null) ? 0 : responseBody.hashCode());
		result = prime * result + statusCode;
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
		ResponseBodyAndStatusCode other = (ResponseBodyAndStatusCode) obj;
		if (responseBody == null) {
			if (other.responseBody != null)
				return false;
		} else if (!responseBody.equals(other.responseBody))
			return false;
		if (statusCode != other.statusCode)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ResponseBodyAndStatusCode [responseBody=" + responseBody
				+ ", statusCode=" + statusCode + "]";
	}
	
	

}
