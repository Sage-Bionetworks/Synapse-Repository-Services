package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_SERIALIZED_OBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACTIVITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACTIVITY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * @author dburdick
 *
 */
public class DBOActivity implements DatabaseObject<DBOActivity>, ObservableEntity {
	private Long id;
	private String eTag;
	private byte[] serializedObject;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACTIVITY_ID, true),
		new FieldColumn("eTag", COL_ACTIVITY_ETAG),
		new FieldColumn("serializedObject", COL_ACTIVITY_SERIALIZED_OBJECT)
		};

	@Override
	public TableMapping<DBOActivity> getTableMapping() {
		return new TableMapping<DBOActivity>() {
			// Map a result set to this object
			@Override
			public DBOActivity mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOActivity ar = new DBOActivity();
				ar.setId(rs.getLong(COL_ACTIVITY_ID));
				ar.seteTag(rs.getString(COL_ACTIVITY_ETAG));
				java.sql.Blob blob = rs.getBlob(COL_ACTIVITY_SERIALIZED_OBJECT);
				if(blob != null){
					ar.setSerializedObject(blob.getBytes(1, (int) blob.length()));
				}
				return ar;
			}

			@Override
			public String getTableName() {
				return TABLE_ACTIVITY;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACTIVITY;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOActivity> getDBOClass() {
				return DBOActivity.class;
			}
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public byte[] getSerializedObject() {
		return serializedObject;
	}

	public void setSerializedObject(byte[] serializedObject) {
		this.serializedObject = serializedObject;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.ACTIVITY;
	}
	@Override
	public String getIdString() {
		return id.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(serializedObject);
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
		DBOActivity other = (DBOActivity) obj;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(serializedObject, other.serializedObject))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOActivity [id=" + id + ", eTag=" + eTag
				+ ", serializedObject=" + Arrays.toString(serializedObject)
				+ "]";
	}

}
