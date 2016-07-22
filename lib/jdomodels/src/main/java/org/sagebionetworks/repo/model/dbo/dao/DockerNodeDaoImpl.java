package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOCKER_REPOSITORY_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOCKER_REPOSITORY_NAME;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerManagedRepositoryName;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
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
		if (StringUtils.isEmpty(repositoryName)) throw new InvalidModelException("repositoryName is required.");
		DBODockerManagedRepositoryName dbo = new DBODockerManagedRepositoryName();
		dbo.setOwner(KeyFactory.stringToKey(entityId));
		dbo.setRepositoryName(repositoryName);
		basicDao.createNew(dbo);
	}
	
	@Override
	public String getEntityIdForRepositoryName(String repositoryName) {
		if (StringUtils.isEmpty(repositoryName)) throw new InvalidModelException("repositoryName is required.");
		try {
			long nodeId = jdbcTemplate.queryForObject(REPOSITORY_ID_SQL, Long.class, repositoryName);
			return KeyFactory.keyToString(nodeId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public String getRepositoryNameForEntityId(String entityId) {
		if (StringUtils.isEmpty(entityId)) throw new InvalidModelException("repositoryName is required.");
		try {
			return jdbcTemplate.queryForObject(REPOSITORY_NAME_SQL, String.class, KeyFactory.stringToKey(entityId));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
}
