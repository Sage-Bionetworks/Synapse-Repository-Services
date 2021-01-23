package org.sagebionetworks.file.worker;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.file.MultipartManagerV2;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

public class MultipartCleanupWorker implements ProgressingRunner {
	
	protected static final long BATCH_SIZE = 10000;
	
	private MultipartManagerV2 multipartManager;
	
	private StackStatusManager stackStatusManager;
	
	private FeatureManager featureManager;
	
	private Logger logger;
	
	@Autowired
	public MultipartCleanupWorker(MultipartManagerV2 multipartManager, StackStatusManager stackStatusManager, FeatureManager featureManager) {
		this.multipartManager = multipartManager;
		this.stackStatusManager = stackStatusManager;
		this.featureManager = featureManager;
	}
	
	@Autowired
	public void configureLogger(LoggerProvider loggerProvider) {
		logger = loggerProvider.getLogger(MultipartCleanupWorker.class.getName());
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		
		if (!featureManager.isFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP)) {
			return;
		}
				
		List<String> expiredUploads = multipartManager.getUploadsModifiedBefore(MultipartManagerV2.EXPIRE_PERIOD_DAYS, BATCH_SIZE);
		
		long startTime = System.currentTimeMillis();
		int processedCount = 0;
		int errorsCount = 0;
		
		for (String uploadId : expiredUploads) {
			
			if (!StatusEnum.READ_WRITE.equals(stackStatusManager.getCurrentStatus().getStatus())) {
				break;
			}
			
			try {
				multipartManager.clearMultipartUpload(uploadId);
			} catch (Throwable e) {
				logger.warn(e.getMessage(), e);
				errorsCount++;
			}
			processedCount++;
		}
	
		logger.info("Processed {} multipart uploads (Errored: {}, Time: {} ms).", processedCount, errorsCount, System.currentTimeMillis() - startTime);
	}

}
