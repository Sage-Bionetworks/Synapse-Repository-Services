package org.sagebionetworks.bridge.model.dbo.persistence;

import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * descriptor of what's in a participant data record
 */
@Table(name = SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR, constraints = { "unique key UNIQUE_DSM_NAME ("
		+ SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_NAME + ")" })
public class DBOParticipantDataDescriptor implements MigratableDatabaseObject<DBOParticipantDataDescriptor, DBOParticipantDataDescriptor> {

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_ID, backupId = true, primary = true)
	private Long id;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_NAME, varchar = 64, nullable = false)
	private String name;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_DESCRIPTION, type = "text")
	private String description;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_REPEAT_TYPE, nullable = false)
	private ParticipantDataRepeatType repeatType;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_FREQUENCY, varchar = 64, nullable = true)
	private String repeatFrequency;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_DATETIME_START_COLUMN_NAME, varchar = 64, nullable = true)
	private String datetimeStartColumnName;

	@Field(name = SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_DATETIME_END_COLUMN_NAME, varchar = 64, nullable = true)
	private String datetimeEndColumnName;

	private static TableMapping<DBOParticipantDataDescriptor> tableMapping = AutoTableMapping.create(DBOParticipantDataDescriptor.class);

	@Override
	public TableMapping<DBOParticipantDataDescriptor> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PARTICIPANT_DATA_DESCRIPTOR;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ParticipantDataRepeatType getRepeatType() {
		return repeatType;
	}

	public void setRepeatType(ParticipantDataRepeatType repeatType) {
		this.repeatType = repeatType;
	}

	public String getRepeatFrequency() {
		return repeatFrequency;
	}

	public void setRepeatFrequency(String repeatFrequency) {
		this.repeatFrequency = repeatFrequency;
	}

	public String getDatetimeStartColumnName() {
		return datetimeStartColumnName;
	}

	public void setDatetimeStartColumnName(String datetimeStartColumnName) {
		this.datetimeStartColumnName = datetimeStartColumnName;
	}

	public String getDatetimeEndColumnName() {
		return datetimeEndColumnName;
	}

	public void setDatetimeEndColumnName(String datetimeEndColumnName) {
		this.datetimeEndColumnName = datetimeEndColumnName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((datetimeEndColumnName == null) ? 0 : datetimeEndColumnName.hashCode());
		result = prime * result + ((datetimeStartColumnName == null) ? 0 : datetimeStartColumnName.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((repeatFrequency == null) ? 0 : repeatFrequency.hashCode());
		result = prime * result + ((repeatType == null) ? 0 : repeatType.hashCode());
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
		DBOParticipantDataDescriptor other = (DBOParticipantDataDescriptor) obj;
		if (datetimeEndColumnName == null) {
			if (other.datetimeEndColumnName != null)
				return false;
		} else if (!datetimeEndColumnName.equals(other.datetimeEndColumnName))
			return false;
		if (datetimeStartColumnName == null) {
			if (other.datetimeStartColumnName != null)
				return false;
		} else if (!datetimeStartColumnName.equals(other.datetimeStartColumnName))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
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
		if (repeatFrequency == null) {
			if (other.repeatFrequency != null)
				return false;
		} else if (!repeatFrequency.equals(other.repeatFrequency))
			return false;
		if (repeatType != other.repeatType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOParticipantDataDescriptor [id=" + id + ", name=" + name + ", description=" + description + ", repeatType=" + repeatType
				+ ", repeatFrequency=" + repeatFrequency + ", datetimeStartColumnName=" + datetimeStartColumnName
				+ ", datetimeEndColumnName=" + datetimeEndColumnName + "]";
	}

	@Override
	public MigratableTableTranslation<DBOParticipantDataDescriptor, DBOParticipantDataDescriptor> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOParticipantDataDescriptor, DBOParticipantDataDescriptor>() {

			@Override
			public DBOParticipantDataDescriptor createDatabaseObjectFromBackup(DBOParticipantDataDescriptor backup) {
				return backup;
			}

			@Override
			public DBOParticipantDataDescriptor createBackupFromDatabaseObject(DBOParticipantDataDescriptor dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOParticipantDataDescriptor> getBackupClass() {
		return DBOParticipantDataDescriptor.class;
	}

	@Override
	public Class<? extends DBOParticipantDataDescriptor> getDatabaseObjectClass() {
		return DBOParticipantDataDescriptor.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
