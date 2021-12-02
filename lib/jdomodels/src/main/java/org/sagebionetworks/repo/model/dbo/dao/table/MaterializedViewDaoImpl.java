package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Collections;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.stereotype.Repository;

@Repository
public class MaterializedViewDaoImpl implements MaterializedViewDao {

	public MaterializedViewDaoImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	@WriteTransaction
	public void clearSourceTables(IdAndVersion viewId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	@WriteTransaction
	public void addSourceTables(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<IdAndVersion> getSourceTables(IdAndVersion viewId) {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public void deleteSourceTables(IdAndVersion viewId, Set<IdAndVersion> sourceTableIds) {
		// TODO Auto-generated method stub
		
	}

}
