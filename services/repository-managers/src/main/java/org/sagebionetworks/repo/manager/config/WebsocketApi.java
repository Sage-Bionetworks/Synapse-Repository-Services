package org.sagebionetworks.repo.manager.config;

import java.util.Objects;
import java.util.StringJoiner;

public class WebsocketApi {

	private String apiId;
	private String apiEndpoint;
	private String apiName;
	private String stageName;

	public String getApiId() {
		return apiId;
	}

	public WebsocketApi setApiId(String apiId) {
		this.apiId = apiId;
		return this;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public WebsocketApi setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
		return this;
	}

	public String getApiName() {
		return apiName;
	}

	public WebsocketApi setApiName(String apiName) {
		this.apiName = apiName;
		return this;
	}

	public String getStageName() {
		return stageName;
	}

	public WebsocketApi setStageName(String stageName) {
		this.stageName = stageName;
		return this;
	}

	/**
	 * wss://<apiId>.execute-api.us-east-1.amazonaws.com/<stageName>
	 * @return
	 */
	public String getWssUrl() {
		return new StringJoiner("/").add(apiEndpoint).add(stageName).toString();
	}
	
	/**
	 * https://<apiId>.execute-api.us-east-1.amazonaws.com/<stageName>
	 * @return
	 */
	public String getHttpUrl() {
		return String.format("https://%s.execute-api.us-east-1.amazonaws.com/%s/", apiId, stageName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(apiEndpoint, apiId, apiName, stageName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebsocketApi other = (WebsocketApi) obj;
		return Objects.equals(apiEndpoint, other.apiEndpoint) && Objects.equals(apiId, other.apiId)
				&& Objects.equals(apiName, other.apiName) && Objects.equals(stageName, other.stageName);
	}

	@Override
	public String toString() {
		return "WebsocketApi [apiId=" + apiId + ", apiEndpoint=" + apiEndpoint + ", apiName=" + apiName + ", stageName="
				+ stageName + "]";
	}

}
