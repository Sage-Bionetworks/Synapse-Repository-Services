package org.sagebionetworks.evaluation.dbo;

import static org.sagebionetworks.repo.model.query.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DoubleAnnotationDBO implements DatabaseObject<DoubleAnnotationDBO>{
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_SUBSTATUS_ANNO_ID, true),
		new FieldColumn("ownerId", COL_SUBSTATUS_ANNO_SUBID),
		new FieldColumn("attribute", COL_SUBSTATUS_ANNO_ATTRIBUTE),
		new FieldColumn("value", COL_SUBSTATUS_ANNO_VALUE),
		new FieldColumn("isPrivate", COL_SUBSTATUS_ANNO_IS_PRIVATE)
		};
	
	@Override
	public TableMapping<DoubleAnnotationDBO> getTableMapping() {
		return new TableMapping<DoubleAnnotationDBO>(){
			@Override
			public DoubleAnnotationDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DoubleAnnotationDBO result = new DoubleAnnotationDBO();
				result.setId(rs.getLong(COL_SUBSTATUS_ANNO_ID));
				result.setOwnerId(rs.getLong(COL_SUBSTATUS_ANNO_SUBID));
				result.setAttribute(rs.getString(COL_SUBSTATUS_ANNO_ATTRIBUTE));
				result.setValue(rs.getDouble(COL_SUBSTATUS_ANNO_VALUE));
				result.setIsPrivate(rs.getBoolean(COL_SUBSTATUS_ANNO_IS_PRIVATE));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_SUBSTATUS_DOUBLEANNO;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_SUBSTATUS_DOUBLEANNO;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DoubleAnnotationDBO> getDBOClass() {
				return DoubleAnnotationDBO.class;
			}};
	}
	
	private Long Id;
	private Long ownerId;
	private String attribute;
	private Double value;
	private boolean isPrivate;

	public Long getId() {
		return Id;
	}
	public void setId(Long id) {
		Id = id;
	}
	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
	}	
	public boolean getIsPrivate() {
		return isPrivate;
	}
	public void setIsPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Id == null) ? 0 : Id.hashCode());
		result = prime * result
				+ ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result
				+ ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		DoubleAnnotationDBO other = (DoubleAnnotationDBO) obj;
		if (Id == null) {
			if (other.Id != null)
				return false;
		} else if (!Id.equals(other.Id))
			return false;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (isPrivate != other.isPrivate)
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SubmissionStatusDoubleAnnotationDBO [Id=" + Id
				+ ", submissionId=" + ownerId + ", attribute=" + attribute
				+ ", value=" + value + ", isPrivate=" + isPrivate + "]";
	}
}
