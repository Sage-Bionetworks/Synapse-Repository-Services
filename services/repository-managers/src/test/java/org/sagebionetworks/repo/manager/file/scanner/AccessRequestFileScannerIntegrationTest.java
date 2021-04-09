package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequestFileScannerIntegrationTest {
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;

	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;
	
	@Autowired
	private DaoObjectHelper<ResearchProject> researchHelper;
	
	@Autowired
	private DaoObjectHelper<UserGroup> userGroupHelper;

	@Autowired
	private DaoObjectHelper<Request> requestHelper;
		
	@Autowired
	private ResearchProjectDAO researchProjectDao;
	
	@Autowired
	private AccessRequirementDAO arDao;
	
	@Autowired
	private RequestDAO requestDao;
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.DataAccessRequestAttachment;
	
	private AccessRequirement ar;
	private ResearchProject rp;
	
	private UserInfo user;
	private List<String> usersToDelete;
	 
	@BeforeEach
	public void before() {
		usersToDelete = new ArrayList<>();
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		arDao.clear();
		fileHandleDao.truncateTable();
		
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		ar = managedHelper.create(ar -> {
			ar.setCreatedBy(user.getId().toString());
			ar.setSubjectIds(Collections.emptyList());
		});
		
		rp = researchHelper.create(r -> {
			r.setCreatedBy(user.getId().toString());
			r.setAccessRequirementId(ar.getId().toString());
		});
		
	}
	
	@AfterEach
	public void after() {
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		arDao.clear();
		fileHandleDao.truncateTable();
		
		usersToDelete.forEach(userGroupDao::delete);
	}
	
	@Test
	public void testScanner() {
		List<RequestInterface> request = new ArrayList<>();
		
		request.add(createRequest(1));
		request.add(createRequest(0));
		request.add(createRequest(2));
		
		List<ScannedFileHandleAssociation> expected = request.stream().map(r -> {
			ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(Long.valueOf(r.getId()));
			
			Set<String> fileHandles = new HashSet<>();
			
			fileHandles.add(r.getIrbFileHandleId());
			fileHandles.add(r.getDucFileHandleId());
			
			if (r.getAttachments() != null) {
				fileHandles.addAll(r.getAttachments());
			}
			
			association.withFileHandleIds(fileHandles.stream().map(id -> Long.valueOf(id)).collect(Collectors.toSet()));

			return association;
		}).collect(Collectors.toList());
		

		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, range).spliterator(), false
		).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	private RequestInterface createRequest(int attachmentsNum) {
		String userId = userGroupHelper.create((u) -> {}).getId();
		
		return requestHelper.create(request -> {
			request.setCreatedBy(userId);
			request.setModifiedBy(userId);
			request.setAccessRequirementId(ar.getId().toString());
			request.setResearchProjectId(rp.getId());
			request.setDucFileHandleId(utils.generateFileHandle(userId));
			request.setIrbFileHandleId(utils.generateFileHandle(userId));
					
			request.setAttachments(IntStream.range(0, attachmentsNum).boxed()
				.map(i -> utils.generateFileHandle(userId)).collect(Collectors.toList())	
			);
		});
		
	}
}
