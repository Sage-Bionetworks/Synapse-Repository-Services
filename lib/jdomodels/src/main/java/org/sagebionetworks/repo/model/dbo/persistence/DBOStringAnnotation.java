package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;


import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;


public class DBOStringAnnotation implements DatabaseObject<DBOStringAnnotation>{
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ID, true),
		new FieldColumn("owner", ANNOTATION_OWNER_ID_COLUMN),
		new FieldColumn("attribute", ANNOTATION_ATTRIBUTE_COLUMN),
		new FieldColumn("value", ANNOTATION_VALUE_COLUMN),
		};
	
	@Override
	public TableMapping<DBOStringAnnotation> getTableMapping() {
		return new TableMapping<DBOStringAnnotation>(){
			@Override
			public DBOStringAnnotation mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOStringAnnotation result = new DBOStringAnnotation();
				result.setId(rs.getLong(COL_ID));
				result.setOwner(rs.getLong(ANNOTATION_OWNER_ID_COLUMN));
				result.setAttribute(rs.getString(ANNOTATION_ATTRIBUTE_COLUMN));
				result.setValue(rs.getString(ANNOTATION_VALUE_COLUMN));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_STRING_ANNOTATIONS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_STRING_ANNOTATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOStringAnnotation> getDBOClass() {
				return DBOStringAnnotation.class;
			}};
	}
	
	private Long id;
	private Long owner;
	private String attribute;
	private String value;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		DBOStringAnnotation other = (DBOStringAnnotation) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
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
		return "DBOStringAnnotation [id=" + id + ", owner=" + owner
				+ ", attribute=" + attribute + ", value=" + value + "]";
	}

}
