package org.sagebionetworks.repo.model.athena.project;

import java.time.YearMonth;
import java.util.List;

import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.model.Database;

@Repository
public class AthenaProjectFileStatisticsDAOImpl implements AthenaProjectFileStatisticsDAO {

	static final String DATABASE_NAME = "firehoseLogs";

	private static final String COL_YEAR = "year";
	private static final String COL_MONTH = "month";
	private static final String COL_PROJECT_ID = "projectId";
	private static final String COL_USER_ID = "userId";

	// @formatter:off

	private static final String SQL_AGGREGATE_TEMPLATE = 
			"SELECT " + COL_PROJECT_ID + " AS PROJECT_ID, "
			+ "COUNT(" + COL_PROJECT_ID + ") AS FILES_COUNT, "
			+ "COUNT(DISTINCT " + COL_USER_ID + ") AS USERS_COUNT "
			+ "FROM %1$s WHERE " 
			+ COL_YEAR + "='%2$s' AND "
			// Note: The year, month and day partition key types are defined in glue as string, 
			// makes sure that we have the 0 padded version of the month
			+ COL_MONTH + "='%3$02d' "
			+ "GROUP BY " + COL_PROJECT_ID;
	
	// @formatter:on

	private AthenaSupport athenaSupport;
	private FileEventTableNameProvider tableNameProvider;

	@Autowired
	public AthenaProjectFileStatisticsDAOImpl(AthenaSupport athenaSupport, FileEventTableNameProvider tableNameProvider) {
		this.athenaSupport = athenaSupport;
		this.tableNameProvider = tableNameProvider;
	}

	@Override
	public AthenaQueryResult<StatisticsMonthlyProjectFiles> aggregateForMonth(FileEvent eventType, YearMonth month) {
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(month, "month");

		Database database = getDatabase();
		String query = getAggregateQuery(eventType, month);

		return athenaSupport.executeQuery(database, query, getMapper(eventType, month));
	}

	RowMapper<StatisticsMonthlyProjectFiles> getMapper(FileEvent eventType, YearMonth month) {
		return new RowMapper<StatisticsMonthlyProjectFiles>() {

			@Override
			public StatisticsMonthlyProjectFiles mapRow(Row row) {
				List<Datum> values = row.getData();

				int colIndex = 0;

				String projectId = values.get(colIndex++).getVarCharValue();
				String filesCount = values.get(colIndex++).getVarCharValue();
				String usersCount = values.get(colIndex++).getVarCharValue();

				StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();

				dto.setEventType(eventType);
				dto.setMonth(month);
				
				// The S3 data might contain null values, still return a record but skip the parsing
				if (projectId != null) {
					dto.setProjectId(Long.valueOf(projectId));
				}
				
				if (filesCount != null) {
					dto.setFilesCount(Integer.valueOf(filesCount));
				} else {
					dto.setFilesCount(0);
				}
				
				if (usersCount != null) {
					dto.setUsersCount(Integer.valueOf(usersCount));
				} else {
					dto.setUsersCount(0);
				}

				return dto;
			}

		};
	}

	String getAggregateQuery(FileEvent eventType, YearMonth month) {

		String tableName = tableNameProvider.getTableName(eventType);
		int yearValue = month.getYear();
		int monthValue = month.getMonthValue();

		return String.format(SQL_AGGREGATE_TEMPLATE, tableName, yearValue, monthValue);
	}

	private Database getDatabase() {
		return athenaSupport.getDatabase(DATABASE_NAME);
	}

}
