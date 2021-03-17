package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DaoObjectHelper<MessageToUser> messageHelper;
		
	@Autowired
	private MessageDAO messageDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.MessageAttachment;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		messageDao.truncateAll();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		messageDao.truncateAll();
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		MessageToUser m1 = createMessage();
		MessageToUser m2 = createMessage();
		MessageToUser m3 = createMessage();
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation(Long.valueOf(m1.getId()), Long.valueOf(m1.getFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(m2.getId()), Long.valueOf(m2.getFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(m3.getId()), Long.valueOf(m3.getFileHandleId()))
		);
		
		IdRange range = manager.getIdRange(associationType);
		
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, range).spliterator(), false
		).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	
	private MessageToUser createMessage() {
		return messageHelper.create(message -> {
			message.setCreatedBy(user.getId().toString());
			message.setFileHandleId(utils.generateFileHandle(user));
			message.setSubject("Subject");
			message.setRecipients(Collections.singleton(user.getId().toString()));	
		});
	}
	
}
