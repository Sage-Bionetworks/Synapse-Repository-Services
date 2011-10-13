
package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * Model object for end user license agreements
 * 
 * @author deflaux
 *
 */
public class Eula implements Nodeable {

    private byte[] agreementBlob;
    private Date creationDate;
    private String etag;
    private String id;
    private String name;
    private String parentId;
    private String uri;

	@TransientField
    private String agreement;
	@TransientField
    private String accessControlList;
	@TransientField
    private String annotations;

    public String getAccessControlList() {
        return accessControlList;
    }

    public void setAccessControlList(String accessControlList) {
        this.accessControlList = accessControlList;
    }

    /**
     * @return the agreement verbiage as a String
     */
    public String getAgreement() {
        return agreement;
    }

    /**
     * @param agreement
     */
    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    /**
     * @return the agreement verbiage as a byte array
     */
    public byte[] getAgreementBlob() {
		return agreementBlob;
	}

    /**
     * @param agreementBlob
     */
    public void setAgreementBlob(byte[] agreementBlob) {
		this.agreementBlob = agreementBlob;
	}

    public String getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
