package org.sagebionetworks.evaluation.dao;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class EvaluationFilter {

	private Set<Long> principalIds;
	private ACCESS_TYPE accessType;
	private Long timeFilter;
	private Long contentSource;
	private List<Long> idsFilter;

	public EvaluationFilter(UserInfo user, ACCESS_TYPE accessType) {
		this(user.getGroups(), accessType);
	}

	public EvaluationFilter(Set<Long> principalIds, ACCESS_TYPE accessType) {
		this.principalIds = principalIds;
		this.accessType = accessType;
	}

	public Set<Long> getPrincipalIds() {
		return principalIds;
	}

	public ACCESS_TYPE getAccessType() {
		return accessType;
	}

	public Long getTimeFilter() {
		return timeFilter;
	}

	public EvaluationFilter withTimeFilter(Long timeFilter) {
		this.timeFilter = timeFilter;
		return this;
	}

	public Long getContentSourceFilter() {
		return contentSource;
	}

	public EvaluationFilter withContentSourceFilter(String contentSource) {
		this.contentSource = contentSource == null ? null : KeyFactory.stringToKey(contentSource);
		return this;
	}

	public List<Long> getIdsFilter() {
		return idsFilter;
	}

	public EvaluationFilter withIdsFilter(List<Long> ids) {
		this.idsFilter = ids;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessType, contentSource, idsFilter, principalIds, timeFilter);
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
		EvaluationFilter other = (EvaluationFilter) obj;
		return accessType == other.accessType && Objects.equals(contentSource, other.contentSource)
				&& Objects.equals(idsFilter, other.idsFilter) && Objects.equals(principalIds, other.principalIds)
				&& Objects.equals(timeFilter, other.timeFilter);
	}

	@Override
	public String toString() {
		return "EvaluationFilter [principalIds=" + principalIds + ", accessType=" + accessType + ", timeFilter="
				+ timeFilter + ", contentSource=" + contentSource + ", idsFilter=" + idsFilter + "]";
	}

}
