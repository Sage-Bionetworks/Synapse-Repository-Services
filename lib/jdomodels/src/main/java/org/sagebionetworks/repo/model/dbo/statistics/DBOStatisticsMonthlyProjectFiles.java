package org.sagebionetworks.repo.model.dbo.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE;
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
public class DBOStatisticsMonthlyProjectFiles implements DatabaseObject<DBOStatisticsMonthlyProjectFiles> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("projectId", COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID, true),
			new FieldColumn("month", COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH, true),
			new FieldColumn("eventType", COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE, true),
			new FieldColumn("filesCount", COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT),
			new FieldColumn("usersCount", COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT),
			new FieldColumn("lastUpdatedOn", COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON) };

	private static final TableMapping<DBOStatisticsMonthlyProjectFiles> TABLE_MAPPING = new TableMapping<DBOStatisticsMonthlyProjectFiles>() {

		@Override
		public DBOStatisticsMonthlyProjectFiles mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOStatisticsMonthlyProjectFiles dbo = new DBOStatisticsMonthlyProjectFiles();

			dbo.setProjectId(rs.getLong(COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID));
			dbo.setMonth(rs.getObject(COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH, LocalDate.class));
			dbo.setEventType(rs.getString(COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE));
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
		public Class<? extends DBOStatisticsMonthlyProjectFiles> getDBOClass() {
			return DBOStatisticsMonthlyProjectFiles.class;
		}

	};

	private Long projectId;
	private LocalDate month;
	private String eventType;
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

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
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
	public TableMapping<DBOStatisticsMonthlyProjectFiles> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(eventType, filesCount, lastUpdatedOn, month, projectId, usersCount);
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
		DBOStatisticsMonthlyProjectFiles other = (DBOStatisticsMonthlyProjectFiles) obj;
		return Objects.equals(eventType, other.eventType) && Objects.equals(filesCount, other.filesCount)
				&& Objects.equals(lastUpdatedOn, other.lastUpdatedOn) && Objects.equals(month, other.month)
				&& Objects.equals(projectId, other.projectId) && Objects.equals(usersCount, other.usersCount);
	}

	@Override
	public String toString() {
		return "DBOMonthlyStatisticsProjectFiles [projectId=" + projectId + ", month=" + month + ", eventType=" + eventType
				+ ", filesCount=" + filesCount + ", usersCount=" + usersCount + ", lastUpdatedOn=" + lastUpdatedOn + "]";
	}

}
