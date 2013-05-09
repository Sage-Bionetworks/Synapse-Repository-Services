package org.sagebionetworks.tool.migration.v3;

import java.util.List;
import java.util.Stack;

import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Stub implementation of synapse for testing.
 * 
 * @author jmhill
 *
 */
public class StubSynapseAdministration implements SynapseAdministrationInt {
	
	Stack<StackStatus> statusHistory;
	
	/**
	 * Create a new stub
	 */
	public StubSynapseAdministration(){
		// Start with a status of read/write
		StackStatus status = new StackStatus();
		status.setCurrentMessage("Synapse is read for read/write");
		status.setStatus(StatusEnum.READ_WRITE);
		statusHistory = new Stack<StackStatus>();
		statusHistory.push(status);
	}

	@Override
	public StackStatus getCurrentStackStatus() throws SynapseException,
			JSONObjectAdapterException {
		return statusHistory.lastElement();
	}

	@Override
	public StackStatus updateCurrentStackStatus(StackStatus updated) throws JSONObjectAdapterException, SynapseException {
		if(updated == null) throw new IllegalArgumentException("StackStatus cannot be null");
		StackStatus status = cloneJsonEntity(updated);
		statusHistory.push(status);
		return status;
	}
	
	/**
	 * Create a clone of a JSONEntity.
	 * 
	 * @param toClone
	 * @return
	 * @throws JSONObjectAdapterException 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends JSONEntity> T cloneJsonEntity(T toClone) throws JSONObjectAdapterException{
		if(toClone == null) throw new IllegalArgumentException("Clone cannot be null");
		// First go to JSON
		String json = EntityFactory.createJSONStringForEntity(toClone);
		return (T) EntityFactory.createEntityFromJSONString(json, toClone.getClass());
	}
	
	/**
	 * Get the full history of status changes made to this stack.
	 * 
	 * @return
	 */
	public Stack<StackStatus> getStatusHistory(){
		return statusHistory;
	}

}
