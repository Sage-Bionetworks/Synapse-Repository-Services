package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableStatus;
import org.sagebionetworks.repo.model.dbo.persistence.table.TableStatusUtils;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A very basic DAO to tack table status.
 * 
 * @author John
 *
 */
public class TableStatusDAOImpl implements TableStatusDAO {
	
	@Autowired
	private DBOBasicDao basicDao;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public TableStatus createOrUpdateTableStatus(TableStatus status) {
		if(status == null) throw new IllegalArgumentException("Status cannot be null");
		DBOTableStatus dbo = TableStatusUtils.createDBOFromDTO(status);
		basicDao.createOrUpdate(dbo);
		try {
			return getTableStatus(status.getTableId());
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public TableStatus getTableStatus(String tableId) throws DatastoreException, NotFoundException {
		SqlParameterSource param = new MapSqlParameterSource("tableId", tableId);
		DBOTableStatus dbo =  basicDao.getObjectByPrimaryKey(DBOTableStatus.class, param);
		return TableStatusUtils.createDTOFromDBO(dbo);
	}

}
