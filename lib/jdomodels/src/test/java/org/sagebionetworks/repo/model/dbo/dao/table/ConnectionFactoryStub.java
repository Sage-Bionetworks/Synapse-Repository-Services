package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.sagebionetworks.collections.Maps2;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ConnectionFactoryStub implements ConnectionFactory {

	public Map<Long, CurrentRowCacheDao> currentRowCacheDaos = Maps2.createSupplierHashMap(new Supplier<CurrentRowCacheDao>() {
		@Override
		public CurrentRowCacheDao get() {
			return new CurrentRowCacheDaoStub();
		}
	});

	boolean isEnabled = false;

	@Override
	public TableIndexDAO getConnection(String tableId) {
		throw new NotImplementedException();
	}

	@Override
	public CurrentRowCacheDao getCurrentRowCacheConnection(Long tableId) {
		CurrentRowCacheDao currentRowCacheDao = currentRowCacheDaos.get(tableId);
		((CurrentRowCacheDaoStub) currentRowCacheDao).isEnabled = isEnabled;
		return currentRowCacheDao;
	}

	@Override
	public Iterable<CurrentRowCacheDao> getCurrentRowCacheConnections() {
		return Lists.<CurrentRowCacheDao> newArrayList(currentRowCacheDaos.values());
	}

	@Override
	public void dropAllTablesForAllConnections() {
	}
}
