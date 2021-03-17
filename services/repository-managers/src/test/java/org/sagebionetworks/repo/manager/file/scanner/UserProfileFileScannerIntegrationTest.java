package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private DaoObjectHelper<UserProfile> userProfileHelper;
		
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.UserProfileAttachment;
	
	private UserInfo user;
	
	private List<String> usersToDelete;
	
	@BeforeEach
	public void before() {
		usersToDelete = new ArrayList<>();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		usersToDelete.forEach(userGroupDao::delete);
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {

		UserProfile u1 = createUserProfile(true);
		UserProfile u2 = createUserProfile(true);
		
		createUserProfile(false);
		
		UserProfile u4 = createUserProfile(true);
		
		// Call under test
		IdRange range = manager.getIdRange(associationType);
		
		assertNotNull(range);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation(Long.valueOf(u1.getOwnerId()), Long.valueOf(u1.getProfilePicureFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(u2.getOwnerId()), Long.valueOf(u2.getProfilePicureFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(u4.getOwnerId()), Long.valueOf(u4.getProfilePicureFileHandleId()))
		);
				
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, new IdRange(Long.valueOf(u1.getOwnerId()), Long.valueOf(u4.getOwnerId()))
		).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	@Test
	private UserProfile createUserProfile(boolean withProfilePicture) {
		return userProfileHelper.create(up -> {
			if (withProfilePicture) {
				up.setProfilePicureFileHandleId(utils.generateFileHandle(user));
			}	
		});
	}

}
