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

public class StorageLocationDAOImpl implements StorageLocationDAO {

	private static String DELETE_SQL = "DELETE FROM "
			+ TABLE_STORAGE_LOCATION + " WHERE "
			+ COL_STORAGE_LOCATION_NODE_ID + " = ?";

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;

	@Override
	public void replaceLocationData(StorageLocations locations) throws DatastoreException {

		if (locations == null) {
			return;
		}

		// Delete first
		Long nodeId = locations.getNodeId();
		this.simpleJdbcTempalte.update(DELETE_SQL, nodeId);

		// Then update
		List<DBOStorageLocation> batch = StorageLocationUtils.createBatch(locations);
		this.dboBasicDao.createBatch(batch);
	}
}
