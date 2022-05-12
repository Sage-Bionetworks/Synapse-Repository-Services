package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_PROJECT_AR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * A dynamic (no migration) index built by a worker that maps access requirements to projects. 
 *
 */
public class DBOAccessRequirementProject implements DatabaseObject<DBOAccessRequirementProject> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("accessRequirementId", COL_ACCESS_REQUIREMENT_PROJECT_AR_ID, true),
			new FieldColumn("projectId", COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID, true) };
	
	TableMapping<DBOAccessRequirementProject> MAPPING = new TableMapping<DBOAccessRequirementProject>() {

		@Override
		public DBOAccessRequirementProject mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAccessRequirementProject dbo = new DBOAccessRequirementProject();
			dbo.setAccessRequirementId(rs.getLong(COL_ACCESS_REQUIREMENT_PROJECT_AR_ID));
			dbo.setProjectId(rs.getLong(COL_ACCESS_REQUIREMENT_PROJECT_PROJECT_ID));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_ACCESS_REQUIREMENT_PROJECTS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.COL_ACCESS_REQUIREMENT_PROJECT_SCHEMA;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOAccessRequirementProject> getDBOClass() {
			return DBOAccessRequirementProject.class;
		}
	};

	@Override
	public TableMapping<DBOAccessRequirementProject> getTableMapping() {
		return MAPPING;
	}

	private Long accessRequirementId;
	private Long projectId;

	/**
	 * @return the accessRequirementId
	 */
	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	/**
	 * @param accessRequirementId the accessRequirementId to set
	 */
	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	/**
	 * @return the projectId
	 */
	public Long getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRequirementId, projectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOAccessRequirementProject)) {
			return false;
		}
		DBOAccessRequirementProject other = (DBOAccessRequirementProject) obj;
		return Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(projectId, other.projectId);
	}

	@Override
	public String toString() {
		return "DBOAccessRequirementProject [accessRequirementId=" + accessRequirementId + ", projectId=" + projectId
				+ "]";
	}

}
