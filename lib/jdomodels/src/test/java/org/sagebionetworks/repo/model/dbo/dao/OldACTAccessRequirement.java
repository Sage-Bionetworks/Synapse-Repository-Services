package org.sagebionetworks.repo.model.dbo.dao;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectValidator;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;


public class OldACTAccessRequirement {

    /**
     * Created On
     * 
     */
    private Date createdOn;
    private Long id;
    /**
     * Modified On
     * 
     */
    private Date modifiedOn;
    /**
     * Created By
     * 
     */
    private String createdBy;
    private String etag;
    private List<Long> entityIds;
    /**
     * Modified By
     * 
     */
    private String modifiedBy;
    private String entityType;
    private String uri;
    private ACCESS_TYPE accessType;
    /**
     * ACT Contact Information
     * 
     */
    private String actContactInfo;

    public OldACTAccessRequirement() {
    }


    /**
     * Created On
     * 
     * The date this object was created.
     * 
     * 
     * 
     * @return
     *     createdOn
     */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * Created On
     * 
     * The date this object was created.
     * 
     * 
     * 
     * @param createdOn
     */
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
     * The unique immutable ID
     * 
     * 
     * 
     * @return
     *     id
     */
    public Long getId() {
        return id;
    }

    /**
     * The unique immutable ID
     * 
     * 
     * 
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Modified On
     * 
     * The date this object was last modified.
     * 
     * 
     * 
     * @return
     *     modifiedOn
     */
    public Date getModifiedOn() {
        return modifiedOn;
    }

    /**
     * Modified On
     * 
     * The date this object was last modified.
     * 
     * 
     * 
     * @param modifiedOn
     */
    public void setModifiedOn(Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    /**
     * Created By
     * 
     * The user that created this object.
     * 
     * 
     * 
     * @return
     *     createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Created By
     * 
     * The user that created this object.
     * 
     * 
     * 
     * @param createdBy
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle concurrent updates. Since the E-Tag changes every time an entity is updated it is used to detect when a client's current representation of an object is out-of-date.
     * 
     * 
     * 
     * @return
     *     etag
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle concurrent updates. Since the E-Tag changes every time an entity is updated it is used to detect when a client's current representation of an object is out-of-date.
     * 
     * 
     * 
     * @param etag
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

 
    public List<Long> getEntityIds() {
		return entityIds;
	}


	public void setEntityIds(List<Long> entityIds) {
		this.entityIds = entityIds;
	}


	/**
     * Modified By
     * 
     * The user that last modified this object.
     * 
     * 
     * 
     * @return
     *     modifiedBy
     */
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Modified By
     * 
     * The user that last modified this object.
     * 
     * 
     * 
     * @param modifiedBy
     */
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * The full class name of this entity.
     * 
     * 
     * 
     * @return
     *     entityType
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * The full class name of this entity.
     * 
     * 
     * 
     * @param entityType
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * The Uniform Resource Identifier (URI) for this object.
     * 
     * 
     * 
     * @return
     *     uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * The Uniform Resource Identifier (URI) for this object.
     * 
     * 
     * 
     * @param uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * JSON schema for the access types allowed in access control lists.
     * 
     * 
     * 
     * @return
     *     accessType
     */
    public ACCESS_TYPE getAccessType() {
        return accessType;
    }

    /**
     * JSON schema for the access types allowed in access control lists.
     * 
     * 
     * 
     * @param accessType
     */
    public void setAccessType(ACCESS_TYPE accessType) {
        this.accessType = accessType;
    }

    /**
     * ACT Contact Information
     * 
     * Information on how to contact the ACT Team for access approval (external to Synapse)
     * 
     * 
     * 
     * @return
     *     actContactInfo
     */
    public String getActContactInfo() {
        return actContactInfo;
    }

    /**
     * ACT Contact Information
     * 
     * Information on how to contact the ACT Team for access approval (external to Synapse)
     * 
     * 
     * 
     * @param actContactInfo
     */
    public void setActContactInfo(String actContactInfo) {
        this.actContactInfo = actContactInfo;
    }


 
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result
				+ ((actContactInfo == null) ? 0 : actContactInfo.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((entityIds == null) ? 0 : entityIds.hashCode());
		result = prime * result
				+ ((entityType == null) ? 0 : entityType.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OldACTAccessRequirement other = (OldACTAccessRequirement) obj;
		if (accessType != other.accessType)
			return false;
		if (actContactInfo == null) {
			if (other.actContactInfo != null)
				return false;
		} else if (!actContactInfo.equals(other.actContactInfo))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (entityIds == null) {
			if (other.entityIds != null)
				return false;
		} else if (!entityIds.equals(other.entityIds))
			return false;
		if (entityType == null) {
			if (other.entityType != null)
				return false;
		} else if (!entityType.equals(other.entityType))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

    @Override
	public String toString() {
		return "OldACTAccessRequirement [createdOn=" + createdOn + ", id=" + id
				+ ", modifiedOn=" + modifiedOn + ", createdBy=" + createdBy
				+ ", etag=" + etag + ", entityIds=" + entityIds
				+ ", modifiedBy=" + modifiedBy + ", entityType=" + entityType
				+ ", uri=" + uri + ", accessType=" + accessType
				+ ", actContactInfo=" + actContactInfo + "]";
	}

}
