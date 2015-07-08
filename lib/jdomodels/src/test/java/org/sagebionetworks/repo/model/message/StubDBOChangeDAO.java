package org.sagebionetworks.repo.model.message;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * A simple stub implementation of the DBOChangeDAO used for unit testing.
 * @author John
 *
 */
public class StubDBOChangeDAO implements DBOChangeDAO {
	
	Map<String, ChangeMessage> map = new HashMap<String, ChangeMessage>();
	long changeCount = 0l;

	@Override
	public ChangeMessage replaceChange(ChangeMessage change) {
		if(change == null) throw new IllegalArgumentException("Change cannot be null");
		if(change.getObjectId() == null) throw new IllegalArgumentException("ObjectId cannot be null");
		// Cone the message 
		String json;
		try {
			json = EntityFactory.createJSONStringForEntity(change);
			ChangeMessage clone = EntityFactory.createEntityFromJSONString(json, ChangeMessage.class);
			// Set the id and the timestamp just like the DB will do
			clone.setChangeNumber(changeCount++);
			clone.setTimestamp(new Date(System.currentTimeMillis()/1000));
			map.put(clone.getObjectId(), clone);
			return clone;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public List<ChangeMessage> replaceChange(List<ChangeMessage> batch) {
		List<ChangeMessage> newList = new LinkedList<ChangeMessage>();
		for(ChangeMessage message: batch){
			ChangeMessage clone = replaceChange(message);
			newList.add(clone);
		}
		return newList;
	}

	@Override
	public long getCurrentChangeNumber() {
		return changeCount;
	}

	@Override
	public void deleteChange(Long objectId, ObjectType type) {
		map.remove(objectId);
	}

	@Override
	public void deleteAllChanges() {
		map.clear();
		
	}

	@Override
	public List<ChangeMessage> listChanges(long greaterOrEqualChangeNumber,	ObjectType type, long limit) {
		return new LinkedList<ChangeMessage>(map.values());
	}


	@Override
	public List<ChangeMessage> listUnsentMessages(long limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMinimumChangeNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<ChangeMessage> listUnsentMessages(long lowerBound,
			long upperBound, Timestamp time) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerMessageProcessed(long changeNumber, String queueName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ChangeMessage> listNotProcessedMessages(String queueName,
			long limit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerMessageSent(ChangeMessage message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerMessageSent(ObjectType type, List<ChangeMessage> batch) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean checkUnsentMessageByCheckSumForRange(long lowerBound,
			long upperBound) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Long getMaxSentChangeNumber(Long lessThanOrEqual) {
		// TODO Auto-generated method stub
		return null;
	}

}
