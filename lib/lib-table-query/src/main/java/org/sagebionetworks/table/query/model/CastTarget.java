package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * <cast target> ::= {@link ColumnType } | {@link UnsignedInteger} 
 *
 */
public class CastTarget extends SQLElement implements Replaceable<CastTarget> {
	
	private final ColumnType type;
	private final UnsignedInteger columnId;
	private final String castTypeString;
	
	public CastTarget(ColumnType type) {
		this.type = type;
		this.columnId = null;
		this.castTypeString = null;
	}
	
	public CastTarget(UnsignedInteger columnId) {
		this.columnId = columnId;
		this.type = null;
		this.castTypeString = null;
	}
	
	public CastTarget(String castTypeString) {
		this.columnId = null;
		this.type = null;
		this.castTypeString = castTypeString;
	}
	
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(type!= null) {
			builder.append(type.name());
		}
		if(columnId != null) {
			columnId.toSql(builder, parameters);
		}
		if(castTypeString != null) {
			builder.append(castTypeString);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnId);
	}

	public ColumnType getType() {
		return type;
	}

	public UnsignedInteger getColumnId() {
		return columnId;
	}

}
