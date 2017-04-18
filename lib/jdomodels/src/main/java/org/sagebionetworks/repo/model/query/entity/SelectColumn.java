package org.sagebionetworks.repo.model.query.entity;

/**
 * SQL element for a select column.
 *
 */
public class SelectColumn extends SqlElement {

	NodeToEntity nodeToEntity;
	String annotationName;
	
	/**
	 * This column represents an entity field.
	 * 
	 * @param nodeToEntity
	 */
	public SelectColumn(NodeToEntity nodeToEntity) {
		super();
		this.nodeToEntity = nodeToEntity;
	}

	/**
	 * This column represents an annotation.
	 * 
	 * @param annotationName
	 */
	public SelectColumn(String annotationName) {
		super();
		this.annotationName = annotationName;
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
			builder.append(" AS '");
			builder.append(nodeToEntity.nodeField.getFieldName());
			builder.append("'");

		}else{
			builder.append("NULL AS '");
			builder.append(annotationName);
			builder.append("'");
		}
	}

	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}

}
