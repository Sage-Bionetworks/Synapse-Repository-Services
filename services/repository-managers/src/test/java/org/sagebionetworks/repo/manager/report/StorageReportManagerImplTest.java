package org.sagebionetworks.repo.manager.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.csv.CSVWriterStream;

@RunWith(MockitoJUnitRunner.class)
public class StorageReportManagerImplTest {

	@Mock
	private ConnectionFactory mockConnectionFactory;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private TableIndexDAO mockTableIndexDao;
	@Mock
	private CSVWriterStream mockCsvWriter;

	@InjectMocks
	StorageReportManagerImpl storageReportManager;

	private static final UserInfo adminUser = new UserInfo(true);
	private static final DownloadStorageReportRequest request = new DownloadStorageReportRequest();

	private static final String projectId = "syn123";
	private static final String projectName = "Project Name";
	private static final Long projectSize = 123456L;
	private static SynapseStorageProjectStats projectStats;


	@Before
	public void before() {
		adminUser.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		when(mockConnectionFactory.getFirstConnection()).thenReturn(mockTableIndexDao);
		when(mockAuthorizationManager.isReportTeamMemberOrAdmin(adminUser)).thenReturn(true);

		projectStats = new SynapseStorageProjectStats();
		projectStats.setId(projectId);
		projectStats.setProjectName(projectName);
		projectStats.setSizeInBytes(projectSize);
	}

	@Test
	public void writeStorageReportAllProjects() {
		request.setReportType(StorageReportType.ALL_PROJECTS);
		doAnswer(invocation -> {
			Callback<SynapseStorageProjectStats> callback = invocation.getArgument(1);
			callback.invoke(projectStats);
			return null;
		}).when(mockTableIndexDao).streamSynapseStorageStats(any(), any());
		// Call under test
		storageReportManager.writeStorageReport(adminUser, request, mockCsvWriter);
		// Verify that the CSV had the header and data written in order;
		String[] expectedHeader = {"projectId", "projectName", "sizeInBytes"};
		String[] expectedData = {"syn" + projectId, projectName, projectSize.toString()};
		InOrder orderVerifier = Mockito.inOrder(mockCsvWriter);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedHeader);
		orderVerifier.verify(mockCsvWriter).writeNext(expectedData);
	}

	@Test
	public void writeStorageReportNullReportType() {
		request.setReportType(null);
		doAnswer(invocation -> {
			Callback<SynapseStorageProjectStats> callback = invocation.getArgument(1);
			callback.invoke(projectStats);
			return null;
		}).when(mockTableIndexDao).streamSynapseStorageStats(any(), any());
		// Call under test
		storageReportManager.writeStorageReport(adminUser, request, mockCsvWriter);
		// Verify that the CSV had the header and data written in order;
		String[] expectedHeader = {"projectId", "projectName", "sizeInBytes"};
		String[] expectedData = {"syn" + projectId, projectName, projectSize.toString()};
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
