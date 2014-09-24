
package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;

import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTApprovalStatus;


/**
 * ACT Access Approval
 * 
 * JSON schema for ACT Access Approval, used for a 'tier 3' Access Approval
 * 
 * Note: This class was auto-generated, and should not be directly modified.
 * 
 */
public class ACTAccessApprovalLegacy {

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
    private String concreteType = ACTAccessApproval.class.getName();
    /**
     * Created By
     * 
     */
    private String createdBy;
    private String etag;
    /**
     * Modified By
     * 
     */
    private String modifiedBy;
    /**
     * Accessor
     * 
     */
    private String accessorId;
    private String entityType;
    private Long requirementId;
    private String uri;
    private ACTApprovalStatus approvalStatus;


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
     * Indicates which implementation of AccessApproval this object represents.
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
     * Indicates which implementation of AccessApproval this object represents.
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
     * Accessor
     * 
     * The ID of the principal (user or group) approved for access
     * 
     * 
     * 
     * @return
     *     accessorId
     */
    public String getAccessorId() {
        return accessorId;
    }

    /**
     * Accessor
     * 
     * The ID of the principal (user or group) approved for access
     * 
     * 
     * 
     * @param accessorId
     */
    public void setAccessorId(String accessorId) {
        this.accessorId = accessorId;
    }

    /**
     * This field is deprecated and will be removed in future versions of Synapse.
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
     * This field is deprecated and will be removed in future versions of Synapse.
     * 
     * 
     * 
     * @param entityType
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * The ID of the Access Requirement that this object approves.
     * 
     * 
     * 
     * @return
     *     requirementId
     */
    public Long getRequirementId() {
        return requirementId;
    }

    /**
     * The ID of the Access Requirement that this object approves.
     * 
     * 
     * 
     * @param requirementId
     */
    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
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
     * JSON schema for the approval status of an 'ACT' controlled tier 3 access request.
     * 
     * 
     * 
     * @return
     *     approvalStatus
     */
    public ACTApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    /**
     * JSON schema for the approval status of an 'ACT' controlled tier 3 access request.
     * 
     * 
     * 
     * @param approvalStatus
     */
    public void setApprovalStatus(ACTApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }


}
