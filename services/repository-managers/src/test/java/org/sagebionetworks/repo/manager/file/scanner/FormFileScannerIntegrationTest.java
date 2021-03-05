package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FormFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FormDao formDao;
		
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.FormData;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		formDao.truncateAll();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		formDao.truncateAll();
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		FormGroup group1 = formDao.createFormGroup(user.getId(), "TestGroup1");
		FormGroup group2 = formDao.createFormGroup(user.getId(), "TestGroup2");
		
		FormData f1 = createForm(group1);
		FormData f2 = createForm(group2);
		FormData f3 = createForm(group2);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation(Long.valueOf(f1.getFormDataId()), Long.valueOf(f1.getDataFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(f2.getFormDataId()), Long.valueOf(f2.getDataFileHandleId())),
			new ScannedFileHandleAssociation(Long.valueOf(f3.getFormDataId()), Long.valueOf(f3.getDataFileHandleId()))
		);
	
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(manager.scanRange(associationType, range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private FormData createForm(FormGroup group) {
		return formDao.createFormData(user.getId(), group.getGroupId(), "TestForm_" + UUID.randomUUID().toString(), utils.generateFileHandle(user));
	}

}
