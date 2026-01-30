package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.repo.model.grid.query.RowIdFilter;
import org.sagebionetworks.util.ValidateArgument;

public class VectorIdFilterElement implements FilterElement {

	private List<Object[]> idTuples;

	public VectorIdFilterElement(Filter filter) {
		this((RowIdFilter) filter);
	}

	public VectorIdFilterElement(RowIdFilter filter) {
        ValidateArgument.required(filter.getRowIdsIn(), "rowIdsIn");
        ValidateArgument.requirement(!filter.getRowIdsIn().isEmpty(), "rowIdsIn cannot be empty");
        List<LogicalTimestamp> tsList = filter.getRowIdsIn()
                .stream()
                .map(LogicalTimestamp::parse)
                .collect(Collectors.toList());
        initFromLogicalTimestamps(tsList);
	}

	public VectorIdFilterElement(List<LogicalTimestamp> vectorIds) {
		initFromLogicalTimestamps(vectorIds);
	}
	
    private void initFromLogicalTimestamps(List<LogicalTimestamp> vectorIds) {
        ValidateArgument.required(vectorIds, "vectorIds");
        ValidateArgument.requirement(!vectorIds.isEmpty(), "vectorIds cannot be empty");
        idTuples = vectorIds.stream()
                .map(ts -> new Object[]{ts.getReplicaId(), ts.getSequenceNumber()})
                .collect(Collectors.toList());
    }

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		int index = params.size();
		String bind = String.format("vectorIds%d", index);
		sqlBuilder.append(String.format(" (VEC_REP, VEC_SEQ) IN (:%s)", bind));
		params.put(bind, idTuples);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idTuples);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VectorIdFilterElement other = (VectorIdFilterElement) obj;
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
		return "VectorIdFilterElement [idTuples=" + idTuples + "]";
	}

}
