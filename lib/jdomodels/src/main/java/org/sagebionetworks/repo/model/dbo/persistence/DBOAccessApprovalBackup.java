/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;


/**
 * @author brucehoff
 *
 */
public class DBOAccessApprovalBackup  {
	private Long id;
	private String eTag;
	private Long createdBy;
	private long createdOn;
	private Long modifiedBy;
	private long modifiedOn;
	private Long requirementId;
	private Long accessorId;
	private String entityType;
	private byte[] serializedEntity;
	


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String geteTag() {
		return eTag;
	}


	public void seteTag(String eTag) {
		this.eTag = eTag;
	}


	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}


	public Long getModifiedBy() {
		return modifiedBy;
	}


	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}


	public Long getRequirementId() {
		return requirementId;
	}


	public void setRequirementId(Long requirementId) {
		this.requirementId = requirementId;
	}


	public Long getAccessorId() {
		return accessorId;
	}


	public void setAccessorId(Long accessorId) {
		this.accessorId = accessorId;
	}

	public long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}


	public long getModifiedOn() {
		return modifiedOn;
	}


	public void setModifiedOn(long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}


	public String getEntityType() {
		return entityType;
	}


	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}


	public byte[] getSerializedEntity() {
		return serializedEntity;
	}


	public void setSerializedEntity(byte[] serilizedEntity) {
		this.serializedEntity = serilizedEntity;
	}



	
}
