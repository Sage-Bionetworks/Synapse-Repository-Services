package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_IDU;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_INSTITUTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESEARCH_PROJECT_PROJECT_LEAD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_RESEARCH_PROJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESEARCH_PROJECT;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOResearchProject implements MigratableDatabaseObject<DBOResearchProject, DBOResearchProject>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_RESEARCH_PROJECT_ID, true).withIsBackupId(true),
			new FieldColumn("accessRequirementId", COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID),
			new FieldColumn("createdBy", COL_RESEARCH_PROJECT_CREATED_BY),
			new FieldColumn("createdOn", COL_RESEARCH_PROJECT_CREATED_ON),
			new FieldColumn("modifiedBy", COL_RESEARCH_PROJECT_MODIFIED_BY),
			new FieldColumn("modifiedOn", COL_RESEARCH_PROJECT_MODIFIED_ON),
			new FieldColumn("etag", COL_RESEARCH_PROJECT_ETAG).withIsEtag(true),
			new FieldColumn("projectLead", COL_RESEARCH_PROJECT_PROJECT_LEAD),
			new FieldColumn("institution", COL_RESEARCH_PROJECT_INSTITUTION),
			new FieldColumn("idu", COL_RESEARCH_PROJECT_IDU)
		};

	private static final TableMapping<DBOResearchProject> TABLE_MAPPER = new TableMapping<DBOResearchProject>() {

			@Override
			public DBOResearchProject mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOResearchProject dbo = new DBOResearchProject();
				dbo.setId(rs.getLong(COL_RESEARCH_PROJECT_ID));
				dbo.setAccessRequirementId(rs.getLong(COL_RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID));
				dbo.setCreatedBy(rs.getLong(COL_RESEARCH_PROJECT_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(COL_RESEARCH_PROJECT_CREATED_ON));
				dbo.setModifiedBy(rs.getLong(COL_RESEARCH_PROJECT_MODIFIED_BY));
				dbo.setModifiedOn(rs.getLong(COL_RESEARCH_PROJECT_MODIFIED_ON));
				dbo.setEtag(rs.getString(COL_RESEARCH_PROJECT_ETAG));
				dbo.setProjectLead(rs.getString(COL_RESEARCH_PROJECT_PROJECT_LEAD));
				dbo.setInstitution(rs.getString(COL_RESEARCH_PROJECT_INSTITUTION));
				
				Blob blob = rs.getBlob(COL_RESEARCH_PROJECT_IDU);
				
				if (blob != null) {
					dbo.setIdu(blob.getBytes(1, (int) blob.length()));
				}

				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_RESEARCH_PROJECT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_RESEARCH_PROJECT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOResearchProject> getDBOClass() {
				return DBOResearchProject.class;
			}

		};

	private static final MigratableTableTranslation<DBOResearchProject, DBOResearchProject> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<DBOResearchProject>();

	private Long id;
	private Long accessRequirementId;
	private Long createdBy;
	private Long createdOn;
	private Long modifiedBy;
	private Long modifiedOn;
	private String etag;
	private String projectLead;
	private String institution;
	private byte[] idu;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
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

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getProjectLead() {
		return projectLead;
	}

	public void setProjectLead(String projectLead) {
		this.projectLead = projectLead;
	}

	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public byte[] getIdu() {
		return idu;
	}

	public void setIdu(byte[] idu) {
		this.idu = idu;
	}

	@Override
	public TableMapping<DBOResearchProject> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.RESEARCH_PROJECT;
	}

	@Override
	public MigratableTableTranslation<DBOResearchProject, DBOResearchProject> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOResearchProject> getBackupClass() {
		return DBOResearchProject.class;
	}

	@Override
	public Class<? extends DBOResearchProject> getDatabaseObjectClass() {
		return DBOResearchProject.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(idu);
		result = prime * result
				+ Objects.hash(accessRequirementId, createdBy, createdOn, etag, id, institution, modifiedBy, modifiedOn, projectLead);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOResearchProject other = (DBOResearchProject) obj;
		return Objects.equals(accessRequirementId, other.accessRequirementId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Arrays.equals(idu, other.idu) && Objects.equals(institution, other.institution)
				&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(projectLead, other.projectLead);
	}

	@Override
	public String toString() {
		return "DBOResearchProject [id=" + id + ", accessRequirementId=" + accessRequirementId + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", etag=" + etag + ", projectLead="
				+ projectLead + ", institution=" + institution + ", idu=" + Arrays.toString(idu) + "]";
	}
	
}
