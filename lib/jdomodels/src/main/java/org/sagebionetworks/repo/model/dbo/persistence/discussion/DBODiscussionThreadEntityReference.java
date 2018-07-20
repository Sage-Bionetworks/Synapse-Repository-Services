package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODiscussionThreadEntityReference implements MigratableDatabaseObject<DBODiscussionThreadEntityReference, DBODiscussionThreadEntityReference>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("threadId", COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID, true).withIsBackupId(true),
		new FieldColumn("entityId", COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID, true),
	};

	private Long threadId;
	private Long entityId;

	@Override
	public String toString() {
		return "DBODiscussionThreadEntityReference [threadId=" + threadId + ", entityId=" + entityId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((threadId == null) ? 0 : threadId.hashCode());
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
		DBODiscussionThreadEntityReference other = (DBODiscussionThreadEntityReference) obj;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (threadId == null) {
			if (other.threadId != null)
				return false;
		} else if (!threadId.equals(other.threadId))
			return false;
		return true;
	}

	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long threadId) {
		this.threadId = threadId;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	@Override
	public TableMapping<DBODiscussionThreadEntityReference> getTableMapping() {
		return new TableMapping<DBODiscussionThreadEntityReference>() {

			@Override
			public DBODiscussionThreadEntityReference mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBODiscussionThreadEntityReference dbo = new DBODiscussionThreadEntityReference();
				dbo.setThreadId(rs.getLong(COL_DISCUSSION_THREAD_ENTITY_REFERENCE_THREAD_ID));
				dbo.setEntityId(rs.getLong(COL_DISCUSSION_THREAD_ENTITY_REFERENCE_ENTITY_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_DISCUSSION_THREAD_ENTITY_REFERENCE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_DISCUSSION_THREAD_ENTITY_REFERENCE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBODiscussionThreadEntityReference> getDBOClass() {
				return DBODiscussionThreadEntityReference.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DISCUSSION_THREAD_ENTITY_REFERENCE;
	}

	@Override
	public MigratableTableTranslation<DBODiscussionThreadEntityReference, DBODiscussionThreadEntityReference> getTranslator() {
		return new BasicMigratableTableTranslation<DBODiscussionThreadEntityReference>();
	}

	@Override
	public Class<? extends DBODiscussionThreadEntityReference> getBackupClass() {
		return DBODiscussionThreadEntityReference.class;
	}

	@Override
	public Class<? extends DBODiscussionThreadEntityReference> getDatabaseObjectClass() {
		return DBODiscussionThreadEntityReference.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
