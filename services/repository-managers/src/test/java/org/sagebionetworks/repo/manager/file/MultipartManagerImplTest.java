package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.CreateChunkedFileTokenRequest;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.transfer.TransferManager;



public class MultipartManagerImplTest {
	@Mock
	private SynapseS3Client s3Client;
	
	@Mock
	private FileHandleDao fileHandleDao;
	
	@Mock
	private TransferManager transferManager;
	
	@Mock
	private ProjectSettingsManager projectSettingsManager;
	
	@Mock
	private IdGenerator idGenerator;

	private MultipartManager manager;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		manager = new MultipartManagerImpl();
		ReflectionTestUtils.setField(manager, "s3Client", s3Client);
		ReflectionTestUtils.setField(manager, "fileHandleDao", fileHandleDao);
		ReflectionTestUtils.setField(manager, "transferManager", transferManager);
		ReflectionTestUtils.setField(manager, "projectSettingsManager", projectSettingsManager);
		ReflectionTestUtils.setField(manager, "idGenerator", idGenerator);
	}

	@Test
	public void testCreateChunkedFileUploadToken() {
		CreateChunkedFileTokenRequest ccftr=new CreateChunkedFileTokenRequest();
		ccftr.setFileName("foo.txt");
		Long storageLocationId=101L;
		StorageLocationSetting sls = new S3StorageLocationSetting();
		when(projectSettingsManager.getStorageLocationSetting(storageLocationId)).thenReturn(sls);
		String userId=null;
		ArgumentCaptor<InitiateMultipartUploadRequest> imurCapture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);

		InitiateMultipartUploadResult imur = new InitiateMultipartUploadResult();
		when(s3Client.initiateMultipartUpload(imurCapture.capture())).thenReturn(imur);
		
		// method under test
		manager.createChunkedFileUploadToken(ccftr, storageLocationId, userId);

		assertEquals(CannedAccessControlList.BucketOwnerFullControl, imurCapture.getValue().getCannedACL());

	}

}
