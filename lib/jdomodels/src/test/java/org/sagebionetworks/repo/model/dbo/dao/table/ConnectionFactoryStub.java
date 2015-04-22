package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.sagebionetworks.collections.Maps2;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.CurrentVersionCacheDao;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ConnectionFactoryStub implements ConnectionFactory {

	public Map<Long, CurrentVersionCacheDao> currentVersionCacheDaos = Maps2.createSupplierHashMap(new Supplier<CurrentVersionCacheDao>() {
		@Override
		public CurrentVersionCacheDao get() {
			return new CurrentVersionCacheDaoStub();
		}
	});

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
	public CurrentVersionCacheDao getCurrentVersionCacheConnection(Long tableId) {
		CurrentVersionCacheDao currentRowCacheDao = currentVersionCacheDaos.get(tableId);
		((CurrentVersionCacheDaoStub) currentRowCacheDao).isEnabled = isEnabled;
		return currentRowCacheDao;
	}

	@Override
	public CurrentRowCacheDao getCurrentRowCacheConnection(Long tableId) {
		CurrentRowCacheDao currentRowCacheDao = currentRowCacheDaos.get(tableId);
		((CurrentRowCacheDaoStub) currentRowCacheDao).isEnabled = isEnabled;
		return currentRowCacheDao;
	}

	@Override
	public Iterable<CurrentVersionCacheDao> getCurrentVersionCacheConnections() {
		return Lists.<CurrentVersionCacheDao> newArrayList(currentVersionCacheDaos.values());
	}

	@Override
	public void dropAllTablesForAllConnections() {
	}
}
