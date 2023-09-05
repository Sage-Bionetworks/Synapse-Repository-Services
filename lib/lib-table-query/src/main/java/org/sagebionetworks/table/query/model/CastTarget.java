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
	
	// Constructor that is not exposed to the parser and used internally to replace the target with the MySql equivalent type
	// The original column type is retained
	public CastTarget(ColumnType type, String castTypeString) {
		this.columnId = null;
		this.type = type;
		this.castTypeString = castTypeString;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(castTypeString != null) {
			builder.append(castTypeString);
		} else if (type!= null) {
			builder.append(type.name());
		} else if (columnId != null) {
			columnId.toSql(builder, parameters);
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
