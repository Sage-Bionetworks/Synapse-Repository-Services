package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableIndexConnectionFactoryImpl implements TableIndexConnectionFactory {
	

	private final TableIndexManager manager;

	@Autowired
	public TableIndexConnectionFactoryImpl(TableIndexManager manager) {
		this.manager = manager;
	}

	@Override
	public TableIndexManager connectToTableIndex(IdAndVersion tableId) {
		if(tableId == null){
			throw new IllegalArgumentException("TableId cannot be null");
		}
		return manager;
	}
	
	@Override
	public TableIndexManager connectToFirstIndex(){
		return manager;
	}

}
