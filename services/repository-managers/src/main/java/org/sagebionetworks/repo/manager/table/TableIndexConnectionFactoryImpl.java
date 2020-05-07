package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;


public class TableIndexConnectionFactoryImpl implements TableIndexConnectionFactory {
	
	@Autowired
	private ConnectionFactory connectionFactory;
	
	@Autowired
	private TableManagerSupport tableManagerSupport;
	
	@Autowired
	private MetadataIndexProviderFactory metaDataIndexProviderFactory;
	
	@Override
	public TableIndexManager connectToTableIndex(IdAndVersion tableId) {
		if(tableId == null){
			throw new IllegalArgumentException("TableId cannot be null");
		}
		TableIndexDAO dao = connectionFactory.getConnection(tableId);
		if(dao == null){
			throw new TableIndexConnectionUnavailableException("Cannot connect to table: "+tableId);
		}
		return new TableIndexManagerImpl(dao, tableManagerSupport, metaDataIndexProviderFactory);
	}
	
	@Override
	public TableIndexManager connectToFirstIndex(){
		TableIndexDAO dao = connectionFactory.getFirstConnection();
		return new TableIndexManagerImpl(dao, tableManagerSupport, metaDataIndexProviderFactory);
	}

}
