package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class TableTruthManagerImpl implements TableTruthManager {

	@Autowired
	ColumnModelDAO columnModelDao;
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	TableRowTruthDAO tableTruthDao;

	@Override
	public String getSchemaMD5Hex(String tableId) {
		List<ColumnModel> truthSchema = columnModelDao
				.getColumnModelsForObject(tableId);
		return TableModelUtils.createSchemaMD5HexCM(truthSchema);
	}

	@Override
	public long getTableVersion(String tableId) {
		// Determine the type of able
		ObjectType type = getTableType(tableId);
		switch (type) {
		case TABLE:
			// For tables the version of the last change set is used.
			return tableTruthDao.getLastTableRowChange(tableId).getRowVersion();
		}
		throw new IllegalArgumentException("unknown table type: " + type);
	}

	@Override
	public boolean isTableAvailable(String tableId) {
		return nodeDao.isNodeAvailable(KeyFactory.stringToKey(tableId));
	}

	@Override
	public ObjectType getTableType(String tableId) {
		EntityType type = nodeDao.getNodeTypeById(tableId);
		switch (type) {
		case table:
			return ObjectType.TABLE;
		case fileview:
			return ObjectType.FILE_VIEW;
		}
		throw new IllegalArgumentException("unknown table type: " + type);
	}

}
