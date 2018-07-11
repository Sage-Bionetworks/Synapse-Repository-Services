package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_SETTING;

import java.util.List;

import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * descriptor of what's in a column of a participant data record
 */
@Table(name = TABLE_PROJECT_SETTING, constraints = { "unique key UNIQUE_PSID_TYPE (" + COL_PROJECT_SETTING_PROJECT_ID + ", "
		+ COL_PROJECT_SETTING_TYPE + ")" })
public class DBOProjectSetting implements MigratableDatabaseObject<DBOProjectSetting, DBOProjectSetting> {

	@Field(name = COL_PROJECT_SETTING_ID, backupId = true, primary = true, nullable = false)
	private Long id;

	@Field(name = COL_PROJECT_SETTING_PROJECT_ID, nullable = false)
	@ForeignKey(table = TABLE_NODE, field = COL_NODE_ID, cascadeDelete = true)
	private Long projectId;

	@Field(name = COL_PROJECT_SETTING_TYPE, nullable = false, varchar = 256)
	private ProjectSettingsType type;

	@Field(name = COL_PROJECT_SETTING_ETAG, etag = true, nullable = false)
	private String etag;

	@Field(name = COL_PROJECT_SETTING_DATA, serialized = "mediumblob")
	private ProjectSetting data;

	private static TableMapping<DBOProjectSetting> tableMapping = AutoTableMapping.create(DBOProjectSetting.class);

	@Override
	public TableMapping<DBOProjectSetting> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PROJECT_SETTINGS;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public ProjectSettingsType getType() {
		return type;
	}

	public void setType(ProjectSettingsType type) {
		this.type = type;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public ProjectSetting getData() {
		return data;
	}

	public void setData(ProjectSetting data) {
		this.data = data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DBOProjectSetting other = (DBOProjectSetting) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOProjectSettings [id=" + id + ", projectId=" + projectId + ", type=" + type + ", etag=" + etag + ", data=" + data + "]";
	}

	@Override
	public MigratableTableTranslation<DBOProjectSetting, DBOProjectSetting> getTranslator() {
		return new BasicMigratableTableTranslation<DBOProjectSetting>();
	}

	@Override
	public Class<? extends DBOProjectSetting> getBackupClass() {
		return DBOProjectSetting.class;
	}

	@Override
	public Class<? extends DBOProjectSetting> getDatabaseObjectClass() {
		return DBOProjectSetting.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
