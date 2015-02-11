package org.sagebionetworks.table.cluster;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Maps;

public class CurrentRowCacheSqlDaoImpl implements CurrentRowCacheDao {
	private static Logger log = LogManager.getLogger(CurrentRowCacheSqlDaoImpl.class);

	// Copied from com.mysql.jdbc.SQLError
	public static final String SQL_STATE_COLUMN_NOT_FOUND = "S0022"; //$NON-NLS-1$

	private final TransactionTemplate transactionReadTemplate;
	private final JdbcTemplate template;
	private final NamedParameterJdbcTemplate namedTemplate;

	/**
	 * The IoC constructor.
	 * 
	 * @param template
	 * @param transactionManager
	 */
	public CurrentRowCacheSqlDaoImpl(DataSource dataSource) {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
		// This will manage transactions for calls that need read transactions
		this.transactionReadTemplate = createTransactionTemplate(transactionManager, true);

		this.template = new JdbcTemplate(dataSource);
		this.namedTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	private static TransactionTemplate createTransactionTemplate(DataSourceTransactionManager transactionManager, boolean readOnly) {
		DefaultTransactionDefinition transactionDef;
		transactionDef = new DefaultTransactionDefinition();
		transactionDef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		transactionDef.setReadOnly(readOnly);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDef.setName("CurrentRowCacheSqlDaoImpl");
		return new TransactionTemplate(transactionManager, transactionDef);
	}

	@Override
	public boolean isEnabled() {
		return StackConfiguration.singleton().getTableEnabled();
	}

	@Override
	public long getLatestCurrentRowVersionNumber(final Long tableId) {
		return tryInReadTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.getStatusMaxVersionSQL(tableId.toString());
				List<Long> version = template.queryForList(sql, Long.class);
				if (version.size() == 1 && version.get(0) != null) {
					return version.get(0);
				} else {
					return -1L;
				}
			}
		}, -1L);
	}

	@Override
	public Map<Long, RowAccessor> getCurrentRows(final Long tableId, final Iterable<Long> rowIds, final ColumnMapper mapper) {
		final Map<Long, RowAccessor> result = Maps.newHashMap();
		return tryInReadTransaction(new TransactionCallback<Map<Long, RowAccessor>>() {
			@Override
			public Map<Long, RowAccessor> doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.selectRowValuesForRowId(tableId);
				namedTemplate.query(sql, new MapSqlParameterSource(SQLUtils.ROW_ID_BIND, rowIds), new RowCallbackHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						final Long rowId = rs.getLong(TableConstants.ROW_ID);
						final Long version = rs.getLong(TableConstants.ROW_VERSION);
						final Map<Long, String> columnIdToValueMap = Maps.newHashMap();
						for (SelectColumnAndModel selectAndColumnModel : mapper.getSelectColumnAndModels()) {
							String value = null;
							try {
								value = rs.getString(SQLUtils.getColumnNameForId(selectAndColumnModel.getColumnModel().getId()));
								value = TableModelUtils.translateRowValueFromQuery(value, selectAndColumnModel.getColumnType());
								columnIdToValueMap.put(Long.parseLong(selectAndColumnModel.getColumnModel().getId()), value);
							} catch (SQLException e) {
								// we expect there to be columns that don't exist because they've been added but the
								// table is not yet updated
								if (!e.getSQLState().equals(SQL_STATE_COLUMN_NOT_FOUND)) {
									throw e;
								}
							}
						}
						result.put(rowId, new RowAccessor() {
							@Override
							public String getCellById(Long columnId) {
								return columnIdToValueMap.get(columnId);
							}

							@Override
							public Long getRowId() {
								return rowId;
							}

							@Override
							public Long getVersionNumber() {
								return version;
							}
						});
					}
				});
				return result;
			}
		}, result);
	}

	private <T> T tryInReadTransaction(TransactionCallback<T> action, T defaultValueIfTableDoesntExist) {
		try {
			return this.transactionReadTemplate.execute(action);
		} catch (BadSqlGrammarException e) {
			// this usually means the table does not exist
			return defaultValueIfTableDoesntExist;
		}
	}
}
