package org.sagebionetworks.repo.web;

import java.util.Objects;

public class HttpRequestIdentifier {
	Long userId;
	String sessionId;
	String ipAddress;
	String requestPath;

	public HttpRequestIdentifier(Long userId, String sessionId, String ipAddress, String requestPath){
		this.userId = userId;
		this.sessionId = sessionId;
		this.ipAddress = ipAddress;
		this.requestPath = requestPath;
	}

	public Long getUserId() {
		return userId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public String getUserMachineIdentifierString(){
		return userId + "|" + sessionId + "|" + ipAddress;
	}

	@Override
	public boolean equals(Object o) { //auto-generated
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HttpRequestIdentifier that = (HttpRequestIdentifier) o;
		return Objects.equals(userId, that.userId) &&
				Objects.equals(sessionId, that.sessionId) &&
				Objects.equals(ipAddress, that.ipAddress) &&
				Objects.equals(requestPath, that.requestPath);
	}

	@Override
	public int hashCode() { //auto-generated
		return Objects.hash(userId, sessionId, ipAddress, requestPath);
	}
}
