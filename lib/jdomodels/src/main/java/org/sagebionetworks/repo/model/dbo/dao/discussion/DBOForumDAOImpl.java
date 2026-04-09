package org.sagebionetworks.repo.model.dbo.dao.discussion;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBOForum;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.ForumUtils;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOForumDAOImpl implements ForumDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private static RowMapper<DBOForum> ROW_MAPPER = new DBOForum().getTableMapping();

	@WriteTransaction
	@Override
	public Forum createForum(String objectId, ForumObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		long id = idGenerator.generateNewId(IdType.FORUM_ID);
		DBOForum dbo = new DBOForum();
		dbo.setId(id);
		dbo.setObjectId(KeyFactory.stringToKey(objectId));
		dbo.setObjectType(objectType.name());
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.createNew(dbo);
		return getForum(id);
	}

	@Override
	public Forum getForum(long id) {
		List<DBOForum> results = jdbcTemplate.query("SELECT * FROM FORUM WHERE ID = ?", ROW_MAPPER, id);
		if (results.size() != 1) {
			throw new NotFoundException(String.format("Forum: '%s' does not exist", id));
		}
		return ForumUtils.createDTOFromDBO(results.get(0));
	}

	@Override
	public Forum getForumByObjectIdAndType(String objectId, ForumObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");
		List<DBOForum> results = jdbcTemplate.query(
				"SELECT * FROM FORUM WHERE OBJECT_ID = ? AND OBJECT_TYPE = ?",
				ROW_MAPPER, KeyFactory.stringToKey(objectId), objectType.name());
		if (results.size() != 1) {
			throw new NotFoundException(String.format("Forum for %s '%s' does not exist", objectType, objectId));
		}
		return ForumUtils.createDTOFromDBO(results.get(0));
	}

	@WriteTransaction
	@Override
	public int deleteForum(long id) {
		return jdbcTemplate.update("DELETE FROM FORUM WHERE ID = ?", id);
	}

}
