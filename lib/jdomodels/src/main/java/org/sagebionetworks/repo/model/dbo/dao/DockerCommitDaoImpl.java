package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBODockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DockerCommitDaoImpl implements DockerCommitDao {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;
	
	// get the latest tags for an entity
	// for this to return a single record per <entity,tag> it's crucial
	// that the table have a unique key constraint on <entity,tag,createdOn>
	private static final String LATEST_COMMIT_SQL = 
		"SELECT * FROM "+TABLE_DOCKER_COMMIT+" d "+
		" WHERE d."+COL_DOCKER_COMMIT_OWNER_ID+"=? AND d."+
		COL_DOCKER_COMMIT_CREATED_ON+"=(SELECT MAX(d2."+COL_DOCKER_COMMIT_CREATED_ON+
		") FROM "+TABLE_DOCKER_COMMIT+" d2 WHERE "+
			" d2."+COL_DOCKER_COMMIT_OWNER_ID+"=d."+COL_DOCKER_COMMIT_OWNER_ID+" AND "+
			" d2."+COL_DOCKER_COMMIT_TAG+"=d."+COL_DOCKER_COMMIT_TAG+")";
	
	private static final TableMapping<DBODockerCommit> COMMIT_ROW_MAPPER = 
			(new DBODockerCommit()).getTableMapping();
	

	@WriteTransaction
	@Override
	public void createDockerCommit(String entityId, DockerCommit commit) {
		DBODockerCommit dbo = new DBODockerCommit();
		dbo.setOwner(KeyFactory.stringToKey(entityId));
		dbo.setTag(commit.getTag());
		dbo.setDigest(commit.getDigest());
		dbo.setCreatedOn(commit.getCreatedOn().getTime());
		basicDao.createNew(dbo);
		// TODO touch node's etag
	}
	
	@Override
	public List<DockerCommit> listDockerCommits(String entityId) {
		if (entityId==null) throw new IllegalArgumentException("entityId is required.");
		long nodeIdAsLong = KeyFactory.stringToKey(entityId);
		List<DBODockerCommit> dbos = jdbcTemplate.query(LATEST_COMMIT_SQL, COMMIT_ROW_MAPPER, nodeIdAsLong);
		List<DockerCommit> result = new ArrayList<DockerCommit>();
		for (DBODockerCommit dbo : dbos) {
			DockerCommit dto = new DockerCommit();
			dto.setDigest(dbo.getDigest());
			dto.setTag(dbo.getTag());
			dto.setCreatedOn(new Date(dbo.getCreatedOn()));
			result.add(dto);
		}
		return result;
	}

}
