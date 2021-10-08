package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProviderFactory;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.search.RowSearchProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableIndexConnectionFactoryImpl implements TableIndexConnectionFactory {
	
	private ConnectionFactory connectionFactory;
	
	private TableManagerSupport tableManagerSupport;
	
	private MetadataIndexProviderFactory metaDataIndexProviderFactory;
	
	private ObjectFieldModelResolverFactory objectFieldModelResolverFactory;
	
	private RowSearchProcessor searchProcessor;

	@Autowired
	public TableIndexConnectionFactoryImpl(ConnectionFactory connectionFactory, TableManagerSupport tableManagerSupport,
			MetadataIndexProviderFactory metaDataIndexProviderFactory, ObjectFieldModelResolverFactory objectFieldModelResolverFactory,
			RowSearchProcessor searchProcessor) {
		this.connectionFactory = connectionFactory;
		this.tableManagerSupport = tableManagerSupport;
		this.metaDataIndexProviderFactory = metaDataIndexProviderFactory;
		this.objectFieldModelResolverFactory = objectFieldModelResolverFactory;
		this.searchProcessor = searchProcessor;
	}

	@Override
	public TableIndexManager connectToTableIndex(IdAndVersion tableId) {
		if(tableId == null){
			throw new IllegalArgumentException("TableId cannot be null");
		}
		TableIndexDAO dao = connectionFactory.getConnection(tableId);
		if(dao == null){
			throw new TableIndexConnectionUnavailableException("Cannot connect to table: "+tableId);
		}
		return new TableIndexManagerImpl(dao, tableManagerSupport, metaDataIndexProviderFactory, objectFieldModelResolverFactory, searchProcessor);
	}
	
	@Override
	public TableIndexManager connectToFirstIndex(){
		TableIndexDAO dao = connectionFactory.getFirstConnection();
		return new TableIndexManagerImpl(dao, tableManagerSupport, metaDataIndexProviderFactory, objectFieldModelResolverFactory, searchProcessor);
	}

}
