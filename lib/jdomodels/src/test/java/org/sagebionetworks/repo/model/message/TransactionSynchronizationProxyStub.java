package org.sagebionetworks.repo.model.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * A simple stub implementation of TransactionSynchronizationProxy for testing.
 * @author jmhill
 *
 */
public class TransactionSynchronizationProxyStub implements TransactionSynchronizationProxy {
	
	 Map<String, Map<String, ChangeMessage>> map = new HashMap<String, Map<String,ChangeMessage>>();
	 List<TransactionSynchronization> list = new ArrayList<TransactionSynchronization>();

	@Override
	public boolean isSynchronizationActive() {
		return true;
	}

	@Override
	public void bindResource(String key, Map<String, ChangeMessage> value) {
		map.put(key, value);
	}

	@Override
	public Map<String, ChangeMessage> getResource(String key) {
		return map.get(key);
	}

	@Override
	public List<TransactionSynchronization> getSynchronizations() {
		return list;
	}

	@Override
	public void registerSynchronization(TransactionSynchronization synchronizationHandler) {
		list.add(synchronizationHandler);
	}

	@Override
	public void unbindResourceIfPossible(String key) {
		map.remove(key);
	}

}
