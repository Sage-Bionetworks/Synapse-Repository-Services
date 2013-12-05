package org.sagebionetworks.repo.model.dbo;

import java.util.Arrays;
import java.util.List;

@Table(name = "ANNOTATED_EXAMPLE_TEST")
public class DBOAnnotatedExample implements AutoIncrementDatabaseObject<DBOAnnotatedExample> {

	private static final TableMapping<DBOAnnotatedExample> tableMapping = AutoTableMapping.create(DBOAnnotatedExample.class, "CUSTOM");

	@Override
	public TableMapping<DBOAnnotatedExample> getTableMapping() {
		return tableMapping;
	}

	@Field(name = "ID", primary = true, nullable = false, sql = "AUTO_INCREMENT")
	private Long id;
	@Field(name = "NUMBER", nullable = false)
	private Long number;
	@Field(name = "NUMBER_OR_NULL", nullable = true)
	private Long numberOrNull;
	@Field(name = "BLOB_ONE", blob = "mediumblob")
	private byte[] blob;
	@Field(name = "CUSTOM", blob = "blob")
	private List<String> custom;
	@Field(name = "COMMENT", varchar = 256, defaultNull = true)
	private String comment;
	@Field(name = "NAME", fixedchar = 16, defaultNull = true)
	private String name;
	@Field(name = "ETAG", etag = true, defaultNull = true)
	private String etag;
	@Field(name = "MODIFIED_BY", varchar = 256, nullable = false)
	private String modifiedBy;
	@Field(name = "MODIFIED_ON", nullable = false)
	private Long modifiedOn;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public Long getNumberOrNull() {
		return numberOrNull;
	}

	public void setNumberOrNull(Long numberOrNull) {
		this.numberOrNull = numberOrNull;
	}

	public byte[] getBlob() {
		return blob;
	}

	public void setBlob(byte[] blob) {
		this.blob = blob;
	}

	public List<String> getCustom() {
		return custom;
	}

	public void setCustom(List<String> custom) {
		this.custom = custom;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(blob);
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + ((custom == null) ? 0 : custom.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		result = prime * result + ((numberOrNull == null) ? 0 : numberOrNull.hashCode());
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
		DBOAnnotatedExample other = (DBOAnnotatedExample) obj;
		if (!Arrays.equals(blob, other.blob))
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
			return false;
		if (custom == null) {
			if (other.custom != null)
				return false;
		} else if (!custom.equals(other.custom))
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		if (numberOrNull == null) {
			if (other.numberOrNull != null)
				return false;
		} else if (!numberOrNull.equals(other.numberOrNull))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOAnnotatedExample [id=" + id + ", number=" + number + ", numberOrNull=" + numberOrNull + ", blob=" + Arrays.toString(blob)
				+ ", custom=" + custom + ", comment=" + comment + ", name=" + name + ", etag=" + etag + ", modifiedBy=" + modifiedBy
				+ ", modifiedOn=" + modifiedOn + "]";
	}

}
