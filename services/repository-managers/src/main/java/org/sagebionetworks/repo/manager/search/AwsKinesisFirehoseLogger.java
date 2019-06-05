package org.sagebionetworks.repo.manager.search;

public interface AwsKinesisFirehoseLogger {

	void log(AwsKinesisLogRecord logRecord);
}
