package org.sagebionetworks.repo.model.query.entity;

import static org.sagebionetworks.repo.model.table.TableConstants.*;

/**
 * Represents a single join with the annotations table
 * to match an annotation expression.
 *
 */
public class AnnotationJoin extends SqlElement {
	
	public static final String BIND_PREFIX = "bJoinName";
	
	String tableAlias;
	String bindName;
	String columnName;
	boolean leftJoin;

	public AnnotationJoin(ColumnReference reference, boolean leftJoin) {
		this.tableAlias = reference.getAnnotationAlias();
		this.bindName = BIND_PREFIX+reference.getColumnIndex();
		this.columnName = reference.getAnnotationName();
		this.leftJoin = leftJoin;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(leftJoin){
			builder.append(" LEFT");
		}
		builder.append(" JOIN ");
		builder.append(ANNOTATION_REPLICATION_TABLE);
		builder.append(" ");
		builder.append(this.tableAlias);
		builder.append(" ON (");
		builder.append("R.");
		builder.append(ENTITY_REPLICATION_COL_ID);
		builder.append(" = ");
		builder.append(this.tableAlias);
		builder.append(".");
		builder.append(ANNOTATION_REPLICATION_COL_ENTITY_ID);
		builder.append(" AND ");
		builder.append(this.tableAlias);
		builder.append(".");
		builder.append(ANNOTATION_REPLICATION_COL_KEY);
		builder.append(" = :");
		builder.append(bindName);
		builder.append(")");
	}

	@Override
	public void bindParameters(Parameters parameters) {
		parameters.put(this.bindName, this.columnName);
	}

}
