package org.sagebionetworks.table.cluster;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.util.ProgressCallback;
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CurrentRowCacheSqlDaoImpl implements CurrentRowCacheDao {
	private static Logger log = LogManager.getLogger(CurrentRowCacheSqlDaoImpl.class);

	private final TransactionTemplate transactionWriteTemplate;
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
		// This will manage transactions for calls that need write transactions
		this.transactionWriteTemplate = createTransactionTemplate(transactionManager, false);
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
	public long getLatestCurrentVersionNumber(final Long tableId) {
		return tryInReadTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.selectCurrentRowMaxVersion(tableId);
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
	public void putCurrentVersion(final Long tableId, final Long rowId, final Long versionNumber) {
		putCurrentVersions(tableId, Collections.singletonMap(rowId, versionNumber), null);
	}

	@Override
	public void putCurrentVersions(final Long tableId, final Map<Long, Long> rowsAndVersions, ProgressCallback<Long> progressCallback) {
		TransactionCallback<Void> action = new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.updateCurrentRowSQL(tableId);
				List<Object[]> batchArgs = Lists.newArrayListWithCapacity(rowsAndVersions.size());
				for (Entry<Long, Long> entry : rowsAndVersions.entrySet()) {
					batchArgs.add(new Object[] { entry.getKey(), entry.getValue() });
				}
				template.batchUpdate(sql, batchArgs);
				return null;
			}
		};
		tryAndCreateInWriteTransaction(tableId, action);
	}

	@Override
	public Long getCurrentVersion(final Long tableId, final Long rowId) {
		return tryInReadTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.selectCurrentRowVersionOfRow(tableId);
				List<Long> version = template.queryForList(sql, Long.class, rowId);
				if (version.size() == 1) {
					return version.get(0);
				} else if (version.size() > 1) {
					throw new IllegalStateException("More than one version returned for this row, should not be possible");
				} else {
					return null;
				}
			}
		}, null);
	}

	@Override
	public Map<Long, Long> getCurrentVersions(final Long tableId, final Iterable<Long> rowIds) {
		final Map<Long, Long> result = Maps.newHashMap();
		return tryInReadTransaction(new TransactionCallback<Map<Long, Long>>() {
			@Override
			public Map<Long, Long> doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.selectCurrentRowVersionsForInRows(tableId);
				namedTemplate.query(sql, new MapSqlParameterSource(SQLUtils.ROW_ID_BIND, rowIds), new RowCallbackHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						Long rowId = rs.getLong(TableConstants.ROW_ID);
						Long version = rs.getLong(TableConstants.ROW_VERSION);
						result.put(rowId, version);
					}
				});
				return result;
			}
		}, result);
	}

	@Override
	public Map<Long, Long> getCurrentVersions(final Long tableId, final long rowIdOffset, final long limit) {
		final Map<Long, Long> result = Maps.newHashMap();
		return tryInReadTransaction(new TransactionCallback<Map<Long, Long>>() {
			@Override
			public Map<Long, Long> doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.selectCurrentRowVersionsForRowRange(tableId);
				template.query(sql, new RowCallbackHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						Long rowId = rs.getLong(TableConstants.ROW_ID);
						Long version = rs.getLong(TableConstants.ROW_VERSION);
						result.put(rowId, version);
					}
				}, rowIdOffset, rowIdOffset + limit);
				return result;
			}
		}, result);
	}

	@Override
	public void deleteCurrentVersion(final Long tableId, final Long rowId) {
		tryAndIgnoreInWriteTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.deleteFromTableSQL(tableId, TableType.CURRENT_ROW);
				template.update(sql, rowId);
				return null;
			}
		});
	}

	@Override
	public void deleteCurrentVersions(final Long tableId, final Iterable<Long> rowIds) {
		tryAndIgnoreInWriteTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				String sql = SQLUtils.deleteBatchFromTableSQL(tableId, TableType.CURRENT_ROW);
				for (List<Long> batch : Iterables.partition(rowIds, 300)) {
					namedTemplate.update(sql, new MapSqlParameterSource(SQLUtils.ROW_ID_BIND, batch));
				}
				return null;
			}
		});
	}

	@Override
	public void deleteCurrentTable(final Long tableId) {
		tryAndIgnoreInWriteTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				String dropTableDML = SQLUtils.dropTableSQL(tableId, SQLUtils.TableType.CURRENT_ROW);
				template.update(dropTableDML);
				return null;
			}
		});
	}

	@Override
	public void truncateAllData() {
		tryAndIgnoreInWriteTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				List<String> tables = template.queryForList("show tables", String.class);
				for (String table : tables) {
					if (SQLUtils.isTableName(table, TableType.CURRENT_ROW)) {
						template.update("drop table " + table);
					}
				}
				return null;
			}
		});
	}

	private void tryAndCreateInWriteTransaction(Long tableId, TransactionCallback<Void> action) {
		try {
			this.transactionWriteTemplate.execute(action);
		} catch (BadSqlGrammarException e) {
			// this usually means the table does not exist, so create and try again
			String sql = SQLUtils.createTableSQL(tableId, TableType.CURRENT_ROW);
			template.execute(sql);
			this.transactionWriteTemplate.execute(action);
		}
	}

	private void tryAndIgnoreInWriteTransaction(TransactionCallback<Void> action) {
		try {
			this.transactionWriteTemplate.execute(action);
		} catch (BadSqlGrammarException e) {
			// this usually means the table does not exist, so ignore
		}
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
