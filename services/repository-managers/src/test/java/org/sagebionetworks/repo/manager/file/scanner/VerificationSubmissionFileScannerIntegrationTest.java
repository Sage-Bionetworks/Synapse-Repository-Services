package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class VerificationSubmissionFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private DaoObjectHelper<UserGroup> userGroupHelper;

	@Autowired
	private DaoObjectHelper<VerificationSubmission> submissionHelper;
	
	@Autowired
	private VerificationDAO verificationDao;
		
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.VerificationSubmission;
	
	private String user1;
	private String user2;
	private String user3;
	
	private List<String> usersToDelete;
	private List<Long> verificationToDelete;
	
	@BeforeEach
	public void before() {
		usersToDelete = new ArrayList<>();
		verificationToDelete = new ArrayList<>();
		
		fileHandleDao.truncateTable();
		
		user1 = userGroupHelper.create((u) -> {}).getId();
		user2 = userGroupHelper.create((u) -> {}).getId();
		user3 = userGroupHelper.create((u) -> {}).getId();
		
		usersToDelete.add(user1);
		usersToDelete.add(user2);
		usersToDelete.add(user3);
	}
	
	@AfterEach
	public void after() {
		verificationToDelete.forEach(verificationDao::deleteVerificationSubmission);
		usersToDelete.forEach(userGroupDao::delete);
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		List<VerificationSubmission> submissions = Arrays.asList(
				createVerificationSubmission(user1, 3),
				createVerificationSubmission(user2, 0),
				createVerificationSubmission(user3, 1)
		);
		
		List<ScannedFileHandleAssociation> expected = new ArrayList<>();
				
		for (VerificationSubmission s : submissions) {
			for (AttachmentMetadata f : s.getAttachments()) {
				expected.add(new ScannedFileHandleAssociation(Long.valueOf(s.getId()), Long.valueOf(f.getId())));
			}
		}
		
		IdRange range = manager.getIdRange(associationType);
		
		assertEquals(Long.valueOf(submissions.get(submissions.size() - 1).getId()), range.getMaxId());
		
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, new IdRange(Long.valueOf(submissions.get(0).getId()), Long.valueOf(submissions.get(submissions.size() - 1).getId()))
		).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}

	private VerificationSubmission createVerificationSubmission(String userId, int fileNum) {

		VerificationSubmission result = submissionHelper.create(submission -> {
			submission.setCreatedBy(userId);
			submission.setCreatedOn(new Date());

			submission.setAttachments(IntStream.range(0, fileNum).boxed().map(i -> {
				AttachmentMetadata a = new AttachmentMetadata();
				a.setFileName("File_" + i);
				a.setId(utils.generateFileHandle(userId));
				return a;
			}).collect(Collectors.toList()));
		});

		verificationToDelete.add(Long.valueOf(result.getId()));

		return result;
	}

}
