package org.sagebionetworks.repo.model.jdo.temp;

import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.JDOAnnotatable;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
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
	public JDODataset get(String id) {
		return  jdoTemplate.getObjectById(JDODataset.class, Long.valueOf(id));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(JDODataset toCreate) throws DataAccessException, InvalidModelException {
		// Make sure it has annoatations
		if(toCreate.getAnnotations() == null){
			toCreate.setAnnotations(JDOAnnotations.newJDOAnnotations());
		}
		// Convert it to a JDO
		JDODataset result = (JDODataset) jdoTemplate.makePersistent(toCreate);
		return ""+result.getId();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean delete(String id) {
		JDODataset toDelete = (JDODataset) jdoTemplate.getObjectById(JDODataset.class, Long.valueOf(id));
		if(toDelete == null) return false;
		jdoTemplate.deletePersistent(toDelete);
		return true;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(Class<? extends JDOAnnotatable> ownerClass, String datasetid, JDOAnnotations newAnnoations) {
		if(datasetid == null) throw new IllegalArgumentException("Dataset id cannot be null");
		if(newAnnoations == null) throw new IllegalArgumentException("Annotations cannot be null");
		// Get the datasets
		Long dsId = Long.valueOf(datasetid);
		JDOAnnotatable able = jdoTemplate.getObjectById(ownerClass, dsId);
		if(able == null) throw new IllegalArgumentException("Cannot find a dataset with id: "+datasetid);
		able.setAnnotations(newAnnoations);
	}


	@Transactional(readOnly = true)
	@Override
	public JDOAnnotations getAnnotations(Class<? extends JDOAnnotatable> ownerClass, String id) {
		if(ownerClass == null) throw new IllegalArgumentException("Owner class cannot be null");
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		JDOAnnotatable able = jdoTemplate.getObjectById(ownerClass, Long.parseLong(id));
		if(able == null) throw new IllegalArgumentException("Cannot find JDOAnnotatable for id: "+id);
		// By calling each get we can enusre the data is loaded
		JDOAnnotations results = able.getAnnotations();
		results.toString();	
		return results;
	}


}
