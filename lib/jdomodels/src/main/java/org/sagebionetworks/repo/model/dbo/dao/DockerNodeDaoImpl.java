package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerManagedRepositoryName;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@WriteTransactionReadCommitted
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
		List<Long> nodeIds = jdbcTemplate.queryForList(REPOSITORY_ID_SQL, Long.class, repositoryName);
		if (nodeIds.size()==0) {
			return null;
		}
		if (nodeIds.size()==1) {
			 return KeyFactory.keyToString(nodeIds.get(0));
		}
		throw new DatastoreException("Expected 0-1 but found "+nodeIds.size());
	}

	@Override
	public String getRepositoryNameForEntityId(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		List<String> repositoryNames = jdbcTemplate.queryForList(REPOSITORY_NAME_SQL, String.class, KeyFactory.stringToKey(entityId));
		if (repositoryNames.size()==0) {
			return null;
		}
		if (repositoryNames.size()==1) {
			 return repositoryNames.get(0);
		}
		throw new DatastoreException("Expected 0-1 but found "+repositoryNames.size());
	}
}
