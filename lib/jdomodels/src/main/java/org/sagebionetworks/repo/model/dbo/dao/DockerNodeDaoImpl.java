package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;

import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerManagedRepositoryName;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DockerNodeDaoImpl implements DockerNodeDao {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;
	
	private static final String REPOSITORY_ID_SQL = 
			"SELECT "+COL_DOCKER_REPOSITORY_OWNER_ID+" FROM "+TABLE_DOCKER_REPOSITORY_NAME+
			" WHERE "+COL_DOCKER_REPOSITORY_NAME+"=?";
	
	private static final String REPOSITORY_NAME_SQL = 
			"SELECT "+COL_DOCKER_REPOSITORY_NAME+" FROM "+TABLE_DOCKER_REPOSITORY_NAME+
			" WHERE "+COL_DOCKER_REPOSITORY_OWNER_ID+"=?";
	
	@WriteTransaction
	@Override
	public void createRepositoryName(String entityId, String repositoryName) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(repositoryName, "repositoryName");
		DBODockerManagedRepositoryName dbo = new DBODockerManagedRepositoryName();
		dbo.setOwner(KeyFactory.stringToKey(entityId));
		dbo.setRepositoryName(repositoryName);
		basicDao.createNew(dbo);
	}
	
	@Override
	public String getEntityIdForRepositoryName(String repositoryName) {
		ValidateArgument.required(repositoryName, "repositoryName");
		try {
			long nodeId = jdbcTemplate.queryForObject(REPOSITORY_ID_SQL, Long.class, repositoryName);
			return KeyFactory.keyToString(nodeId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public String getRepositoryNameForEntityId(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		try {
			return jdbcTemplate.queryForObject(REPOSITORY_NAME_SQL, String.class, KeyFactory.stringToKey(entityId));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
}
