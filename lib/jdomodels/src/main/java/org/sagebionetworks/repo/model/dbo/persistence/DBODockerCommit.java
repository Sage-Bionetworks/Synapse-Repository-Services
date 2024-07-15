package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_DIGEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_TAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DOCKER_COMMIT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_COMMIT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBODockerCommit implements MigratableDatabaseObject<DBODockerCommit, DBODockerCommit> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("migrationId", COL_DOCKER_COMMIT_ID).withIsBackupId(true).withIsPrimaryKey(true),
			new FieldColumn("owner", COL_DOCKER_COMMIT_OWNER_ID),
			new FieldColumn("tag", COL_DOCKER_COMMIT_TAG),
			new FieldColumn("digest", COL_DOCKER_COMMIT_DIGEST),
			new FieldColumn("createdOn",COL_DOCKER_COMMIT_CREATED_ON)};

	private Long migrationId;
	private Long owner;
	private String tag;
	private String digest;
	private Long createdOn;

	@Override
	public TableMapping<DBODockerCommit> getTableMapping() {
		return new TableMapping<DBODockerCommit>() {
			
			@Override
			public DBODockerCommit mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBODockerCommit dbo = new DBODockerCommit();
				dbo.setMigrationId(rs.getLong(COL_DOCKER_COMMIT_ID));
				dbo.setOwner(rs.getLong(COL_DOCKER_COMMIT_OWNER_ID));
				dbo.setTag(rs.getString(COL_DOCKER_COMMIT_TAG));
				dbo.setCreatedOn(rs.getLong(COL_DOCKER_COMMIT_CREATED_ON));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_DOCKER_COMMIT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_DOCKER_COMMIT;
			}
			
			@Override
			public Class<? extends DBODockerCommit> getDBOClass() {
				return DBODockerCommit.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DOCKER_COMMIT;
	}

	@Override
	public MigratableTableTranslation<DBODockerCommit, DBODockerCommit> getTranslator() {
		return new BasicMigratableTableTranslation<DBODockerCommit>();
	}

	@Override
	public Class<? extends DBODockerCommit> getBackupClass() {
		return DBODockerCommit.class;
	}

	@Override
	public Class<? extends DBODockerCommit> getDatabaseObjectClass() {
		return DBODockerCommit.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getMigrationId() {
		return migrationId;
	}

	public void setMigrationId(Long migrationId) {
		this.migrationId = migrationId;
	}

	public Long getOwner() {
		return owner;
	}

	public void setOwner(Long owner) {
		this.owner = owner;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, digest, migrationId, owner, tag);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBODockerCommit other = (DBODockerCommit) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(digest, other.digest)
				&& Objects.equals(migrationId, other.migrationId) && Objects.equals(owner, other.owner)
				&& Objects.equals(tag, other.tag);
	}

	@Override
	public String toString() {
		return "DBODockerCommit [migrationId=" + migrationId + ", owner=" + owner + ", tag=" + tag + ", digest="
				+ digest + ", createdOn=" + createdOn + "]";
	}

}
