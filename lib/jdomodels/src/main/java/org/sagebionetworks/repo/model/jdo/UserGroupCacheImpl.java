package org.sagebionetworks.repo.model.jdo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A simple cache for getting user names from user ids and user ids from user names. 
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class UserGroupCacheImpl implements UserGroupCache {
	
	// Get an ID using the name.
	private static final String SQL_GET_ID_FOR_NAME = "SELECT "+SqlConstants.COL_USER_GROUP_ID+" FROM "+SqlConstants.TABLE_USER_GROUP+" WHERE "+SqlConstants.COL_USER_GROUP_NAME+" = ?";

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;
	
	private Map<String, Long> mapNamesToId = Collections.synchronizedMap(new HashMap<String, Long>());
	private Map<Long, String> mapIdToName = Collections.synchronizedMap(new HashMap<Long, String>());

	@Transactional(readOnly = true)
	@Override
	public Long getIdForUserGroupName(String name) throws NotFoundException {
		if(name == null) throw new IllegalArgumentException("Name cannot be null");
		// Check the cache
		Long id = mapNamesToId.get(name);
		if(id == null){
			try{
				id = simpleJdbcTempalte.queryForLong(SQL_GET_ID_FOR_NAME, name);
			}catch(Exception e){
				throw new NotFoundException("Could not find a principal named: "+name);
			}
			mapNamesToId.put(name, id);
			mapIdToName.put(id, name);
		}
		return id;
	}

	@Transactional(readOnly = true)
	@Override
	public String getUserGroupNameForId(Long id) throws NotFoundException {
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		// Check the cache
		String name = mapIdToName.get(id);
		if(name == null){
			try{
				name = simpleJdbcTempalte.queryForObject("SELECT "+SqlConstants.COL_USER_GROUP_NAME+" FROM "+SqlConstants.TABLE_USER_GROUP+" WHERE "+SqlConstants.COL_USER_GROUP_ID+" = ?", String.class, id);
			}catch(Exception e){
				throw new NotFoundException("Could not find a principal named: "+name);
			}
			mapIdToName.put(id, name);
			mapNamesToId.put(name, id);
		}
		return name;
	}

	@Override
	public void delete(Long id) {
		if(id == null) throw new IllegalArgumentException("ID cannot be null");
		String name = mapIdToName.get(id);
		if(name != null){
			mapNamesToId.remove(name);
		}
		mapIdToName.remove(id);
		
	}

}
