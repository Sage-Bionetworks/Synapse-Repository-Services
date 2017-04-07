package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.CommentDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCommentDAOImplTest {
	
	@Autowired
	private CommentDAO commentDAO;

	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private FileHandleDao fileDAO;

	@Autowired
	private IdGenerator idGenerator;
	
	private String fileHandleId;	
	private UserGroup maliciousUser;
	
	private List<String> cleanup;
	
	@Before
	public void setup() throws Exception {
		cleanup = new ArrayList<String>();
		
		maliciousUser = new UserGroup();
		maliciousUser.setIsIndividual(true);
		maliciousUser.setId(userGroupDAO.create(maliciousUser).toString());
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		FileHandle handle = TestUtils.createS3FileHandle(maliciousUser.getId(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
	}
	
	@After
	public void cleanup() throws Exception {
		for (String id : cleanup) {
			messageDAO.deleteMessage(id);
		}
		fileDAO.delete(fileHandleId);
		userGroupDAO.delete(maliciousUser.getId());
	}
	
	@Test
	public void testCreate() throws Exception {
		Comment dto = new Comment();
		// Note: ID is auto generated
		dto.setCreatedBy(maliciousUser.getId());
		dto.setFileHandleId(fileHandleId);
		// Note: CreatedOn is set by the DAO
		dto.setTargetId("1337");
		dto.setTargetType(ObjectType.ENTITY);
		
		dto = commentDAO.createComment(dto);
		assertNotNull(dto.getId());
		cleanup.add(dto.getId());
		assertNotNull(dto.getCreatedOn());
	}
}
