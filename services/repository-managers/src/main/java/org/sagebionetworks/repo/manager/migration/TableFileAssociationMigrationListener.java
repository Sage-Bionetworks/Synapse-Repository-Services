package org.sagebionetworks.repo.manager.migration;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableFileAssociationDao;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class TableFileAssociationMigrationListener implements
		MigrationTypeListener {
	
	private static final Logger log = LogManager.getLogger(TableFileAssociationMigrationListener.class);
	
	@Autowired
	TableRowTruthDAO tableRowTruthDAO;
	
	@Autowired
	ColumnModelDAO columnModelDao;
	
	@Autowired
	TableFileAssociationDao tableFileAssociationDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		
		if(MigrationType.TABLE_CHANGE == type){
			for(D row: delta){
				if(row instanceof DBOTableRowChange){
					DBOTableRowChange change = (DBOTableRowChange) row;
					String tableId = change.getTableId().toString();
					TableRowChange tableRowChange = tableRowTruthDAO.getTableRowChange(tableId, change.getRowVersion());
					List<String> columnIds = new LinkedList<String>();
					for(Long longId: tableRowChange.getIds()){
						columnIds.add(longId.toString());
					}
					List<ColumnModel> columns = columnModelDao.getColumnModel(columnIds, true);
					try {
						final List<Row> rows = new LinkedList<Row>();
						tableRowTruthDAO.scanRowSet(tableId, change.getRowVersion(), new RowHandler() {
							
							@Override
							public void nextRow(Row row) {
								rows.add(row);
							}
						});
						// We can now extract the file handle ids
						Set<String> fileHandleIds = TableModelUtils.getFileHandleIdsInRowSet(columns, rows);
						// bind the files to the table
						tableFileAssociationDao.bindFileHandleIdsToTable(tableId, fileHandleIds);
					} catch (Exception e) {
						log.error("Failed to migrate a row", e);
					} 
				}
			}
		}

	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// TODO Auto-generated method stub

	}

}
