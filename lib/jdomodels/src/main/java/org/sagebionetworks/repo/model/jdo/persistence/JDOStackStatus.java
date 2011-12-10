package org.sagebionetworks.repo.model.jdo.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * This table holds a single row that indicates the status of the entire stack.
 * 
 * @author jmhill
 *
 */
@PersistenceCapable(detachable = "true", table=SqlConstants.TABLE_STACK_STATUS)
public class JDOStackStatus {
	
	/**
	 * This is the only id used by the table.
	 */
	public static final Long STATUS_ID = new Long(0);
	
	public static final String DEFAULT_MESSAGE = "Synapse is ready for both READ and WRITE";

	@PrimaryKey
	private Long id;
	
	@Column(name=SqlConstants.COL_STACK_STATUS_STATUS)
	@Persistent(nullValue = NullValue.EXCEPTION) // cannot be null
	private short status;
	
	@Column(name=SqlConstants.COL_STACK_STATUS_CURRENT_MESSAGE)
	@Persistent(nullValue = NullValue.DEFAULT) // cannot be null
	private String currentMessage;
	
	@Column(name=SqlConstants.COL_STACK_STATUS_PENDING_MESSAGE)
	@Persistent(nullValue = NullValue.DEFAULT) // cannot be null
	private String pendingMessage;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCurrentMessage() {
		return currentMessage;
	}

	public void setCurrentMessage(String currentMessage) {
		this.currentMessage = currentMessage;
	}

	public String getPendingMessage() {
		return pendingMessage;
	}

	public void setPendingMessage(String pendingMessage) {
		this.pendingMessage = pendingMessage;
	}

	public short getStatus() {
		return status;
	}

	public void setStatus(short status) {
		this.status = status;
	}
	
	
}
