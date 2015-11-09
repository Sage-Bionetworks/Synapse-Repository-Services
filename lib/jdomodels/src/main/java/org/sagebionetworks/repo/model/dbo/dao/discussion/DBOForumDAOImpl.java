package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.FK_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;

import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ForumDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBOForum;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.ForumUtils;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOForumDAOImpl implements ForumDAO {

	private static final String SQL_DELETE_FORUM = "DELETE FROM "+TABLE_FORUM
			+" WHERE "+COL_FORUM_ID+" = ?";
	private static final String SQL_DELETE_FORUM_BY_PROJECT_ID = "DELETE FROM "+TABLE_FORUM
			+" WHERE "+FK_FORUM_PROJECT_ID+" = ?";
	private static final String SQL_SELECT_FORUM_BY_ID = "SELECT * FROM "
			+TABLE_FORUM+" WHERE "+COL_FORUM_ID+" = ?";
	private static final String SQL_SELECT_FORUM_BY_PROJECT_ID = "SELECT * FROM "
			+TABLE_FORUM+" WHERE "+FK_FORUM_PROJECT_ID+" = ?";
	private static final String SQL_TRUNCATE = "TRUNCATE "+TABLE_FORUM;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;

	private static RowMapper<DBOForum> ROW_MAPPER = new DBOForum().getTableMapping();

	@WriteTransaction
	@Override
	public Forum createForum(Long projectId) {
		long id = idGenerator.generateNewId(TYPE.FORUM_ID);
		DBOForum dbo = new DBOForum();
		dbo.setId(id);
		dbo.setProjectId(projectId);
		basicDao.createNew(dbo);
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
	public Forum getForumByProjectId(long projectId) {
		List<DBOForum> results = jdbcTemplate.query(SQL_SELECT_FORUM_BY_PROJECT_ID, ROW_MAPPER, projectId);
		if (results.size() != 1) {
			throw new NotFoundException();
		}
		return ForumUtils.createDTOFromDBO(results.get(0));
	}

	@Override
	public int deleteForum(long id) {
		return jdbcTemplate.update(SQL_DELETE_FORUM, id);
	}

	@Override
	public int deleteForumByProjectId(long projectId) {
		return jdbcTemplate.update(SQL_DELETE_FORUM_BY_PROJECT_ID, projectId);
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update(SQL_TRUNCATE);
	}

}
