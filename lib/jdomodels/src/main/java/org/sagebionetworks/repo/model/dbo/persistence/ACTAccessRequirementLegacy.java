
package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;

public class ACTAccessRequirementLegacy {

    private Date createdOn;
    private Long id;
    private Date modifiedOn;
    private String concreteType = ACTAccessRequirement.class.getName();

    private String createdBy;
    private String etag;
    private List<RestrictableObjectDescriptor> subjectIds;

    private String modifiedBy;
    private String entityType;
    private String uri;
    private ACCESS_TYPE accessType;

    private String actContactInfo;

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
     * Indicates which implementation of AccessRequirement this object represents.
     * 
     * 
     * 
     * @return
     *     concreteType
     */
    public String getConcreteType() {
        return concreteType;
    }

    /**
     * Indicates which implementation of AccessRequirement this object represents.
     * 
     * 
     * 
     * @param concreteType
     */
    public void setConcreteType(String concreteType) {
        this.concreteType = concreteType;
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

    /**
     * The IDs of the items controlled by this Access Requirement.
     * 
     * 
     * 
     * @return
     *     subjectIds
     */
    public List<RestrictableObjectDescriptor> getSubjectIds() {
        return subjectIds;
    }

    /**
     * The IDs of the items controlled by this Access Requirement.
     * 
     * 
     * 
     * @param subjectIds
     */
    public void setSubjectIds(List<RestrictableObjectDescriptor> subjectIds) {
        this.subjectIds = subjectIds;
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
     * This field is deprecated and will be removed in future versions of Synapse
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
     * This field is deprecated and will be removed in future versions of Synapse
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
     * The enumeration of possible permission.
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
     * The enumeration of possible permission.
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



}
