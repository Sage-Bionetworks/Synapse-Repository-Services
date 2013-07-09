package org.sagebionetworks.javadoc.velocity.controller;

import java.util.LinkedList;
import java.util.List;

/**
 * Model of a controller web services request.
 * 
 * @author John
 *
 */
public class MethodModel {
	
	public static int MAX_SHORT_DESCRIPTION_LENGHT = 300;
	
	String fullMethodName;
	Link methodLink;
	Link responseBody;
	Link requestBody;
	String httpType;
	String url;
	String description;
	boolean isAuthenticationRequired;
	List<ParameterModel> pathVariables;
	List<ParameterModel> parameters;
	
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
	
	public boolean getIsAuthenticationRequired() {
		return isAuthenticationRequired;
	}
	public void setIsAuthenticationRequired(boolean authenticationRequired) {
		this.isAuthenticationRequired = authenticationRequired;
	}
	public List<ParameterModel> getPathVariables() {
		return pathVariables;
	}
	
	public void addPathVariable(ParameterModel pathVar){
		if(this.pathVariables == null){
			this.pathVariables = new LinkedList<ParameterModel>();
		}
		this.pathVariables.add(pathVar);
	}
	
	public void addParameter(ParameterModel param){
		if(this.parameters == null){
			this.parameters = new LinkedList<ParameterModel>();
		}
		this.parameters.add(param);
	}
	
	
	public void setPathVariables(List<ParameterModel> pathVariables) {
		this.pathVariables = pathVariables;
	}
	public List<ParameterModel> getParameters() {
		return parameters;
	}
	public void setParameters(List<ParameterModel> parameters) {
		this.parameters = parameters;
	}
	public String getShortDescription() {
		if(this.description.length() > MAX_SHORT_DESCRIPTION_LENGHT){
			StringBuilder builder = new StringBuilder();
			builder.append(this.description.substring(0, MAX_SHORT_DESCRIPTION_LENGHT-3)).append("...");
			return builder.toString();
		}else{
			return this.description;
		}
	}
	public Link getMethodLink() {
		return methodLink;
	}
	public void setMethodLink(Link methodLink) {
		this.methodLink = methodLink;
	}
	
	public String getFullMethodName() {
		return fullMethodName;
	}
	public void setFullMethodName(String fullMethodName) {
		this.fullMethodName = fullMethodName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((fullMethodName == null) ? 0 : fullMethodName.hashCode());
		result = prime * result
				+ ((httpType == null) ? 0 : httpType.hashCode());
		result = prime * result
				+ ((methodLink == null) ? 0 : methodLink.hashCode());
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
		if (fullMethodName == null) {
			if (other.fullMethodName != null)
				return false;
		} else if (!fullMethodName.equals(other.fullMethodName))
			return false;
		if (httpType == null) {
			if (other.httpType != null)
				return false;
		} else if (!httpType.equals(other.httpType))
			return false;
		if (methodLink == null) {
			if (other.methodLink != null)
				return false;
		} else if (!methodLink.equals(other.methodLink))
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
		return "MethodModel [fullMethodName=" + fullMethodName
				+ ", methodLink=" + methodLink + ", responseBody="
				+ responseBody + ", requestBody=" + requestBody + ", httpType="
				+ httpType + ", url=" + url + ", description=" + description
				+ "]";
	}

}
