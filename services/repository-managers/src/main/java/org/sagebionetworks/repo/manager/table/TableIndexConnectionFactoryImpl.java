package org.sagebionetworks.repo.manager.table;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;


public class TableIndexConnectionFactoryImpl implements
		TableIndexConnectionFactory {
	
	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired
	TableManagerSupport tableManagerSupport;
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory#connectToTableIndex(java.lang.String)
	 */
	@Override
	public TableIndexManager connectToTableIndex(String tableId) {
		if(tableId == null){
			throw new IllegalArgumentException(tableId);
		}
		TableIndexDAO dao = connectionFactory.getConnection(tableId);
		if(dao == null){
			throw new TableIndexConnectionUnavailableException("Cannot connect to table: "+tableId);
		}
		return new TableIndexManagerImpl(dao, tableManagerSupport, tableId);
	}
	@Override
	public List<TableIndexManager> getAllConnections() {
		List<TableIndexManager> results = new LinkedList<>();
		List<TableIndexDAO> daoList = connectionFactory.getAllConnections();
		for(TableIndexDAO dao: daoList){
			if(dao == null){
				throw new TableIndexConnectionUnavailableException("Cannot connect to table: "+tableId);
			}
			results.add(new TableIndexManagerImpl(dao, tableManagerSupport, tableId);
		}
		return null;
	}

}
