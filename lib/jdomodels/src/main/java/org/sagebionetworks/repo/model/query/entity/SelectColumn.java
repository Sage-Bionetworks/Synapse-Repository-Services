package org.sagebionetworks.repo.model.query.entity;


/**
 * SQL element for a select column.
 *
 */
public class SelectColumn extends SqlElement {

	ColumnReference columnReference;


	/**
	 * Select column can represent a node field or an annotation.
	 * 
	 * @param name The name of the field.
	 */
	public SelectColumn(ColumnReference columnRefrence) {
		this.columnReference = columnRefrence;
	}

	@Override
	public void toSql(StringBuilder builder) {
		columnReference.toSql(builder);
		builder.append(" AS '");
		if(columnReference.nodeToEntity != null){
			builder.append(columnReference.nodeToEntity.nodeField.getFieldName());
		}else{
			builder.append(columnReference.annotationName);
		}
		builder.append("'");
	}

	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}

}
