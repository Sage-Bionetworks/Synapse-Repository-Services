package org.sagebionetworks.bridge.model;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

public class ParticipantDataId {
	private final long id;

	public ParticipantDataId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		ParticipantDataId other = (ParticipantDataId) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ParticipantDataId [id=" + id + "]";
	}

	public static List<Long> convert(Collection<ParticipantDataId> participantDataIds) {
		List<Long> dataIds = Lists.newArrayListWithCapacity(participantDataIds.size());
		for (ParticipantDataId participantDataId : participantDataIds) {
			dataIds.add(participantDataId.getId());
		}
		return dataIds;
	}
}
