package org.sagebionetworks.bridge.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PARTICIPANT_DATA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR;

import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
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
 * descriptor of what's in a column of a participant data record
 */
@Table(name = SqlConstants.TABLE_PARTICIPANT_DATA_COLUMN_DESCRIPTOR, constraints = { "unique key UNIQUE_PDCD_NAME ("
		+ COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PARTICIPANT_DATA_ID + ", "
		+ COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_NAME + ")" })
public class DBOParticipantDataColumnDescriptor implements MigratableDatabaseObject<DBOParticipantDataColumnDescriptor, DBOParticipantDataColumnDescriptor> {

	public static final String PARTICIPANT_DATA_COLUMN_DESCRIPTOR_ID_FIELD = "id";
	public static final String PARTICIPANT_DATA_DESCRIPTOR_ID_FIELD = "participantDataDescriptorId";

	@Field(name = COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_ID, backupId = true, primary = true, nullable = false)
	private Long id;

	@Field(name = COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PARTICIPANT_DATA_ID, varchar = 64, nullable = false)
	@ForeignKey(table = TABLE_PARTICIPANT_DATA_DESCRIPTOR, field = COL_PARTICIPANT_DATA_DESCRIPTOR_ID, cascadeDelete = true)
	private Long participantDataDescriptorId;
	
	@Field(name = COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_NAME, varchar = 256, nullable = false)
	private String name;

	@Field(name = COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PROPERTIES, serialized = "mediumblob")
	private ParticipantDataColumnDescriptor participantDataColumnDescriptor;

	private static TableMapping<DBOParticipantDataColumnDescriptor> tableMapping = AutoTableMapping.create(DBOParticipantDataColumnDescriptor.class);

	@Override
	public TableMapping<DBOParticipantDataColumnDescriptor> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PARTICIPANT_DATA_COLUMN_DESCRIPTOR;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getParticipantDataDescriptorId() {
		return participantDataDescriptorId;
	}

	public void setParticipantDataDescriptorId(Long participantDataDescriptorId) {
		this.participantDataDescriptorId = participantDataDescriptorId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParticipantDataColumnDescriptor getParticipantDataColumnDescriptor() {
		return participantDataColumnDescriptor;
	}

	public void setParticipantDataColumnDescriptor(ParticipantDataColumnDescriptor participantDataColumnDescriptor) {
		this.participantDataColumnDescriptor = participantDataColumnDescriptor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((participantDataColumnDescriptor == null) ? 0 : participantDataColumnDescriptor.hashCode());
		result = prime * result + ((participantDataDescriptorId == null) ? 0 : participantDataDescriptorId.hashCode());
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
		DBOParticipantDataColumnDescriptor other = (DBOParticipantDataColumnDescriptor) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (participantDataColumnDescriptor == null) {
			if (other.participantDataColumnDescriptor != null)
				return false;
		} else if (!participantDataColumnDescriptor.equals(other.participantDataColumnDescriptor))
			return false;
		if (participantDataDescriptorId == null) {
			if (other.participantDataDescriptorId != null)
				return false;
		} else if (!participantDataDescriptorId.equals(other.participantDataDescriptorId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOParticipantDataColumnDescriptor [id=" + id + ", participantDataDescriptorId="
				+ participantDataDescriptorId + ", name=" + name + ", participantDataColumnDescriptor="
				+ participantDataColumnDescriptor + "]";
	}

	@Override
	public MigratableTableTranslation<DBOParticipantDataColumnDescriptor, DBOParticipantDataColumnDescriptor> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOParticipantDataColumnDescriptor, DBOParticipantDataColumnDescriptor>() {

			@Override
			public DBOParticipantDataColumnDescriptor createDatabaseObjectFromBackup(DBOParticipantDataColumnDescriptor backup) {
				return backup;
			}

			@Override
			public DBOParticipantDataColumnDescriptor createBackupFromDatabaseObject(DBOParticipantDataColumnDescriptor dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOParticipantDataColumnDescriptor> getBackupClass() {
		return DBOParticipantDataColumnDescriptor.class;
	}

	@Override
	public Class<? extends DBOParticipantDataColumnDescriptor> getDatabaseObjectClass() {
		return DBOParticipantDataColumnDescriptor.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
