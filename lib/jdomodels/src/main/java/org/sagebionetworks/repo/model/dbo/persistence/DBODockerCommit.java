package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_DIGEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_COMMIT_TAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_COMMIT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_DOCKER_COMMIT, constraints = { 
		"unique key UNIQUE_DOCKER_COMMIT ("+ COL_DOCKER_COMMIT_OWNER_ID + 
		"," + COL_DOCKER_COMMIT_TAG +"," + COL_DOCKER_COMMIT_CREATED_ON +")" })
public class DBODockerCommit implements MigratableDatabaseObject<DBODockerCommit, DBODockerCommit> {

	@Field(name = COL_DOCKER_COMMIT_ID, backupId = true, primary = true, nullable = false)
	private Long migrationId;
	
	@Field(name = COL_DOCKER_COMMIT_OWNER_ID, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_NODE, field = COL_NODE_ID, cascadeDelete = true, name = "DOCKER_COMMIT_OWNER_FK")
	private Long owner;
	
	// max length is 128 https://docs.docker.com/engine/reference/commandline/tag/
	@Field(name = COL_DOCKER_COMMIT_TAG, varchar = 128, backupId = false, primary = false, nullable = false)
	private String tag;

	// a sha256 digest uses 71 characters, but the Docker spec' is open ended, 
	// potentially allowing other kinds of digests
	@Field(name = COL_DOCKER_COMMIT_DIGEST, varchar = 200, backupId = false, primary = false, nullable = false)
	private String digest;
	
	@Field(name = COL_DOCKER_COMMIT_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	private static TableMapping<DBODockerCommit> TABLE_MAPPING = 
			AutoTableMapping.create(DBODockerCommit.class);
	
	@Override
	public TableMapping<DBODockerCommit> getTableMapping() {
		return TABLE_MAPPING;
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
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((digest == null) ? 0 : digest.hashCode());
		result = prime * result
				+ ((migrationId == null) ? 0 : migrationId.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
		DBODockerCommit other = (DBODockerCommit) obj;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (digest == null) {
			if (other.digest != null)
				return false;
		} else if (!digest.equals(other.digest))
			return false;
		if (migrationId == null) {
			if (other.migrationId != null)
				return false;
		} else if (!migrationId.equals(other.migrationId))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBODockerCommit [migrationId=" + migrationId + ", owner="
				+ owner + ", tag=" + tag + ", digest=" + digest
				+ ", createdOn=" + createdOn + "]";
	}
	
	

}
