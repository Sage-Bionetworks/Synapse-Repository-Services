package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * <cast target> ::= {@link ColumnType } | {@link UnsignedInteger} 
 *
 */
public class CastTarget extends SQLElement {
	
	private final ColumnType type;
	private final UnsignedInteger columnId;
	
	public CastTarget(ColumnType type) {
		this.type = type;
		this.columnId = null;
	}
	
	public CastTarget(UnsignedInteger columnId) {
		this.columnId = columnId;
		this.type = null;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(type!= null) {
			builder.append(type.name());
		}
		if(columnId != null) {
			columnId.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnId);
	}

}
