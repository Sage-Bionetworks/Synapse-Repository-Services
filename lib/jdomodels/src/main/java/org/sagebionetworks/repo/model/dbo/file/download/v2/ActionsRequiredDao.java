package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.download.Action;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class ActionsRequiredDao {

	public static final String TEMP_ACTION_REQUIRED_TEMPLATE = DDLUtilsImpl.loadSQLFromClasspath("sql/TempActionRequired-ddl.sql");
	
	public static final RowMapper<ActionRequiredCount> ACTION_MAPPER = (ResultSet rs, int rowNum) -> {
		ActionType type = ActionType.valueOf(rs.getString("ACTION_TYPE"));
		Long actionId = rs.getLong("ACTION_ID");
		Long count = rs.getLong("COUNT");
		Action action = null;
		switch (type) {
		case ACCESS_REQUIREMENT:
			action = new MeetAccessRequirement().setAccessRequirementId(actionId);
			break;
		case DOWNLOAD_PERMISSION:
			action = new RequestDownload().setBenefactorId(actionId);
			break;
		case ENABLE_TWO_FA:
			action = new EnableTwoFa().setAccessRequirementId(actionId);
			break;
		default:
			throw new IllegalStateException("Unknown type: " + type.name());
		}
		return new ActionRequiredCount().setCount(count).setAction(action);
	};
	
	private static String getTempTableName(long userId) {
		return "U" + userId + "A";
	}
	
	private JdbcTemplate jdbcTemplate;
	
	public ActionsRequiredDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}
	
	public String createActionsRequiredTable(long userId, long batchSize, FilesBatchProvider filesProvider, EntityActionRequiredCallback callback) {
		String tableName = getTempTableName(userId);
		jdbcTemplate.update(String.format(TEMP_ACTION_REQUIRED_TEMPLATE, tableName));
		List<Long> batch = null;
		long limit = batchSize;
		long offset = 0L;
		do {
			batch = filesProvider.getBatchOfFiles(limit, offset);
			offset += limit;
			if (batch.isEmpty()) {
				break;
			}
			// Determine the sub-set that requires some actions
			List<FileActionRequired> actions = callback.filter(batch);
			// Add the sub-set to the temporary table.
			addBatchOfActionsToTempTable(actions.toArray(new FileActionRequired[actions.size()]), tableName);
		} while (batch.size() == batchSize);
		
		return tableName;
	}
	
	public List<ActionRequiredCount> getActionsRequiredCount(long userId, long limit) {
		String tableName = getTempTableName(userId);
		String sql = String.format("SELECT ACTION_TYPE, ACTION_ID, COUNT(*) AS COUNT FROM %s GROUP BY ACTION_TYPE, ACTION_ID ORDER BY COUNT DESC LIMIT %s", tableName, limit);
		return jdbcTemplate.query(sql, ACTION_MAPPER);
	}
	
	public void dropActionsRequiredTable(long userId) {
		jdbcTemplate.update(String.format("DROP TEMPORARY TABLE IF EXISTS %S ", getTempTableName(userId)));
	}
	
	private void addBatchOfActionsToTempTable(FileActionRequired[] actions, String tableName) {
		if (actions.length < 1) {
			return;
		}
		String sql = String.format("INSERT IGNORE INTO %S (FILE_ID, ACTION_TYPE, ACTION_ID) VALUES (?,?,?)", tableName);
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				FileActionRequired required = actions[i];
				int index = 0;
				ps.setLong(++index, required.getFileId());
				Action action = required.getAction();
				if (action instanceof MeetAccessRequirement) {
					ps.setString(++index, ActionType.ACCESS_REQUIREMENT.name());
					ps.setLong(++index, ((MeetAccessRequirement) action).getAccessRequirementId());
				} else if (action instanceof RequestDownload) {
					ps.setString(++index, ActionType.DOWNLOAD_PERMISSION.name());
					ps.setLong(++index, ((RequestDownload) action).getBenefactorId());
				} else if (action instanceof EnableTwoFa) {
					ps.setString(++index, ActionType.ENABLE_TWO_FA.name());
					ps.setLong(++index, ((EnableTwoFa) action).getAccessRequirementId());
				} else {
					throw new IllegalStateException("Unknown action type: " + action.getClass().getName());
				}
			}

			@Override
			public int getBatchSize() {
				return actions.length;
			}
		});
	}
}
