package org.sagebionetworks.repo.model.dbo.schema;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DERIVED_ANNOTATIONS_ANNOS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DERIVED_ANNOTATIONS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DERIVED_ANNOTATIONS_KEYS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_DERIVED_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DERIVED_ANNOTATIONS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * A DBO for storing derived annotations. Note: Derived annotations do not
 * migrate, as they are dynamically created for each stack.
 *
 */
public class DBODerivedAnnotations implements DatabaseObject<DBODerivedAnnotations> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("objectId", COL_DERIVED_ANNOTATIONS_ID, true),
			new FieldColumn("keys", COL_DERIVED_ANNOTATIONS_KEYS),
			new FieldColumn("annotations", COL_DERIVED_ANNOTATIONS_ANNOS),
	};
	
	private Long objectId;
	private String keys;
	private String annotations;

	@Override
	public TableMapping<DBODerivedAnnotations> getTableMapping() {
		return new  TableMapping<DBODerivedAnnotations>() {

			@Override
			public DBODerivedAnnotations mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODerivedAnnotations result = new DBODerivedAnnotations();
				result.setObjectId(rs.getLong(COL_DERIVED_ANNOTATIONS_ID));
				result.setKeys(rs.getString(COL_DERIVED_ANNOTATIONS_KEYS));
				result.setAnnotations(rs.getString(COL_DERIVED_ANNOTATIONS_ANNOS));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_DERIVED_ANNOTATIONS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_DERIVED_ANNOTATIONS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODerivedAnnotations> getDBOClass() {
				return DBODerivedAnnotations.class;
			}};
	}
	
	

	/**
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}



	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}



	/**
	 * @return the keys
	 */
	public String getKeys() {
		return keys;
	}



	/**
	 * @param keys the keys to set
	 */
	public void setKeys(String keys) {
		this.keys = keys;
	}



	/**
	 * @return the annotations
	 */
	public String getAnnotations() {
		return annotations;
	}



	/**
	 * @param annotations the annotations to set
	 */
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}


	@Override
	public int hashCode() {
		return Objects.hash(annotations, keys, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBODerivedAnnotations)) {
			return false;
		}
		DBODerivedAnnotations other = (DBODerivedAnnotations) obj;
		return Objects.equals(annotations, other.annotations) && Objects.equals(keys, other.keys)
				&& Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "DBODerivedAnnotations [objectId=" + objectId + ", keys=" + keys + ", annotations=" + annotations + "]";
	}

}
