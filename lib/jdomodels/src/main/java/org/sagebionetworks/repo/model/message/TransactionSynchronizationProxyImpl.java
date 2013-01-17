package org.sagebionetworks.repo.model.message;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This implementation uses the static methods of TransactionSynchronizationManager.
 * 
 * @author jmhill
 *
 */
public class TransactionSynchronizationProxyImpl implements TransactionSynchronizationProxy {

	@Override
	public boolean isSynchronizationActive() {
		return TransactionSynchronizationManager.isSynchronizationActive();
	}

	@Override
	public void bindResource(String key, Map<ChangeMessageKey, ChangeMessage> value) {
		TransactionSynchronizationManager.bindResource(key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<ChangeMessageKey, ChangeMessage> getResource(String key) {
		return (Map<ChangeMessageKey, ChangeMessage>) TransactionSynchronizationManager.getResource(key);
	}

	@Override
	public List<TransactionSynchronization> getSynchronizations() {
		return TransactionSynchronizationManager.getSynchronizations();
	}

	@Override
	public void registerSynchronization(
			TransactionSynchronization synchronizationHandler) {
		TransactionSynchronizationManager.registerSynchronization(synchronizationHandler);
	}

	@Override
	public void unbindResourceIfPossible(String key) {
		TransactionSynchronizationManager.unbindResourceIfPossible(key);
	}

}
