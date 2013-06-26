package org.sagebionetworks.javadoc.velocity.controller;

/**
 * Model of a controller web services request.
 * 
 * @author John
 *
 */
public class MethodModel {
	
	Link responseBody;
	Link requestBody;
	String httpType;
	String url;
	String description;
	public Link getResponseBody() {
		return responseBody;
	}
	public void setResponseBody(Link responseBody) {
		this.responseBody = responseBody;
	}
	public Link getRequestBody() {
		return requestBody;
	}
	public void setRequestBody(Link requestBody) {
		this.requestBody = requestBody;
	}
	public String getHttpType() {
		return httpType;
	}
	public void setHttpType(String httpType) {
		this.httpType = httpType;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((httpType == null) ? 0 : httpType.hashCode());
		result = prime * result
				+ ((requestBody == null) ? 0 : requestBody.hashCode());
		result = prime * result
				+ ((responseBody == null) ? 0 : responseBody.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		MethodModel other = (MethodModel) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (httpType == null) {
			if (other.httpType != null)
				return false;
		} else if (!httpType.equals(other.httpType))
			return false;
		if (requestBody == null) {
			if (other.requestBody != null)
				return false;
		} else if (!requestBody.equals(other.requestBody))
			return false;
		if (responseBody == null) {
			if (other.responseBody != null)
				return false;
		} else if (!responseBody.equals(other.responseBody))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "MethodModel [responseBody=" + responseBody + ", requestBody="
				+ requestBody + ", httpType=" + httpType + ", url=" + url
				+ ", description=" + description + "]";
	}

}
