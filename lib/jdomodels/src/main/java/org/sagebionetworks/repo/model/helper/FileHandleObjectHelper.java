package org.sagebionetworks.repo.model.helper;

import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
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
		Long id = this.idGenerator.generateNewId(IdType.FILE_IDS);
		S3FileHandle fh = new S3FileHandle();
		fh.setBucketName("some-bucket");
		fh.setKey("some-key");
		fh.setContentType("text/plain; charset=UTF-8");
		fh.setContentSize(101L);
		fh.setContentMd5("md5");
		fh.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		fh.setFileName("foobar.txt");
		fh.setId(id.toString());
		fh.setEtag(UUID.randomUUID().toString());
		fh.setIsPreview(false);
		consumer.accept(fh);
		return (S3FileHandle) fileHandleDao.createFile(fh);
	}

}
