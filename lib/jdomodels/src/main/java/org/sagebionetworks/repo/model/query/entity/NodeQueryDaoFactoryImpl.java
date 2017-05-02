package org.sagebionetworks.repo.model.query.entity;

import javax.sql.DataSource;

import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class NodeQueryDaoFactoryImpl implements NodeQueryDaoFactory {
	
	@Autowired
	ConnectionFactory connectionFactory;

	@Override
	public NodeQueryDaoV2 createConnection() {
		DataSource dataSource = connectionFactory.getFirstDataSource();
		return new NodeQueryDaoV2Impl(dataSource);
	}

}
