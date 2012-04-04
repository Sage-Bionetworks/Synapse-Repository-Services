package org.sagebionetworks.repo.model.dbo.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPreviewBlob;
import org.sagebionetworks.repo.model.image.ImagePreviewUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOPreviewBlobDboImpl implements DBOPreviewBlobDao{
	
	/**
	 * Load this from the config.
	 */
	private static final int MAX_PREVIE_PIXELS = StackConfiguration.getMaximumPreivewPixels();

	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createNewPreview(InputStream in, Long owner, Long token) throws IOException, DatastoreException {
		if(in == null) throw new IllegalArgumentException("The input stream cannot be null");
		if(owner == null) throw new IllegalArgumentException("The owner cannot be null");
		if(token == null) throw new IllegalArgumentException("The token ID cannot be null");
		// first read the image
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImagePreviewUtils.createPreviewImage(in, MAX_PREVIE_PIXELS, out);
		DBOPreviewBlob preview = new DBOPreviewBlob();
		preview.setOwnerId(owner);
		preview.setTokenId(token);
		preview.setPreviewBlob(out.toByteArray());
		// Save it to the DB
		dboBasicDao.createNew(preview);
	}

	@Transactional(readOnly = true)
	@Override
	public byte[] getPreview(Long owner, Long token) throws DatastoreException, NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("ownerId", owner);
		params.addValue("tokenId", token);
		DBOPreviewBlob preview = dboBasicDao.getObjectById(DBOPreviewBlob.class, params);
		return preview.getPreviewBlob();
	}

}
