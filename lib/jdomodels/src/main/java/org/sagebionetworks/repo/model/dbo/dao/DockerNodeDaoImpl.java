package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;

import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerManagedRepositoryName;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DockerNodeDaoImpl implements DockerNodeDao {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;
	
	private static final String REPOSITORY_NAME_SQL = 
			"SELECT "+COL_DOCKER_REPOSITORY_OWNER_ID+" FROM "+TABLE_DOCKER_REPOSITORY_NAME+
			" WHERE "+COL_DOCKER_REPOSITORY_NAME+"=?";
	
	@WriteTransaction
	@Override
	public void createRepositoryName(String entityId, String repositoryName) {
		DBODockerManagedRepositoryName dbo = new DBODockerManagedRepositoryName();
		dbo.setOwner(KeyFactory.stringToKey(entityId));
		dbo.setRepositoryName(repositoryName);
		basicDao.createNew(dbo);
	}
	
	@Override
	public String getEntityIdForRepositoryName(String repositoryName) {
		Long nodeId = jdbcTemplate.queryForObject(REPOSITORY_NAME_SQL, Long.class);
		return nodeId==null ? null : KeyFactory.keyToString(nodeId);
	}
}
