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
	public String toString() {
		return "DBOResearchProject [id=" + id + ", accessRequirementId=" + accessRequirementId + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", etag=" + etag + ", projectLead=" + projectLead + ", institution=" + institution + ", idu="
				+ Arrays.toString(idu) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(idu);
		result = prime * result + ((institution == null) ? 0 : institution.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((projectLead == null) ? 0 : projectLead.hashCode());
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
		DBOResearchProject other = (DBOResearchProject) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
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
		if (!Arrays.equals(idu, other.idu))
			return false;
		if (institution == null) {
			if (other.institution != null)
				return false;
		} else if (!institution.equals(other.institution))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (projectLead == null) {
			if (other.projectLead != null)
				return false;
		} else if (!projectLead.equals(other.projectLead))
			return false;
		return true;
	}

	@Override
	public TableMapping<DBOResearchProject> getTableMapping() {
		return new TableMapping<DBOResearchProject>(){

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
				dbo.setIdu(blob.getBytes(1, (int) blob.length()));
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
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.RESEARCH_PROJECT;
	}

	@Override
	public MigratableTableTranslation<DBOResearchProject, DBOResearchProject> getTranslator() {
		return new BasicMigratableTableTranslation<DBOResearchProject>();
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
}
