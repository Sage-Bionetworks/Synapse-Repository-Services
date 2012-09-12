package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;

public class StorageLocationDAOImpl implements StorageLocationDAO {

	private static String DELETE_SQL = "DELETE FROM "
			+ TABLE_STORAGE_LOCATION + " WHERE "
			+ COL_STORAGE_LOCATION_NODE_ID + " = ?";

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;

	@Autowired
	private AmazonS3 amazonS3Client;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void replaceLocationData(StorageLocations locations)
			throws DatastoreException {

		if (locations == null) {
			return;
		}

		// Delete first
		Long nodeId = locations.getNodeId();
		this.simpleJdbcTempalte.update(DELETE_SQL, nodeId);

		// Then update
		try {
			List<DBOStorageLocation> batch = StorageLocationUtils.createBatch(
					locations, amazonS3Client);
			this.dboBasicDao.createBatch(batch);
		} catch (AmazonClientException e) {
			throw new DatastoreException(e);
		}
	}
}
