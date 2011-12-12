package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.ForeignKey;
import javax.jdo.annotations.ForeignKeyAction;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * This bit is used to force the termination of a backup/restore job.
 * @author John
 *
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_BACKUP_TERMINATE)
public class JDODaemonTerminate {
	
	@PrimaryKey
	@Persistent (nullValue = NullValue.EXCEPTION) //cannot be null
	@Column(name=SqlConstants.COL_BACKUP_TERM_OWNER)
	@ForeignKey(name="BACKUP_OWNER_FK", deleteAction=ForeignKeyAction.CASCADE)
	private JDODaemonStatus owner;
	
	@Column(name=SqlConstants.COL_BACKUP_FORCE_TERMINATION)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private Boolean forceTerminate;

	public JDODaemonStatus getOwner() {
		return owner;
	}

	public void setOwner(JDODaemonStatus owner) {
		this.owner = owner;
	}

	public Boolean getForceTerminate() {
		return forceTerminate;
	}

	public void setForceTerminate(Boolean forceTerminate) {
		this.forceTerminate = forceTerminate;
	}
	
	

}
