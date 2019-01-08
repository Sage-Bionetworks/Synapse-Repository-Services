package org.sagebionetworks.repo.manager.report.doi;

import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.report.StorageReportManagerImpl;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class StorageReportManagerImplTest {

	private StorageReportManagerImpl storageReportManager;


	@Mock
	private ConnectionFactory mockConnectionFactory;

	@Mock
	private TableIndexDAO mockTableIndexDao;

	@Mock
	private AuthorizationManager mockAuthorizationManager;

	@Mock
	private CSVWriterStream mockCsvWriter;

	private static final UserInfo adminUser = new UserInfo(true);
	private static final DownloadStorageReportRequest request = new DownloadStorageReportRequest();

	private static final String projectId = "syn123";
	private static final String projectName = "Project Name";
	private static final Long projectSize = 123456L;
	private static SynapseStorageProjectStats projectStats;


	@Before
	public void before() {
		adminUser.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		storageReportManager = new StorageReportManagerImpl();
		ReflectionTestUtils.setField(storageReportManager, "connectionFactory", mockConnectionFactory);
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDao);
		ReflectionTestUtils.setField(storageReportManager, "authorizationManager", mockAuthorizationManager);
		when(mockAuthorizationManager.isReportTeamMemberOrAdmin(adminUser)).thenReturn(true);

		projectStats = new SynapseStorageProjectStats();
		projectStats.setId(projectId);
		projectStats.setProjectName(projectName);
		projectStats.setSizeInBytes(projectSize);
	}

	@Test
	public void writeStorageReportAllProjects() {
		request.setReportType(StorageReportType.ALL_PROJECTS);
		when(mockTableIndexDao.getSynapseStorageStats()).thenReturn(Collections.singletonList(projectStats));
		// Call under test
		storageReportManager.writeStorageReport(adminUser, request, mockCsvWriter);
		// Verify that the CSV had the header and data written in order;
		String[] expectedHeader = {"projectId", "projectName", "sizeInBytes"};
		String[] expectedData = {projectId, projectName, projectSize.toString()};
		InOrder orderVerifier = Mockito.inOrder(mockCsvWriter);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedHeader);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedData);
	}

	@Test
	public void writeStorageReportNullReportType() {
		request.setReportType(null);
		// Behavior should be the same as ALL_PROJECTS
		when(mockTableIndexDao.getSynapseStorageStats()).thenReturn(Collections.singletonList(projectStats));
		// Call under test
		storageReportManager.writeStorageReport(adminUser, request, mockCsvWriter);
		// Verify that the CSV had the header and data written in order;
		String[] expectedHeader = {"projectId", "projectName", "sizeInBytes"};
		String[] expectedData = {projectId, projectName, projectSize.toString()};
		InOrder orderVerifier = Mockito.inOrder(mockCsvWriter);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedHeader);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedData);
	}


	@Test(expected = UnauthorizedException.class)
	public void writeStorageReportUnauthorized() {
		when(mockAuthorizationManager.isReportTeamMemberOrAdmin(adminUser)).thenReturn(false);
		// Call under test
		storageReportManager.writeStorageReport(adminUser, request, mockCsvWriter);
	}
}
