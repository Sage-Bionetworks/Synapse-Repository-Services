package org.sagebionetworks.repo.manager.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.kinesis.CloudSearchDocumentGenerationAwsKinesisLogRecord;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
class AwsKinesisFirehoseLoggerImplTest {

	@Autowired
	AwsKinesisFirehoseLogger logger;

	@Autowired
	StackConfiguration stackConfiguration;

	@Test
	public void testKinesisToS3(){
		System.out.println(stackConfiguration.getStackInstanceNumber());
		AwsKinesisLogRecord record = new CloudSearchDocumentGenerationAwsKinesisLogRecord(1337, ChangeType.CREATE, false, System.currentTimeMillis());
		logger.log(record, "kinesistest-cloudSearchLogger-1VT27TEEVVC72");
	}
}