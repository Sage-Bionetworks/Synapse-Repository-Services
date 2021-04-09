package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequirementFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;

	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;
	
	@Autowired
	private AccessRequirementDAO arDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.AccessRequirementAttachment;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		arDao.clear();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		arDao.clear();
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		List<ManagedACTAccessRequirement> ars = new ArrayList<>();
		
		ars.add(createAR(true));
		ars.add(createAR(false));
		ars.add(createAR(true));
		
		List<ScannedFileHandleAssociation> expected = new ArrayList<>();
		
		for (ManagedACTAccessRequirement ar : ars) {
			ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(ar.getId());
			
			if (ar.getDucTemplateFileHandleId() != null) {
				association.withFileHandleIds(Collections.singleton(Long.valueOf(ar.getDucTemplateFileHandleId())));
			}
			
			expected.add(association);
		}
		
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, range).spliterator(), false
		).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private ManagedACTAccessRequirement createAR(boolean withFileHandle) {
		return managedHelper.create((ar) -> {
			ar.setCreatedBy(user.getId().toString());
			ar.setSubjectIds(Collections.emptyList());
			if (withFileHandle) {
				ar.setDucTemplateFileHandleId(utils.generateFileHandle(user));
			} else {
				ar.setDucTemplateFileHandleId(null);
			}
		});
	}

}
