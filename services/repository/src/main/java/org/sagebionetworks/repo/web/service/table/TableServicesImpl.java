package org.sagebionetworks.repo.web.service.table;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * Basic implementation of the TableServices.
 *
 */
public class TableServicesImpl implements TableServices {
	
	@Autowired
	UserManager userManager;
	@Autowired
	ColumnModelManager columnModelManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	TableQueryManager tableQueryManager;

	@Override
	public ColumnModel createColumnModel(Long userId, ColumnModel columnModel) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.createColumnModel(user, columnModel);
	}

	@Override
	public List<ColumnModel> createColumnModels(Long userId, List<ColumnModel> columnModels) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.createColumnModels(user, columnModels);
	}

	@Override
	public ColumnModel getColumnModel(Long userId, String columnId) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.getColumnModel(user, columnId);
	}

	@Override
	public PaginatedColumnModels getColumnModelsForTableEntity(Long userId, String entityId) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		List<ColumnModel> models = columnModelManager.getColumnModelsForTable(user, entityId);
		PaginatedColumnModels pcm = new PaginatedColumnModels();
		pcm.setResults(models);
		pcm.setTotalNumberOfResults((long) models.size());
		return pcm;
	}

	@Override
	public PaginatedColumnModels listColumnModels(Long userId, String prefix, Long limit, Long offset) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		if(limit == null){
			limit = new Long(10);
		}
		if(offset == null){
			offset = new Long(0);
		}
		return columnModelManager.listColumnModels(user, prefix, limit, offset);
	}

	@Override
	public RowReferenceSet deleteRows(Long userId, RowSelection rowsToDelete) throws DatastoreException, NotFoundException, IOException {
		Validate.required(rowsToDelete, "rowsToDelete");
		Validate.required(rowsToDelete.getTableId(), "rowsToDelete.tableId");
		UserInfo user = userManager.getUserInfo(userId);
		return tableEntityManager.deleteRows(user, rowsToDelete.getTableId(), rowsToDelete);
	}

	@Override
	public RowSet getReferenceSet(Long userId, RowReferenceSet rowsToGet) throws DatastoreException, NotFoundException, IOException {
		Validate.required(rowsToGet, "rowsToGet");
		Validate.required(rowsToGet.getTableId(), "rowsToGet.tableId");
		UserInfo user = userManager.getUserInfo(userId);
		List<ColumnModel> columns = columnModelManager.getCurrentColumns(user, rowsToGet.getTableId(), rowsToGet.getHeaders());

		return tableEntityManager.getCellValues(user, rowsToGet.getTableId(), rowsToGet, columns);
	}

	@Override
	public TableFileHandleResults getFileHandles(Long userId, RowReferenceSet fileHandlesToFind) throws IOException, NotFoundException {
		Validate.required(fileHandlesToFind, "fileHandlesToFind");
		Validate.required(fileHandlesToFind.getTableId(), "fileHandlesToFind.tableId");
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<ColumnModel> columns = columnModelManager.getCurrentColumns(userInfo, fileHandlesToFind.getTableId(), fileHandlesToFind.getHeaders());
		for (ColumnModel cm : columns) {
			if (cm != null
					&& cm.getColumnType() != ColumnType.FILEHANDLEID) {
				throw new IllegalArgumentException("Column " + cm.getId() + " is not of type FILEHANDLEID");
			}
		}
		RowSet rowSet = tableEntityManager.getCellValues(userInfo, fileHandlesToFind.getTableId(), fileHandlesToFind, columns);

		// we expect there to be null entries, but the file handle manager does not
		List<String> idsList = Lists.newArrayListWithCapacity(columns.size() * rowSet.getRows().size());
		for (Row row : rowSet.getRows()) {
			for (String id : row.getValues()) {
				if (id != null) {
					idsList.add(id);
				}
			}
		}

		Map<String, FileHandle> fileHandles = fileHandleManager.getAllFileHandlesBatch(idsList);

		TableFileHandleResults results = new TableFileHandleResults();
		results.setTableId(fileHandlesToFind.getTableId());
		results.setHeaders(fileHandlesToFind.getHeaders());
		results.setRows(Lists.<FileHandleResults> newArrayListWithCapacity(rowSet.getRows().size()));

		// insert the file handles in order. Null ids will give null file handles
		for (Row row : rowSet.getRows()) {
			FileHandleResults rowHandles = new FileHandleResults();
			rowHandles.setList(Lists.<FileHandle> newArrayListWithCapacity(columns.size()));
			for (String id : row.getValues()) {
				FileHandle fh;
				if (id != null) {
					fh = fileHandles.get(id);
				} else {
					fh = null;
				}
				rowHandles.getList().add(fh);
			}
			results.getRows().add(rowHandles);
		}
		return results;
	}

	@Override
	public String getFileRedirectURL(Long userId, String tableId, RowReference rowRef, String columnId) throws IOException, NotFoundException {
		Validate.required(columnId, "columnId");
		Validate.required(userId, "userId");

		// Get the file handles
		UserInfo userInfo = userManager.getUserInfo(userId);
		ColumnModel model = getColumnForTable(userInfo, tableId, columnId);
		if (model.getColumnType() != ColumnType.FILEHANDLEID) {
			throw new IllegalArgumentException("Column " + columnId + " is not of type FILEHANDLEID");
		}
		String fileHandleId = tableEntityManager.getCellValue(userInfo, tableId, rowRef, model);
		// Use the FileHandle ID to get the URL
		return fileHandleManager.getRedirectURLForFileHandle(fileHandleId);
	}

	@Override
	public String getFilePreviewRedirectURL(Long userId, String tableId, RowReference rowRef, String columnId) throws IOException,
			NotFoundException {
		Validate.required(columnId, "columnId");
		Validate.required(userId, "userId");

		// Get the file handles
		UserInfo userInfo = userManager.getUserInfo(userId);
		ColumnModel model = getColumnForTable(userInfo, tableId, columnId);
		if (model.getColumnType() != ColumnType.FILEHANDLEID) {
			throw new IllegalArgumentException("Column " + columnId + " is not of type FILEHANDLEID");
		}
		String fileHandleId = tableEntityManager.getCellValue(userInfo, tableId, rowRef, model);
		// Use the FileHandle ID to get the URL
		String previewFileHandleId = fileHandleManager.getPreviewFileHandleId(fileHandleId);
		return fileHandleManager.getRedirectURLForFileHandle(previewFileHandleId);
	}

	private ColumnModel getColumnForTable(UserInfo user, String tableId, String columnId) throws DatastoreException, NotFoundException {
		return columnModelManager.getColumnModel(user, columnId);
	}

	@Override
	public Long getMaxRowsPerPage(List<ColumnModel> models) {
		return tableQueryManager.getMaxRowsPerPage(models);
	}
	
}
