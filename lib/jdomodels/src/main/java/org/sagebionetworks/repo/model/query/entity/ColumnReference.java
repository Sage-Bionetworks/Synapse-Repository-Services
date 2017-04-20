package org.sagebionetworks.repo.model.query.entity;

import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_VALUE;

/**
 * Represents a reference to a column
 *
 */
public class ColumnReference extends SqlElement {

	NodeToEntity nodeToEntity;
	String annotationName;
	String annotationAlias;
	Integer columnIndex;

	/**
	 * Represents an annotation.
	 * @param columnName
	 * @param columnIndex
	 */
	public ColumnReference(String columnName, int columnIndex) {
		try{
			NodeToEntity type = NodeToEntity.valueOf(columnName);
			this.nodeToEntity = type;
		}catch (IllegalArgumentException e){
			this.annotationName = columnName;
			this.annotationAlias = "A"+columnIndex;
		}
		this.columnIndex = columnIndex;
	}


	@Override
	public void toSql(StringBuilder builder) {
		if(nodeToEntity != null){
			if(nodeToEntity.entityField == null){
				builder.append("NULL");
			}else{
				builder.append(Constants.ENTITY_REPLICATION_ALIAS);
				builder.append(".");
				builder.append(nodeToEntity.entityField.getDatabaseColumnName());
			}
		}else{
			builder.append(annotationAlias);
			builder.append(".");
			builder.append(ANNOTATION_REPLICATION_COL_VALUE);
		}
	}
	
	/**
	 * If this is an annotation expression then
	 * the alias of the column will be returned.
	 * Will return null for a node field.
	 * @return
	 */
	public String getAnnotationAlias(){
		return this.annotationAlias;
	}


	/**
	 * The index of this column
	 * @return
	 */
	public Integer getColumnIndex() {
		return columnIndex;
	}


	/**
	 * If this is an annotation expression then
	 * the name of the column will be returned.
	 * Will return null for a node field.
	 * @return
	 */
	public String getAnnotationName() {
		return annotationName;
	}


	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}
	
}
