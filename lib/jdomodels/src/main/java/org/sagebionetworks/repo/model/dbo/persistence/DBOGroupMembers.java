package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and members of groups
 */
public class DBOGroupMembers implements MigratableDatabaseObject<DBOGroupMembers, DBOGroupMembers> {
	private Long groupId;
	private Long memberId;
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("groupId", COL_GROUP_MEMBERS_GROUP_ID, true).withIsBackupId(true),
		new FieldColumn("memberId", COL_GROUP_MEMBERS_MEMBER_ID, true)
	};
	
	@Override
	public TableMapping<DBOGroupMembers> getTableMapping() {
		return new TableMapping<DBOGroupMembers>() {

			@Override
			public DBOGroupMembers mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOGroupMembers change = new DBOGroupMembers();
				change.setGroupId(rs.getLong(COL_GROUP_MEMBERS_GROUP_ID));
				change.setMemberId(rs.getLong(COL_GROUP_MEMBERS_MEMBER_ID));
				return change;
			}

			@Override
			public String getTableName() {
				return TABLE_GROUP_MEMBERS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_GROUP_MEMBERS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOGroupMembers> getDBOClass() {
				return DBOGroupMembers.class;
			}
		};
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((memberId == null) ? 0 : memberId.hashCode());
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
		DBOGroupMembers other = (DBOGroupMembers) obj;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (memberId == null) {
			if (other.memberId != null)
				return false;
		} else if (!memberId.equals(other.memberId))
			return false;
		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GROUP_MEMBERS;
	}


	@Override
	public MigratableTableTranslation<DBOGroupMembers, DBOGroupMembers> getTranslator() {
		return new BasicMigratableTableTranslation<DBOGroupMembers>();
	}


	@Override
	public Class<? extends DBOGroupMembers> getBackupClass() {
		return DBOGroupMembers.class;
	}


	@Override
	public Class<? extends DBOGroupMembers> getDatabaseObjectClass() {
		return DBOGroupMembers.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}


}
