package org.sagebionetworks.repo.model.dbo.persistence.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_STATISTICS_MONTHLY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
public class DBOMonthlyStatisticsStatus implements DatabaseObject<DBOMonthlyStatisticsStatus> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("objectType", COL_STATISTICS_MONTHLY_OBJECT_TYPE, true),
			new FieldColumn("month", COL_STATISTICS_MONTHLY_MONTH, true),
			new FieldColumn("status", COL_STATISTICS_MONTHLY_STATUS) };

	private static final TableMapping<DBOMonthlyStatisticsStatus> TABLE_MAPPING = new TableMapping<DBOMonthlyStatisticsStatus>() {

		@Override
		public DBOMonthlyStatisticsStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMonthlyStatisticsStatus dbo = new DBOMonthlyStatisticsStatus();
			dbo.setObjectType(rs.getString(COL_STATISTICS_MONTHLY_OBJECT_TYPE));
			dbo.setMonth(rs.getObject(COL_STATISTICS_MONTHLY_MONTH, LocalDate.class));
			dbo.setStatus(rs.getString(COL_STATISTICS_MONTHLY_STATUS));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_STATISTICS_MONTHLY;
		}

		@Override
		public String getDDLFileName() {
			return DDL_STATISTICS_MONTHLY;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOMonthlyStatisticsStatus> getDBOClass() {
			return DBOMonthlyStatisticsStatus.class;
		}
	};

	private String objectType;
	private LocalDate month;
	private String status;

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
		this.month = month;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public TableMapping<DBOMonthlyStatisticsStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectType, status, month);
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
		DBOMonthlyStatisticsStatus other = (DBOMonthlyStatisticsStatus) obj;
		return Objects.equals(objectType, other.objectType) && Objects.equals(status, other.status)
				&& Objects.equals(month, other.month);
	}

	@Override
	public String toString() {
		return "DBOMonthlyStatisticsStatus [objectType=" + objectType + ", month=" + month + ", status=" + status + "]";
	}

}
