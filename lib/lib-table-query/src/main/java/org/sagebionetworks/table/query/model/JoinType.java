package org.sagebionetworks.table.query.model;

import java.util.List;

public class JoinType extends LeafElement{
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

}
