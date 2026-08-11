package org.sagebionetworks.grid.workers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Uploads literal file content (e.g. a CSV) as a file handle, for tests that
 * source a RecordSet or View update from a file.
 * <p>
 * Spring-managed (registered as a bean in {@code test-context.xml}); autowire
 * this into an integration test rather than constructing it directly.
 */
public class FileHandleTestUtils {

	@Autowired
	private FileHandleManager fileHandleManager;

	/**
	 * Upload {@code csvContent} as a CSV file handle owned by {@code user}.
	 * 
	 * @return the id of the resulting file handle.
	 */
	public String uploadCsv(UserInfo user, String csvContent) throws IOException {
		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(user.getId().toString(), new Date(),
				csvContent.getBytes(StandardCharsets.UTF_8), "recordset.csv", ContentType.create("text/csv"), null);
		return fileHandle.getId();
	}
}
