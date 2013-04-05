package org.sagebionetworks.repo.model.dbo.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * This DAO can be used for any 
 * @author John
 *
 */
@SuppressWarnings("rawtypes")
public class MigatableTableDAOImpl implements MigatableTableDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	/**
	 * Injected via Spring
	 */
	private List<DatabaseObject> databaseObjectRegister;
	
	/**
	 * We cache the mapping for each object type.
	 */
	private Map<Class<? extends DatabaseObject>, TableMapping> classToMapping = new HashMap<Class<? extends DatabaseObject>, TableMapping>();
	
	
	/**
	 * Injected via Spring
	 * @param databaseObjectRegister
	 */
	public void setDatabaseObjectRegister(List<DatabaseObject> databaseObjectRegister) {
		this.databaseObjectRegister = databaseObjectRegister;
	}

	/**
	 * Called when this bean is ready.
	 */
	public void initialize(){
		// Make sure we have a table for all registered objects
		if(databaseObjectRegister == null) throw new IllegalArgumentException("databaseObjectRegister bean cannot be null");
		// Create the schema for each 
		for(DatabaseObject dbo: databaseObjectRegister){
			TableMapping mapping = dbo.getTableMapping();
			// Build up the SQL cache.
			
			this.classToMapping.put(mapping.getDBOClass(), dbo.getTableMapping());
		}
	}
	
	@Override
	public long getCount() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public List<RowMetadata> listDeltaRowMetadata(List<String> idList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends DatabaseObject<T>> List<T> createListToBackup(Class<? extends T> clazz, List<String> idList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends DatabaseObject<T>> int batchCreateOrUpdateRows(List<T> toRestore) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T extends DatabaseObject<?>> QueryResults<RowMetadata> listRowMetadata(
			Class<? extends T> clazz, long limit, long offest) {
		// TODO Auto-generated method stub
		return null;
	}

}
