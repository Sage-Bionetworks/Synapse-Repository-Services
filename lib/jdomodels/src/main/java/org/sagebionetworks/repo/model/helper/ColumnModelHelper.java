package org.sagebionetworks.repo.model.helper;

import java.util.List;
import java.util.function.Consumer;

import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ColumnModelHelper implements DaoObjectHelper<ColumnModel> {
	
	@Autowired
	private ColumnModelDAO columnModelDao;

	@Override
	public ColumnModel create(Consumer<ColumnModel> consumer) {
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setName("someBoolean");
		consumer.accept(cm);
		return columnModelDao.createColumnModel(cm);
	}

	@Override
	public void truncateAll() {
		columnModelDao.truncateAllColumnData();
	}
	
	public void bindColumnToObject(List<ColumnModel> columnModels, IdAndVersion objectIdAndVersion) {
		columnModelDao.bindColumnToObject(columnModels, objectIdAndVersion);
	}

}
