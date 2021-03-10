package org.sagebionetworks.repo.model.file;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IdRange {
	
	private long minId;
	private long maxId;
	
	@JsonCreator
	public IdRange(@JsonProperty("minId") long minId, @JsonProperty("maxId") long maxId) {
		this.minId = minId;
		this.maxId = maxId;
	}

	public long getMinId() {
		return minId;
	}

	public long getMaxId() {
		return maxId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(maxId, minId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IdRange other = (IdRange) obj;
		return maxId == other.maxId && minId == other.minId;
	}

	@Override
	public String toString() {
		return "IdRange [minId=" + minId + ", maxId=" + maxId + "]";
	}
	
	

}
