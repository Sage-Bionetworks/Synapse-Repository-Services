package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBOForum;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.ForumUtils;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOForumDAOImpl implements ForumDAO {

	private static final String SQL_DELETE_FORUM = "DELETE FROM "+TABLE_FORUM
			+" WHERE "+COL_FORUM_ID+" = ?";
	private static final String SQL_SELECT_FORUM_BY_ID = "SELECT * FROM "
			+TABLE_FORUM+" WHERE "+COL_FORUM_ID+" = ?";
	private static final String SQL_SELECT_FORUM_BY_PROJECT_ID = "SELECT * FROM "
			+TABLE_FORUM+" WHERE "+COL_FORUM_PROJECT_ID+" = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	private static RowMapper<DBOForum> ROW_MAPPER = new DBOForum().getTableMapping();

	@WriteTransactionReadCommitted
	@Override
	public Forum createForum(String projectId) {
		ValidateArgument.required(projectId, "projectId");
		long id = idGenerator.generateNewId(IdType.FORUM_ID);
		DBOForum dbo = new DBOForum();
		dbo.setId(id);
		dbo.setProjectId(KeyFactory.stringToKey(projectId));
		String etag = UUID.randomUUID().toString();
		dbo.setEtag(etag);
		basicDao.createNew(dbo);
		transactionalMessenger.sendMessageAfterCommit(""+id, ObjectType.FORUM, etag, ChangeType.UPDATE);
		return getForum(id);
	}

	@Override
	public Forum getForum(long id) {
		List<DBOForum> results = jdbcTemplate.query(SQL_SELECT_FORUM_BY_ID, ROW_MAPPER, id);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return ForumUtils.createDTOFromDBO(results.get(0));
	}

	@Override
	public Forum getForumByProjectId(String projectId) {
		ValidateArgument.required(projectId, "projectId");
		List<DBOForum> results = jdbcTemplate.query(SQL_SELECT_FORUM_BY_PROJECT_ID, ROW_MAPPER,
				KeyFactory.stringToKey(projectId));
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return ForumUtils.createDTOFromDBO(results.get(0));
	}

	@WriteTransactionReadCommitted
	@Override
	public int deleteForum(long id) {
		return jdbcTemplate.update(SQL_DELETE_FORUM, id);
	}

}
