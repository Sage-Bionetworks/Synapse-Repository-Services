package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.model.table.TableStatus;

/**
 * This exception is thrown when a consistent query is run against a table entity
 * where the table index is not ready for query.  It will always include the status of the table index.
 * @author John
 *
 */
public class SynapseTableUnavailableException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int ACCEPTED = HttpStatus.SC_ACCEPTED;
	
	TableStatus status = null;

	public SynapseTableUnavailableException(TableStatus status) {
		super(ACCEPTED);
		this.status = status;
	}

	/**
	 * When a TableUnavilableException is thrown from the server, the status of the table is
	 * always included.
	 * 
	 * @return
	 */
	public TableStatus getStatus() {
		return status;
	}

}
