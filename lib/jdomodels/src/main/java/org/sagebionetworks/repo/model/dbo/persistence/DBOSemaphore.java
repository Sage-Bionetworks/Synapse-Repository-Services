package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOSemaphore implements DatabaseObject<DBOSemaphore> {

	@Override
	public TableMapping<DBOSemaphore> getTableMapping() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Integer typeId;
	private String token;
	private Long expiration;

}
