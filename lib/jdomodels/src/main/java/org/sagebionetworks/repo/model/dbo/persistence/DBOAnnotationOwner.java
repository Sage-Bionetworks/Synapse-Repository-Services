package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATION_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_ANNOTATIONS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ANNOTATIONS_OWNER;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * This table provides a single point of entry for all of the Annotations tables.  It was added as
 * part of the fix for PLFM-1696.  All annotations for an owner are removed by deleting the 
 * annotations owner from this table triggering a cascaded delete for all other tables.
 * If two threads attempt to update the annotations of the same owner at the same time
 * the database will throw primary key constraint violation.
 * 
 * @author John
 *
 */
public class DBOAnnotationOwner implements DatabaseObject<DBOAnnotationOwner>{
	
	Long ownerId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_ANNOTATION_OWNER, true),
		};

	@Override
	public TableMapping<DBOAnnotationOwner> getTableMapping() {
		return new TableMapping<DBOAnnotationOwner>() {
			
			@Override
			public DBOAnnotationOwner mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOAnnotationOwner result = new DBOAnnotationOwner();
				result.setOwnerId(rs.getLong(COL_ANNOTATION_OWNER));
				return result;
			}
			
			@Override
			public String getTableName() {
				return TABLE_ANNOTATIONS_OWNER;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_ANNOTATIONS_OWNER;
			}
			
			@Override
			public Class<? extends DBOAnnotationOwner> getDBOClass() {
				return DBOAnnotationOwner.class;
			}
		};
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
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
		DBOAnnotationOwner other = (DBOAnnotationOwner) obj;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOAnnotationOwner [ownerId=" + ownerId + "]";
	}

}
