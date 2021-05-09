package org.sagebionetworks.repo.model.helper;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.file.DBOMultipartUpload;
import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MultipartUploadDBOHelper implements DaoObjectHelper<DBOMultipartUpload> {
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Override
	public DBOMultipartUpload create(Consumer<DBOMultipartUpload> consumer) {
		
		DBOMultipartUpload dbo = new DBOMultipartUpload();

		Date created = new Date();
		
		dbo.setId(idGenerator.generateNewId(IdType.MULTIPART_UPLOAD_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setRequestHash("some hash");
		dbo.setStartedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		dbo.setState(MultipartUploadState.UPLOADING.name());
		dbo.setRequestBlob("request body".getBytes(StandardCharsets.UTF_8));
		dbo.setStartedOn(created);
		dbo.setUpdatedOn(created);
		dbo.setUploadToken("some token");
		dbo.setUploadType(UploadType.S3.toString());
		dbo.setBucket("bucket");
		dbo.setKey("key");
		dbo.setNumberOfParts(10);
		dbo.setPartSize(PartUtils.MIN_PART_SIZE_BYTES);
		dbo.setRequestType(MultiPartRequestType.UPLOAD.name());
		
		consumer.accept(dbo);
		
		return basicDao.createNew(dbo);
		
	}

}
