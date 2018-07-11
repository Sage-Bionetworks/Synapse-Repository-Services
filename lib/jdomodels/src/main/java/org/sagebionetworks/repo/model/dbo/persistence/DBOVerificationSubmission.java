package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_SERIALIZED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FK_VERIFICATION_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;

import java.util.Arrays;
import java.util.LinkedList;
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

@Table(name = TABLE_VERIFICATION_SUBMISSION, constraints={"UNIQUE UNIQUE_VERIFICATION_BY_ON ("+COL_VERIFICATION_SUBMISSION_CREATED_BY+", "+COL_VERIFICATION_SUBMISSION_CREATED_ON+")"})
public class DBOVerificationSubmission implements
		MigratableDatabaseObject<DBOVerificationSubmission, DBOVerificationSubmission> {

	@Field(name = COL_VERIFICATION_SUBMISSION_ID, backupId = true, primary = true, nullable = false)
	private Long id;
	
	@Field(name = COL_VERIFICATION_SUBMISSION_CREATED_BY, backupId = false, primary = false, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true, name = FK_VERIFICATION_USER_GROUP_ID)
	private Long createdBy;
	
	@Field(name = COL_VERIFICATION_SUBMISSION_CREATED_ON, backupId = false, primary = false, nullable = false)
	private Long createdOn;

	@Field(name = COL_VERIFICATION_SUBMISSION_SERIALIZED, backupId = false, primary = false, nullable = false, serialized="mediumblob")
	private byte[] serialized;

	private static TableMapping<DBOVerificationSubmission> TABLE_MAPPING = AutoTableMapping.create(DBOVerificationSubmission.class);

	@Override
	public TableMapping<DBOVerificationSubmission> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.VERIFICATION_SUBMISSION;
	}

	@Override
	public MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> getTranslator() {
		return new BasicMigratableTableTranslation<DBOVerificationSubmission>() ;
	}

	@Override
	public Class<? extends DBOVerificationSubmission> getBackupClass() {
		return DBOVerificationSubmission.class;
	}

	@Override
	public Class<? extends DBOVerificationSubmission> getDatabaseObjectClass() {
		return DBOVerificationSubmission.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOVerificationSubmissionFile());
		return list;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public byte[] getSerialized() {
		return serialized;
	}

	public void setSerialized(byte[] serialized) {
		this.serialized = serialized;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(serialized);
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
		DBOVerificationSubmission other = (DBOVerificationSubmission) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(serialized, other.serialized))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOVerificationSubmission [id=" + id + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", serialized="
				+ Arrays.toString(serialized) + "]";
	}
	
	

}
