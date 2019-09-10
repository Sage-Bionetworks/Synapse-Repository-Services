package org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_ACTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_STATISTICS_MONTHLY_PROJECT_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * DBO object to store the monthly statistics for project files
 * 
 * @author Marco
 *
 */
public class DBOMonthlyStatisticsProjectFiles implements DatabaseObject<DBOMonthlyStatisticsProjectFiles> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("projectId", COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID, true),
			new FieldColumn("month", COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH, true),
			new FieldColumn("fileAction", COL_STATISTICS_MONTHLY_PROJECT_FILES_ACTION, true),
			new FieldColumn("filesCount", COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT),
			new FieldColumn("usersCount", COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT),
			new FieldColumn("lastUpdatedOn", COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON) };

	private static final TableMapping<DBOMonthlyStatisticsProjectFiles> TABLE_MAPPING = new TableMapping<DBOMonthlyStatisticsProjectFiles>() {

		@Override
		public DBOMonthlyStatisticsProjectFiles mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMonthlyStatisticsProjectFiles dbo = new DBOMonthlyStatisticsProjectFiles();

			dbo.setProjectId(rs.getLong(COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID));
			dbo.setMonth(rs.getObject(COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH, LocalDate.class));
			dbo.setFileAction(rs.getString(COL_STATISTICS_MONTHLY_PROJECT_FILES_ACTION));
			dbo.setFilesCount(rs.getInt(COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT));
			dbo.setUsersCount(rs.getInt(COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT));
			dbo.setLastUpdatedOn(rs.getLong(COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON));

			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_STATISTICS_MONTHLY_PROJECT_FILES;
		}

		@Override
		public String getDDLFileName() {
			return DDL_STATISTICS_MONTHLY_PROJECT_FILES;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOMonthlyStatisticsProjectFiles> getDBOClass() {
			return DBOMonthlyStatisticsProjectFiles.class;
		}

	};

	private Long projectId;
	private LocalDate month;
	private String fileAction;
	private Integer filesCount;
	private Integer usersCount;
	private Long lastUpdatedOn;

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public LocalDate getMonth() {
		return month;
	}

	public void setMonth(LocalDate month) {
		this.month = month;
	}

	public String getFileAction() {
		return fileAction;
	}

	public void setFileAction(String fileAction) {
		this.fileAction = fileAction;
	}

	public Integer getFilesCount() {
		return filesCount;
	}

	public void setFilesCount(Integer filesCount) {
		this.filesCount = filesCount;
	}

	public Integer getUsersCount() {
		return usersCount;
	}

	public void setUsersCount(Integer usersCount) {
		this.usersCount = usersCount;
	}

	public Long getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Long lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	@Override
	public TableMapping<DBOMonthlyStatisticsProjectFiles> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileAction, filesCount, lastUpdatedOn, month, projectId, usersCount);
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
		DBOMonthlyStatisticsProjectFiles other = (DBOMonthlyStatisticsProjectFiles) obj;
		return Objects.equals(fileAction, other.fileAction) && Objects.equals(filesCount, other.filesCount)
				&& Objects.equals(lastUpdatedOn, other.lastUpdatedOn) && Objects.equals(month, other.month)
				&& Objects.equals(projectId, other.projectId) && Objects.equals(usersCount, other.usersCount);
	}

	@Override
	public String toString() {
		return "DBOMonthlyStatisticsProjectFiles [projectId=" + projectId + ", month=" + month + ", fileAction=" + fileAction
				+ ", filesCount=" + filesCount + ", usersCount=" + usersCount + ", lastUpdatedOn=" + lastUpdatedOn + "]";
	}

}
