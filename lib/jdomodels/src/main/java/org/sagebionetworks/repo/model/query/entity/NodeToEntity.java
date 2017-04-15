package org.sagebionetworks.repo.model.query.entity;

import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.model.table.EntityField;

/**
 * Enumeration that provides the mapping between node fields used for query
 * and the columns of the entity replication table.
 *
 */
public enum NodeToEntity {
	
	nodeType(NodeField.NODE_TYPE, EntityField.type),
	modifiedOn(NodeField.MODIFIED_ON, EntityField.modifiedOn),
	versionNumber(NodeField.VERSION_NUMBER, EntityField.currentVersion),
	parentId(NodeField.PARENT_ID, EntityField.parentId),
	versionComment(NodeField.VERSION_COMMENT, EntityField.currentVersion),
	createdByPrincipalId(NodeField.CREATED_BY, EntityField.createdBy),
	eTag(NodeField.E_TAG, EntityField.etag),
	modifiedByPrincipalId(NodeField.MODIFIED_BY, EntityField.modifiedBy),
	versionLabel(NodeField.VERSION_LABEL, EntityField.currentVersion),
	createdOn(NodeField.CREATED_ON, EntityField.createdOn),
	name(NodeField.NAME, EntityField.name),
	alias(NodeField.ALIAS_ID, null),
	projectId(NodeField.PROJECT_ID, EntityField.projectId),
	id(NodeField.ID, EntityField.id),
	benefactorId(NodeField.BENEFACTOR_ID, EntityField.benefactorId),
	;
	
	NodeField nodeField;
	EntityField entityField;
	NodeToEntity(NodeField node, EntityField entity){
		this.nodeField = node;
		this.entityField = entity;
	}
}
