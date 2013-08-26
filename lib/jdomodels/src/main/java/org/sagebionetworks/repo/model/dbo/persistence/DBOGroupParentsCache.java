package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOGroupParentsCache implements DatabaseObject<DBOGroupParentsCache> {

	private Long groupId;
	private byte[] parents;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("groupId", COL_GROUP_PARENTS_CACHE_GROUP_ID, true),
		new FieldColumn("parents", COL_GROUP_PARENTS_CACHE_PARENTS)
		};
	
	@Override
	public TableMapping<DBOGroupParentsCache> getTableMapping() {
		return new TableMapping<DBOGroupParentsCache>(){

			@Override
			public DBOGroupParentsCache mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGroupParentsCache dbo = new DBOGroupParentsCache();
				dbo.setGroupId(rs.getLong(COL_GROUP_PARENTS_CACHE_GROUP_ID));
				Blob parents = rs.getBlob(COL_GROUP_PARENTS_CACHE_PARENTS);
				if (parents != null) {
					dbo.setParents(parents.getBytes(1, (int) parents.length()));
				}
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GROUP_PARENTS_CACHE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_GROUP_PARENTS_CACHE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOGroupParentsCache> getDBOClass() {
				return DBOGroupParentsCache.class;
			}};
	}
	
	public long getGroupId() {
		return groupId;
	}
	
	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}
	
	public byte[] getParents() {
		return parents;
	}
	
	public void setParents(byte[] parents) {
		this.parents = parents;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + groupId.hashCode();
		result = prime * result + Arrays.hashCode(parents);
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
		DBOGroupParentsCache other = (DBOGroupParentsCache) obj;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (parents == null) {
			if (other.parents != null)
				return false;
		} else if (!parents.equals(other.parents))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOGroupParentsCache [groupId=" + groupId + ", parents=" + parents + "]";
	}

}
