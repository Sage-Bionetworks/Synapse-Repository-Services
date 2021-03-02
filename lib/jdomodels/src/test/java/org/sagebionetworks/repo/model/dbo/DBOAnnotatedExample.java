package org.sagebionetworks.repo.model.dbo;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Table(name = "ANNOTATED_EXAMPLE_TEST")
public class DBOAnnotatedExample implements AutoIncrementDatabaseObject<DBOAnnotatedExample> {

	public enum ExampleEnum {
		aaa, bbb, ccc
	}

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
	@Field(name = "SERIALIZED", serialized = "blob")
	private List<String> serialized;
	@Field(name = "COMMENT", varchar = 256, defaultNull = true)
	private String comment;
	@Field(name = "ENUM")
	private ExampleEnum exampleEnum;
	@Field(name = "NAME", fixedchar = 16, defaultNull = true)
	private String name;
	@Field(name = "ETAG", etag = true, defaultNull = true)
	private String etag;
	@Field(name = "MODIFIED_BY", varchar = 256, nullable = false)
	private String modifiedBy;
	@Field(name = "MODIFIED_ON", nullable = false)
	private Date modifiedOn;
	@Field(name = "PARENT_ID", isSelfForeignKey = true)
	@ForeignKey(name = "PARENT_FK", table = "ANNOTATED_EXAMPLE_TEST", field = "ID", cascadeDelete = true)
	private Long parentId;
	@Field(name ="FILE_HANDLE", hasFileHandleRef = true)
	private Long fileHandleId;

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

	public List<String> getSerialized() {
		return serialized;
	}

	public void setSerialized(List<String> serialized) {
		this.serialized = serialized;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public ExampleEnum getExampleEnum() {
		return exampleEnum;
	}

	public void setExampleEnum(ExampleEnum exampleEnum) {
		this.exampleEnum = exampleEnum;
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

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	
	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	
	public Long getFileHandleId() {
		return fileHandleId;
	}
	
	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(blob);
		result = prime * result + Objects.hash(comment, custom, etag, exampleEnum, fileHandleId, id, modifiedBy, modifiedOn, name, number,
				numberOrNull, parentId, serialized);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOAnnotatedExample other = (DBOAnnotatedExample) obj;
		return Arrays.equals(blob, other.blob) && Objects.equals(comment, other.comment) && Objects.equals(custom, other.custom)
				&& Objects.equals(etag, other.etag) && exampleEnum == other.exampleEnum && Objects.equals(fileHandleId, other.fileHandleId)
				&& Objects.equals(id, other.id) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(name, other.name) && Objects.equals(number, other.number)
				&& Objects.equals(numberOrNull, other.numberOrNull) && Objects.equals(parentId, other.parentId)
				&& Objects.equals(serialized, other.serialized);
	}

	@Override
	public String toString() {
		return "DBOAnnotatedExample [id=" + id + ", number=" + number + ", numberOrNull=" + numberOrNull + ", blob=" + Arrays.toString(blob)
				+ ", custom=" + custom + ", serialized=" + serialized + ", comment=" + comment + ", exampleEnum=" + exampleEnum + ", name="
				+ name + ", etag=" + etag + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", parentId=" + parentId
				+ ", fileHandleId=" + fileHandleId + "]";
	}

}
