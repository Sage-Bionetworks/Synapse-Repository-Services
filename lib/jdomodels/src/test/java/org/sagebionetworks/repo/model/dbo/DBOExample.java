package org.sagebionetworks.repo.model.dbo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class DBOExample implements AutoIncrementDatabaseObject<DBOExample> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", "ID", true), 
			new FieldColumn("number", "NUMBER"),
			new FieldColumn("blob", "BLOB_ONE"),
			new FieldColumn("comment", "COMMENT"),
			new FieldColumn("modifiedBy", "MODIFIED_BY"),
			new FieldColumn("modifiedOn", "MODIFIED_ON"), };

	@Override
	public TableMapping<DBOExample> getTableMapping() {
		return new TableMapping<DBOExample>() {

			@Override
			public DBOExample mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOExample result = new DBOExample();
				result.setId(rs.getLong("ID"));
				result.setNumber(rs.getLong("NUMBER"));
				java.sql.Blob blob = rs.getBlob("BLOB_ONE");
				result.setBlob(blob.getBytes(1, (int) blob.length()));
				result.setComment(rs.getString("COMMENT"));
				result.setModifiedBy(rs.getString("MODIFIED_BY"));
				result.setModifiedOn(rs.getLong("MODIFIED_ON"));
				return result;
			}

			@Override
			public String getTableName() {
				return "EXAMPLE_TEST";
			}

			@Override
			public String getDDLFileName() {
				return "Example.sql";
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOExample> getDBOClass() {
				return DBOExample.class;
			}

		};
	}

	private Long id;
	private Long number;
	private byte[] blob;
	private String comment;
	private String modifiedBy;
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

	public byte[] getBlob() {
		return blob;
	}

	public void setBlob(byte[] blob) {
		this.blob = blob;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
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
		DBOExample other = (DBOExample) obj;
		if (!Arrays.equals(blob, other.blob))
			return false;
		if (comment == null) {
			if (other.comment != null)
				return false;
		} else if (!comment.equals(other.comment))
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
		if (number == null) {
			if (other.number != null)
				return false;
		} else if (!number.equals(other.number))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOExample [id=" + id + ", number=" + number + ", blob="
				+ Arrays.toString(blob) + ", comment=" + comment
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ "]";
	}

}
