package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A view filter that will limit the results to rows with the provided vector
 * IDs.
 */
public class VectorIdViewFilter implements ViewFilter {

	private List<Object[]> idTuples;

	public VectorIdViewFilter(List<LogicalTimestamp> vectorIds) {
		ValidateArgument.required(vectorIds, "vectorIds");
		ValidateArgument.requirement(!vectorIds.isEmpty(), "vectorIds cannot be empty");
		idTuples = vectorIds.stream().map(ts -> new Object[] { ts.getReplicaId(), ts.getSequenceNumber() })
				.collect(Collectors.toList());
	}

	@Override
	public String getConditionSql(int index) {
		return String.format("(V1.VEC_REP, V1.VEC_SEQ) IN (:vectorIds%d)", index);
	}

	@Override
	public String getParameterKey(int index) {
		return String.format("vectorIds%d", index);
	}

	@Override
	public Object getParameterValue() {
		return idTuples;
	}

	@Override
	public int hashCode() {
		return Objects.hash(idTuples.stream().mapToInt(Arrays::hashCode).toArray());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VectorIdViewFilter other = (VectorIdViewFilter) obj;
		if (idTuples.size() != other.idTuples.size())
			return false;
		for (int i = 0; i < idTuples.size(); i++) {
			if (!Arrays.equals(idTuples.get(i), other.idTuples.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "VectorIdViewFilter [idTuples=" + idTuples.stream().map(Arrays::toString).collect(Collectors.toList())
				+ "]";
	}

}
