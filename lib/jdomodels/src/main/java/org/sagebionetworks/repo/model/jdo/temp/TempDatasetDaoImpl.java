package org.sagebionetworks.repo.model.jdo.temp;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.JDODatasetUtils;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Temporary implementation of a dataset DAO.
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class TempDatasetDaoImpl implements TempDatasetDao {
	
	@Autowired
	private JdoTemplate jdoTemplate;

	/**
	 * The template does all of the work.
	 */
	@Transactional(readOnly = true)
	@Override
	public Dataset get(String id) {
		JDODataset jdo = (JDODataset) jdoTemplate.getObjectById(JDODataset.class, Long.valueOf(id));
		if(jdo == null) return null;
		return JDODatasetUtils.createFromJDO(jdo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(Dataset toCreate) throws DataAccessException, InvalidModelException {
		// Convert it to a JDO
		JDODataset result = (JDODataset) jdoTemplate.makePersistent(JDODatasetUtils.createFromDTO(toCreate));
		return result.getId().toString();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(String id) {
		JDODataset toDelete = (JDODataset) jdoTemplate.getObjectById(JDODataset.class, Long.valueOf(id));
		if(toDelete == null) return false;
		jdoTemplate.deletePersistent(toDelete);
		return true;
	}

}
