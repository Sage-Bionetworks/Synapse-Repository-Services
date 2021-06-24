package org.sagebionetworks.repo.model.helper;

import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Helper for building FileHandle quickly/easily for testing.
 *
 */
@Service
public class FileHandleObjectHelper implements DaoObjectHelper<S3FileHandle> {

	FileHandleDao fileHandleDao;
	IdGenerator idGenerator;

	@Autowired
	public FileHandleObjectHelper(FileHandleDao fileHandleDao, IdGenerator idGenerator) {
		super();
		this.fileHandleDao = fileHandleDao;
		this.idGenerator = idGenerator;
	}

	@Override
	public S3FileHandle create(Consumer<S3FileHandle> consumer) {
		return createS3(consumer);
	}
	
	public S3FileHandle createS3(Consumer<S3FileHandle> consumer) {
		return createFileHandle(consumer, S3FileHandle.class);
	}
	
	/**
	 * Genetic method to create a FileHandl of any type.
	 * @param <T>
	 * @param consumer
	 * @param clazz
	 * @return
	 */
	public <T extends FileHandle> T createFileHandle(Consumer<T> consumer, Class<T> clazz) {		
		try {
			T fh = clazz.newInstance();
			fh.setContentType("text/plain; charset=UTF-8");
			fh.setContentSize(101L);
			fh.setContentMd5("md5");
			fh.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
			fh.setFileName("foobar.txt");
			fh.setId(this.idGenerator.generateNewId(IdType.FILE_IDS).toString());
			fh.setEtag(UUID.randomUUID().toString());
			if(fh instanceof ExternalFileHandle) {
				((ExternalFileHandle)fh).setExternalURL("http://sagebase.org");
			}
			if(fh instanceof S3FileHandle) {
				((S3FileHandle)fh).setKey("some-key");
			}
			if(fh instanceof GoogleCloudFileHandle) {
				((GoogleCloudFileHandle)fh).setKey("some-key");
			}
			consumer.accept(fh);
			return (T) fileHandleDao.createFile(fh);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	

}
