package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.file.MultipartManagerV2;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.helper.MultipartUploadDBOHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class MultipartCleanupWorkerIntegrationTest {
		
	private static final long WORKER_TIMEOUT = 3 * 60 * 1000;

	@Autowired
	private MultipartUploadDAO multipartUploadDao;
	
	@Autowired
	private FeatureStatusDao featureStatusDao;
	
	@Autowired
	private MultipartUploadDBOHelper helper;
	
	@BeforeEach
	public void before() {
		multipartUploadDao.truncateAll();
		featureStatusDao.setFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP, true);
	}
	
	
	@AfterEach
	public void after() {
		multipartUploadDao.truncateAll();
		featureStatusDao.clear();
	}
	
	@Test
	public void testCleanupWorker() throws Exception {
		
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setFileName("fileName");
		request.setContentMD5Hex("md5");
		request.setFileSizeBytes(PartUtils.MIN_PART_SIZE_BYTES);
		request.setGeneratePreview(false);
		request.setPartSizeBytes(PartUtils.MIN_PART_SIZE_BYTES);
		
		String requestBody = MultipartRequestUtils.createRequestJSON(request);
		
		Date created = Date.from(Instant.now().minus(MultipartManagerV2.EXPIRE_PERIOD_DAYS, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS));
		
		Long uploadId = helper.create(dbo -> {
			dbo.setStartedOn(created);
			dbo.setUpdatedOn(created);
			dbo.setRequestBlob(requestBody.getBytes(StandardCharsets.UTF_8));
		}).getId();
				
		// this should work
		assertNotNull(multipartUploadDao.getUploadStatus(String.valueOf(uploadId)));
		
		TimeUtils.waitFor(WORKER_TIMEOUT, 1000L, () -> {
			
			boolean deleted = false;
			
			try {
				multipartUploadDao.getUploadStatus(String.valueOf(uploadId));
			} catch (NotFoundException e) {
				deleted = true;
			}
			
			return new Pair<>(deleted, null);
		});
		
	}
	
}
