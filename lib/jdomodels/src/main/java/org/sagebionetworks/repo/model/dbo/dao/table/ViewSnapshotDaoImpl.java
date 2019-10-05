package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.ids.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ViewSnapshotDaoImpl implements ViewSnapshotDao {
	
	@Autowired
	IdGenerator idGenerator;

	@Override
	public ViewSnapshot createSnapshot(ViewSnapshot snapshot) {
		// TODO Auto-generated method stub
		return null;
	}

}
