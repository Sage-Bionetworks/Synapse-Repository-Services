package org.sagebionetworks.table.query.model;

import java.util.List;

public class JoinType extends SQLElement{
	OuterJoinType outerJoinType;

	public JoinType(OuterJoinType outerJoinType) {
		this.outerJoinType = outerJoinType;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(outerJoinType != null) {
			builder.append(outerJoinType.name());
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		//intentionally blank for now since there's currently no child SQLElement fields.
	}
}
