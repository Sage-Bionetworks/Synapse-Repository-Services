package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PARTICIPANT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * a participant data record
 */
@Table(name = SqlConstants.TABLE_PARTICIPANT_DATA)
public class DBOParticipantData implements MigratableDatabaseObject<DBOParticipantData, DBOParticipantData> {

	public static final String PARTICIPANT_DATA_DESCRIPTOR_ID_FIELD = "participantDataDescriptorId";
	public static final String PARTICIPANT_DATA_ID_FIELD = "participantDataId";

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_DESCRIPTOR_ID, backupId = true, primary = true)
	@ForeignKey(table = TABLE_PARTICIPANT_DATA_DESCRIPTOR, field = COL_PARTICIPANT_DATA_DESCRIPTOR_ID)
	private Long participantDataDescriptorId;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_ID, backupId = true, primary = true)
	@ForeignKey(table = TABLE_PARTICIPANT, field = COL_PARTICIPANT_ID, cascadeDelete = true)
	private Long participantDataId;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_BUCKET, varchar = 256, nullable = false)
	private String s3_bucket;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_KEY, varchar = 256, nullable = false)
	private String s3_key;

	private static TableMapping<DBOParticipantData> tableMapping = AutoTableMapping.create(DBOParticipantData.class);

	@Override
	public TableMapping<DBOParticipantData> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PARTICIPANT_DATA;
	}

	public Long getParticipantDataDescriptorId() {
		return participantDataDescriptorId;
	}

	public void setParticipantDataDescriptorId(Long participantDataDescriptorId) {
		this.participantDataDescriptorId = participantDataDescriptorId;
	}

	public Long getParticipantDataId() {
		return participantDataId;
	}

	public void setParticipantDataId(Long participantDataId) {
		this.participantDataId = participantDataId;
	}

	public String getS3_bucket() {
		return s3_bucket;
	}

	public void setS3_bucket(String s3_bucket) {
		this.s3_bucket = s3_bucket;
	}

	public String getS3_key() {
		return s3_key;
	}

	public void setS3_key(String s3_key) {
		this.s3_key = s3_key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((participantDataDescriptorId == null) ? 0 : participantDataDescriptorId.hashCode());
		result = prime * result + ((participantDataId == null) ? 0 : participantDataId.hashCode());
		result = prime * result + ((s3_bucket == null) ? 0 : s3_bucket.hashCode());
		result = prime * result + ((s3_key == null) ? 0 : s3_key.hashCode());
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
		DBOParticipantData other = (DBOParticipantData) obj;
		if (participantDataDescriptorId == null) {
			if (other.participantDataDescriptorId != null)
				return false;
		} else if (!participantDataDescriptorId.equals(other.participantDataDescriptorId))
			return false;
		if (participantDataId == null) {
			if (other.participantDataId != null)
				return false;
		} else if (!participantDataId.equals(other.participantDataId))
			return false;
		if (s3_bucket == null) {
			if (other.s3_bucket != null)
				return false;
		} else if (!s3_bucket.equals(other.s3_bucket))
			return false;
		if (s3_key == null) {
			if (other.s3_key != null)
				return false;
		} else if (!s3_key.equals(other.s3_key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOParticipantData [participantDataDescriptorId=" + participantDataDescriptorId + ", participantDataId=" + participantDataId
				+ ", s3_bucket=" + s3_bucket + ", s3_key=" + s3_key + "]";
	}

	@Override
	public MigratableTableTranslation<DBOParticipantData, DBOParticipantData> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOParticipantData, DBOParticipantData>() {

			@Override
			public DBOParticipantData createDatabaseObjectFromBackup(DBOParticipantData backup) {
				return backup;
			}

			@Override
			public DBOParticipantData createBackupFromDatabaseObject(DBOParticipantData dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOParticipantData> getBackupClass() {
		return DBOParticipantData.class;
	}

	@Override
	public Class<? extends DBOParticipantData> getDatabaseObjectClass() {
		return DBOParticipantData.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
