
package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.Date;

/**
 * Model object for end user license agreements
 * 
 * @author deflaux
 *
 */
public class EulaOld implements NodeableOld {

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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessControlList == null) ? 0 : accessControlList
						.hashCode());
		result = prime * result
				+ ((agreement == null) ? 0 : agreement.hashCode());
		result = prime * result + Arrays.hashCode(agreementBlob);
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EulaOld other = (EulaOld) obj;
		if (accessControlList == null) {
			if (other.accessControlList != null)
				return false;
		} else if (!accessControlList.equals(other.accessControlList))
			return false;
		if (agreement == null) {
			if (other.agreement != null)
				return false;
		} else if (!agreement.equals(other.agreement))
			return false;
		if (!Arrays.equals(agreementBlob, other.agreementBlob))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Eula [accessControlList=" + accessControlList + ", agreement="
				+ agreement + ", agreementBlob="
				+ Arrays.toString(agreementBlob) + ", annotations="
				+ annotations + ", creationDate=" + creationDate + ", etag="
				+ etag + ", id=" + id + ", name=" + name + ", parentId="
				+ parentId + ", uri=" + uri + "]";
	}
    
    
}
