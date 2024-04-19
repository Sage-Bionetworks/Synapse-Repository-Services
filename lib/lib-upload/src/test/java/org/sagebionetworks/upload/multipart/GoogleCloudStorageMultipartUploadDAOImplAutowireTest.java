package org.sagebionetworks.upload.multipart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.dbo.file.google.AsyncGooglePartRangeDao;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.cloud.storage.Blob;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context-mock-google.xml" })
public class GoogleCloudStorageMultipartUploadDAOImplAutowireTest {

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private GoogleCloudStorageMultipartUploadDAOImpl googleCloudStorageMultipartUploadDAO;

	@Autowired
	private AsyncGooglePartRangeDao asyncDAO;
	
	@Autowired
	private TransactionTemplate readCommitedTransactionTemplate;
	
	// Note: This will be injected with a mock version of this client.
	@Autowired
	private SynapseGoogleCloudStorageClient googleCloudStorageClient;

	@Autowired
	private AsyncGoogleMultipartUploadDao asyncGoogleMultipartUploadDAO;

	private Blob mockBlob = Mockito.mock(Blob.class);

	@BeforeEach
	public void before() {
		multipartUploadDAO.truncateAll();
	}

	@Test
	public void testValidateAndAddPartWithMultipleThreads() throws Exception {

		int numberOfThreads = 50;
		int numberOfParts = 1000;
		long partSize = 100L;

		CompositeMultipartUploadStatus status = multipartUploadDAO.createUploadStatus(new CreateMultipartRequest()
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()).setHash("ahash").setRequestBody("body")
				.setUploadType(UploadType.GOOGLECLOUDSTORAGE).setUploadToken("uploadToken").setBucket("some.bucket")
				.setKey("some.key").setNumberOfParts(numberOfParts).setPartSize(partSize));

		CloudServiceMultipartUploadDAO dao = asyncGoogleMultipartUploadDAO;

		String md5 = "md5";
		String md5Base64 = Base64.encodeBase64String(md5.getBytes("UTF-8"));
		String hexMd5 = Hex.encodeHexString(md5.getBytes("UTF-8"));
		when(mockBlob.getMd5()).thenReturn(md5Base64);
		when(googleCloudStorageClient.getObject(any(), any())).thenReturn(mockBlob);
		when(googleCloudStorageClient.composeObjects(any(), any(), any())).thenReturn(mockBlob);

		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<Void>> futures = new ArrayList<>();

		for (int i = 1; i < numberOfParts; i++) {
			AddPartRequest p = new AddPartRequest().setUploadId(status.getMultipartUploadStatus().getUploadId())
					.setBucket("some.bucket").setKey("some.key").setPartKey("some.key/" + i).setPartMD5Hex(hexMd5).setPartNumber(i)
					.setTotalNumberOfParts(numberOfParts);
			futures.add(executorService.submit(() -> {
				readCommitedTransactionTemplate.executeWithoutResult(c->{
					// call under test
					dao.validateAndAddPart(p);
					multipartUploadDAO.addPartToUpload(p.getUploadId(),(int) p.getPartNumber() , p.getPartMD5Hex());
				});
				return null;
			}));
		}

		for (Future<Void> f : futures) {
			f.get();
		}

		readCommitedTransactionTemplate.executeWithoutResult(c->{
			// call under test
			dao.completeMultipartUpload(new CompleteMultipartRequest()
					.setUploadId(Long.parseLong(status.getMultipartUploadStatus().getUploadId())).setBucket("some.bucket")
					.setKey("some.key").setNumberOfParts((long) numberOfParts));
		});

		verify(googleCloudStorageClient, times(numberOfParts-2)).composeObjects(any(), any(), any());
		verify(googleCloudStorageClient, times((numberOfParts-2)*2)).deleteObject(any(), any());
	}
}