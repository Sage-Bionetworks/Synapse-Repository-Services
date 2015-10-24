package org.sagebionetworks.repo.model.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronization;

import com.google.common.collect.Maps;

/**
 * A simple stub implementation of TransactionSynchronizationProxy for testing.
 * @author jmhill
 *
 */
public class TransactionSynchronizationProxyStub implements TransactionSynchronizationProxy {
	
	Map<String, Map<?, ?>> map = Maps.newHashMap();
	 List<TransactionSynchronization> list = new ArrayList<TransactionSynchronization>();

	@Override
	public boolean isSynchronizationActive() {
		return true;
	}

	@Override
	public void bindResource(String key, Map<?, ?> value) {
		map.put(key, value);
	}

	@Override
	public Map<?, ?> getResource(String key) {
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
