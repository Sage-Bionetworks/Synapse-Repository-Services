package org.sagebionetworks.table.cluster;

import java.sql.Timestamp;
import java.util.Objects;

public class CachedQueryDto {
	
	private String requestHash;
	private String requestJson;
	private String resultJson;
	private Long runtimeMS;
	private Timestamp expiresOn;
	
	public String getRequestHash() {
		return requestHash;
	}
	public CachedQueryDto setRequestHash(String requestHash) {
		this.requestHash = requestHash;
		return this;
	}
	public String getRequestJson() {
		return requestJson;
	}
	public CachedQueryDto setRequestJson(String requestJson) {
		this.requestJson = requestJson;
		return this;
	}
	public String getResultJson() {
		return resultJson;
	}
	public CachedQueryDto setResultJson(String resultJson) {
		this.resultJson = resultJson;
		return this;
	}
	public Long getRuntimeMS() {
		return runtimeMS;
	}
	public CachedQueryDto setRuntimeMS(Long runtimeMS) {
		this.runtimeMS = runtimeMS;
		return this;
	}
	public Timestamp getExpiresOn() {
		return expiresOn;
	}
	public CachedQueryDto setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(expiresOn, requestHash, requestJson, resultJson, runtimeMS);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CachedQueryDto other = (CachedQueryDto) obj;
		return Objects.equals(expiresOn, other.expiresOn) && Objects.equals(requestHash, other.requestHash)
				&& Objects.equals(requestJson, other.requestJson) && Objects.equals(resultJson, other.resultJson)
				&& Objects.equals(runtimeMS, other.runtimeMS);
	}
	
	@Override
	public String toString() {
		return "CachedQueryDto [requestHash=" + requestHash + ", requestJson=" + requestJson + ", resultJson="
				+ resultJson + ", runtimeMS=" + runtimeMS + ", expiresOn=" + expiresOn + "]";
	}
	
}
