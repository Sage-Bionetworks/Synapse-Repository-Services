package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_SETTING_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PROJECT_SETTING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_SETTING;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.TemporaryCode;

/**
 * descriptor of what's in a column of a participant data record
 */
public class DBOProjectSetting implements MigratableDatabaseObject<DBOProjectSetting, DBOProjectSetting> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_PROJECT_SETTING_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("projectId", COL_PROJECT_SETTING_PROJECT_ID),
			new FieldColumn("type", COL_PROJECT_SETTING_TYPE),
			new FieldColumn("etag", COL_PROJECT_SETTING_ETAG).withIsEtag(true),
			new FieldColumn("json", COL_PROJECT_SETTING_JSON),
			};

	private Long id;
	private Long projectId;
	private String type;
	private String etag;
	@TemporaryCode (author = "john", comment="Replaced by json and will be removed.")
	private ProjectSetting data;
	private String json;


	@Override
	public TableMapping<DBOProjectSetting> getTableMapping() {
		return new TableMapping<DBOProjectSetting>() {
			
			@Override
			public DBOProjectSetting mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOProjectSetting dbo = new DBOProjectSetting();
				dbo.setId(rs.getLong(COL_PROJECT_SETTING_ID));
				dbo.setProjectId(rs.getLong(COL_PROJECT_SETTING_PROJECT_ID));
				dbo.setType(rs.getString(COL_PROJECT_SETTING_TYPE));
				dbo.setEtag(rs.getString(COL_PROJECT_SETTING_ETAG));
				dbo.setJson(rs.getString(COL_PROJECT_SETTING_JSON));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_PROJECT_SETTING;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_PROJECT_SETTING;
			}
			
			@Override
			public Class<? extends DBOProjectSetting> getDBOClass() {
				return DBOProjectSetting.class;
			}
		};
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Deprecated
	public ProjectSetting getData() {
		return data;
	}

	@Deprecated
	public void setData(ProjectSetting data) {
		this.data = data;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	@Override
	public int hashCode() {
		return Objects.hash(data, etag, id, json, projectId, type);
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
		return Objects.equals(data, other.data) && Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(json, other.json) && Objects.equals(projectId, other.projectId)
				&& Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "DBOProjectSetting [id=" + id + ", projectId=" + projectId + ", type=" + type + ", etag=" + etag
				+ ", data=" + data + ", json=" + json + "]";
	}

	@Override
	public MigratableTableTranslation<DBOProjectSetting, DBOProjectSetting> getTranslator() {
		return new MigratableTableTranslation<DBOProjectSetting, DBOProjectSetting>() {
			
			@Override
			public DBOProjectSetting createDatabaseObjectFromBackup(DBOProjectSetting backup) {
				if(backup.getData() != null) {
					if(backup.getJson() != null) {
						throw new IllegalArgumentException("Both 'data' and 'json' have values");
					}
					try {
						backup.setJson(EntityFactory.createJSONStringForEntity(backup.getData()));
						backup.setData(null);
					} catch (JSONObjectAdapterException e) {
						throw new RuntimeException(e);
					}
				}
				return backup;
			}
			
			@Override
			public DBOProjectSetting createBackupFromDatabaseObject(DBOProjectSetting dbo) {
				return dbo;
			}
		};
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
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}
}
