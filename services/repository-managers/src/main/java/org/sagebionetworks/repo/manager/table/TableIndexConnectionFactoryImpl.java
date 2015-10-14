package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;


public class TableIndexConnectionFactoryImpl implements
		TableIndexConnectionFactory {
	
	@Autowired
	ConnectionFactory connectionFactory;

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
		return new TableIndexManagerImpl(dao, tableId);
	}

}
