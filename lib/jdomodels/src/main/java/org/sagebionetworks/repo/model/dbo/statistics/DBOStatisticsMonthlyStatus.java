package org.sagebionetworks.repo.model.dbo.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_LAST_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_STATISTICS_MONTHLY_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_STATUS;
import static org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils.FIRST_DAY_OF_THE_MONTH;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * DBO object to store the status of the monthly statistics
 * 
 * @author Marco
 *
 */
public class DBOStatisticsMonthlyStatus implements DatabaseObject<DBOStatisticsMonthlyStatus> {
	
	public static int MAX_ERROR_MESSAGE_CHARS = 1000;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("objectType", COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE, true),
			new FieldColumn("month", COL_STATISTICS_MONTHLY_STATUS_MONTH, true),
			new FieldColumn("status", COL_STATISTICS_MONTHLY_STATUS_STATUS),
			new FieldColumn("lastStartedOn", COL_STATISTICS_MONTHLY_STATUS_LAST_STARTED_ON),
			new FieldColumn("lastUpdatedOn", COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON),
			new FieldColumn("errorMessage", COL_STATISTICS_MONTHLY_STATUS_ERROR_MESSAGE),
			new FieldColumn("errorDetails", COL_STATISTICS_MONTHLY_STATUS_ERROR_DETAILS) };

	private static final TableMapping<DBOStatisticsMonthlyStatus> TABLE_MAPPING = new TableMapping<DBOStatisticsMonthlyStatus>() {

		@Override
		public DBOStatisticsMonthlyStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOStatisticsMonthlyStatus dbo = new DBOStatisticsMonthlyStatus();

			dbo.setObjectType(rs.getString(COL_STATISTICS_MONTHLY_STATUS_OBJECT_TYPE));
			dbo.setMonth(rs.getObject(COL_STATISTICS_MONTHLY_STATUS_MONTH, LocalDate.class));
			dbo.setStatus(rs.getString(COL_STATISTICS_MONTHLY_STATUS_STATUS));

			Long lastStartedOn = rs.getLong(COL_STATISTICS_MONTHLY_STATUS_LAST_STARTED_ON);
			dbo.setLastStartedOn(rs.wasNull() ? null : lastStartedOn);

			Long lastUpdatedOn = rs.getLong(COL_STATISTICS_MONTHLY_STATUS_LAST_UPDATED_ON);
			dbo.setLastUpdatedOn(rs.wasNull() ? null : lastUpdatedOn);

			dbo.setErrorMessage(rs.getString(COL_STATISTICS_MONTHLY_STATUS_ERROR_MESSAGE));
			dbo.setErrorDetails(rs.getBytes(COL_STATISTICS_MONTHLY_STATUS_ERROR_DETAILS));

			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_STATISTICS_MONTHLY_STATUS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_STATISTICS_MONTHLY_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOStatisticsMonthlyStatus> getDBOClass() {
			return DBOStatisticsMonthlyStatus.class;
		}
	};

	private String objectType;
	private LocalDate month;
	private String status;
	private Long lastStartedOn;
	private Long lastUpdatedOn;
	private String errorMessage;
	private byte[] errorDetails;

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public LocalDate getMonth() {
		return month;
	}

	public void setMonth(LocalDate month) {
		// Make sure it's always set to the first day of the month
		this.month = month.withDayOfMonth(FIRST_DAY_OF_THE_MONTH);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getLastStartedOn() {
		return lastStartedOn;
	}

	public void setLastStartedOn(Long lastStartedOn) {
		this.lastStartedOn = lastStartedOn;
	}

	public Long getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Long lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public byte[] getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(byte[] errorDetails) {
		this.errorDetails = errorDetails;
	}

	@Override
	public TableMapping<DBOStatisticsMonthlyStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(errorDetails);
		result = prime * result + Objects.hash(errorMessage, lastStartedOn, lastUpdatedOn, month, objectType, status);
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
		DBOStatisticsMonthlyStatus other = (DBOStatisticsMonthlyStatus) obj;
		return Arrays.equals(errorDetails, other.errorDetails) && Objects.equals(errorMessage, other.errorMessage)
				&& Objects.equals(lastStartedOn, other.lastStartedOn) && Objects.equals(lastUpdatedOn, other.lastUpdatedOn)
				&& Objects.equals(month, other.month) && Objects.equals(objectType, other.objectType)
				&& Objects.equals(status, other.status);
	}

	@Override
	public String toString() {
		return "DBOMonthlyStatisticsStatus [objectType=" + objectType + ", month=" + month + ", status=" + status + ", lastStartedOn="
				+ lastStartedOn + ", lastUpdatedOn=" + lastUpdatedOn + ", errorMessage=" + errorMessage + ", errorDetails="
				+ Arrays.toString(errorDetails) + "]";
	}

}
