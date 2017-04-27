package org.sagebionetworks.repo.model.query.entity;

import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.model.table.EntityField;

/**
 * Enumeration that provides the mapping between node fields used for query
 * and the columns of the entity replication table.
 *
 */
public enum NodeToEntity {
	
	nodeType(NodeField.NODE_TYPE, EntityField.type, new DefaultValueTransformer()),
	modifiedOn(NodeField.MODIFIED_ON, EntityField.modifiedOn, new DefaultValueTransformer()),
	versionNumber(NodeField.VERSION_NUMBER, EntityField.currentVersion, new DefaultValueTransformer()),
	parentId(NodeField.PARENT_ID, EntityField.parentId, new SynapseIdTransfromer()),
	versionComment(NodeField.VERSION_COMMENT, EntityField.currentVersion, new DefaultValueTransformer()),
	createdByPrincipalId(NodeField.CREATED_BY, EntityField.createdBy, new DefaultValueTransformer()),
	eTag(NodeField.E_TAG, EntityField.etag, new DefaultValueTransformer()),
	modifiedByPrincipalId(NodeField.MODIFIED_BY, EntityField.modifiedBy, new DefaultValueTransformer()),
	versionLabel(NodeField.VERSION_LABEL, EntityField.currentVersion, new DefaultValueTransformer()),
	createdOn(NodeField.CREATED_ON, EntityField.createdOn, new DefaultValueTransformer()),
	name(NodeField.NAME, EntityField.name, new DefaultValueTransformer()),
	alias(NodeField.ALIAS_ID, null, new DefaultValueTransformer()),
	projectId(NodeField.PROJECT_ID, EntityField.projectId, new SynapseIdTransfromer()),
	id(NodeField.ID, EntityField.id, new SynapseIdTransfromer()),
	benefactorId(NodeField.BENEFACTOR_ID, EntityField.benefactorId, new SynapseIdTransfromer()),
	;
	
	NodeField nodeField;
	EntityField entityField;
	ValueTransformer transformer;
	
	NodeToEntity(NodeField node, EntityField entity, ValueTransformer transformer){
		this.nodeField = node;
		this.entityField = entity;
		this.transformer = transformer;
	}
	
	public Object transformerValue(Object value){
		return this.transformer.transform(value);
	}

	/**
	 * Is the given key a node field?
	 * 
	 * @param key
	 * @return
	 */
	public static boolean isNodeField(String key) {
		try{
			NodeToEntity.valueOf(key);
			return true;
		}catch(IllegalArgumentException e){
			return false;
		}
	}
}
