package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_BACKUP_STATUS)
public class JDOBackupStatus {
	
	public enum STATUS {
		IN_PROGRESS,
		FAILED,
		COMPLETED
	}
	
	@Column(name=SqlConstants.COL_BACKUP_ID)
	@PrimaryKey
	private Long id;
	
	@Column(name=SqlConstants.COL_BACKUP_STATUS)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private String status;

}
