package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.Context;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.util.ValidateArgument;

public class RowSelectionFilterElement implements FilterElement {
	private Boolean filterSelected;

	public RowSelectionFilterElement(Filter filter) {
		this((RowSelectionFilter) filter);
	}

	public RowSelectionFilterElement(RowSelectionFilter filter) {
		this.filterSelected = filter.getIsSelected();
	}

	public RowSelectionFilterElement() {
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(context.getHeader(), "context.header");

		if (filterSelected == null) {
			filterSelected = true;
		}

		ReplicaSelectionModel model = context.getHeader().getReplicaSelectionModel();

		if (model == null) {
			sqlBuilder.append(filterSelected ? " 1=0" : " 1");
			return;
		}

		if (Boolean.TRUE.equals(model.getRowSelectAll())) {
			sqlBuilder.append(filterSelected ? " 1" : " 1=0");
			return;
		}

		if (model.getRowSelection() == null || model.getRowSelection().isEmpty()) {
			sqlBuilder.append(filterSelected ? " 1=0" : " 1");
			return;
		}

		List<Object[]> arrIdTuples = model.getRowSelection().stream().map(s -> new Object[] { s.getRep(), s.getSeq() })
				.collect(Collectors.toList());

		String bind = "arrIds" + params.size();
		sqlBuilder.append(" (AN_REP, AN_SEQ) ").append(filterSelected ? " IN" : " NOT IN").append("(:").append(bind)
				.append(")");

		params.put(bind, arrIdTuples);
	}

	public Boolean getFilterSelected() {
		return filterSelected;
	}

	public RowSelectionFilterElement setFilterSelected(Boolean filterSelected) {
		this.filterSelected = filterSelected;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(filterSelected);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		RowSelectionFilterElement other = (RowSelectionFilterElement) obj;
		return Objects.equals(filterSelected, other.filterSelected);
	}

	@Override
	public String toString() {
		return "RowSelectionFilterElement [filterSelected=" + filterSelected + "]";
	}
}
