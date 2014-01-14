package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;

public class DBOUserGroupBackup {

	private Long id;
	// Name is long longer part of Principal.
	@Deprecated
	private String name;
	private Date creationDate;
	private Boolean isIndividual = false;
	private String etag;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	@Deprecated
	public String getName() {
		return name;
	}
	@Deprecated
	public void setName(String name) {
		this.name = name;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public Boolean getIsIndividual() {
		return isIndividual;
	}
	public void setIsIndividual(Boolean isIndividual) {
		this.isIndividual = isIndividual;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isIndividual == null) ? 0 : isIndividual.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DBOUserGroupBackup other = (DBOUserGroupBackup) obj;
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
		if (isIndividual == null) {
			if (other.isIndividual != null)
				return false;
		} else if (!isIndividual.equals(other.isIndividual))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOUserGroupBackup [id=" + id + ", name=" + name
				+ ", creationDate=" + creationDate + ", isIndividual="
				+ isIndividual + ", etag=" + etag + "]";
	}
	
	
}
