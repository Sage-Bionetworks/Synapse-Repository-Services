package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FAVORITE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


/**
 * @author dburdick
 *
 */
public class DBOFavoriteDAOImpl implements FavoriteDAO {
	
	@Autowired
	private TagMessenger tagMessenger;
	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;	
	
	private static final String SELECT_FOR_RANGE_SQL = "SELECT " + COL_FAVORITE_PRINCIPAL_ID +", "+ COL_FAVORITE_NODE_ID 
														+ " FROM " + TABLE_FAVORITE 
														+ " ORDER BY " + COL_FAVORITE_PRINCIPAL_ID + ", "+ COL_FAVORITE_NODE_ID 
														+ " LIMIT :" + LIMIT_PARAM_NAME 
														+ " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_GET_FAVORITES_SQL = "SELECT " + COL_FAVORITE_PRINCIPAL_ID +", "+ COL_FAVORITE_NODE_ID +", "+ COL_FAVORITE_CREATED_ON
															+ " FROM " + TABLE_FAVORITE 
															+ " WHERE " + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID
															+ " ORDER BY " + COL_FAVORITE_PRINCIPAL_ID + ", "+ COL_FAVORITE_NODE_ID 
															+ " LIMIT :" + LIMIT_PARAM_NAME 
															+ " OFFSET :" + OFFSET_PARAM_NAME;
	private static final String COUNT_FAVORITES_SQL = "SELECT COUNT(" + COL_FAVORITE_PRINCIPAL_ID +")"
														+ " FROM " + TABLE_FAVORITE 
														+ " WHERE " + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID;
	private static final String SELECT_FAVORITE_SQL = "SELECT " + COL_FAVORITE_PRINCIPAL_ID +", "+ COL_FAVORITE_NODE_ID +", "+ COL_FAVORITE_CREATED_ON
														+ " FROM " + TABLE_FAVORITE 
														+ " WHERE " + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID
														+ " AND " + COL_FAVORITE_NODE_ID + "= :" + COL_FAVORITE_NODE_ID;
	
	public DBOFavoriteDAOImpl() { }
	
	/**
	 * For Testing
	 * @param tagMessenger
	 * @param basicDao
	 * @param simpleJdbcTemplate
	 */
	public DBOFavoriteDAOImpl(TagMessenger tagMessenger, DBOBasicDao basicDao,
			SimpleJdbcTemplate simpleJdbcTemplate) {
		super();
		this.tagMessenger = tagMessenger;
		this.basicDao = basicDao;
		this.simpleJdbcTemplate = simpleJdbcTemplate;
	}

	@Override
	public Favorite add(Favorite dto) throws DatastoreException,
			InvalidModelException {
		dto.setCreatedOn(new Date());
		DBOFavorite dbo = new DBOFavorite(); 
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		basicDao.createNew(dbo);
		try {
			return getIndividualFavorite(dto.getPrincipalId(), dto.getEntityId());
		} catch (NotFoundException e) {
			throw new DatastoreException("Favorite not added.");
		}
	}

	@Override
	public void remove(String principalId, String entityId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOFavorite.FIELD_COLUMN_ID_PRINCIPAL_ID, principalId);
		param.addValue(DBOFavorite.FIELD_COLUMN_ID_NODE_ID, KeyFactory.stringToKey(entityId));
		basicDao.deleteObjectById(DBOFavorite.class, param);
	}

	@Override
	public PaginatedResults<Favorite> getFavorites(String principalId, int limit,
			int offset) throws DatastoreException, InvalidModelException,
			NotFoundException {
		if(principalId == null) throw new IllegalArgumentException("Principal id can not be null");
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
		// get one page of favorites
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_FAVORITE_PRINCIPAL_ID, principalId);
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<Favorite> favorites = null;
		favorites = simpleJdbcTemplate.query(SELECT_GET_FAVORITES_SQL, new RowMapper<Favorite>() {
			@Override
			public Favorite mapRow(ResultSet rs, int rowNum) throws SQLException {
				Favorite favorite = new Favorite();
				favorite.setPrincipalId(String.valueOf(rs.getLong(COL_FAVORITE_PRINCIPAL_ID)));
				favorite.setEntityId(KeyFactory.keyToString(rs.getLong(COL_FAVORITE_NODE_ID)));
				favorite.setCreatedOn(new Date(rs.getLong(COL_FAVORITE_CREATED_ON)));
				return favorite;
			}		
		}, params);
			
		// return the page of objects, along with the total result count
		PaginatedResults<Favorite> queryResults = new PaginatedResults<Favorite>();
		queryResults.setResults(favorites);
		long totalCount = 0;
		try {
			totalCount = simpleJdbcTemplate.queryForLong(COUNT_FAVORITES_SQL, params);		
		} catch (EmptyResultDataAccessException e) {
			// count = 0
		}
		queryResults.setTotalNumberOfResults(totalCount);
		return queryResults;	
	}

	@Override
	public Favorite getIndividualFavorite(String principalId, String entityId)
			throws DatastoreException, InvalidModelException, NotFoundException {
		if(principalId == null) throw new IllegalArgumentException("Principal id can not be null");
		if(entityId == null) throw new IllegalArgumentException("Entity id can not be null");
		// get one page of favorites
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_FAVORITE_PRINCIPAL_ID, principalId);
		params.addValue(COL_FAVORITE_NODE_ID, KeyFactory.stringToKey(entityId));		
		Favorite favorite = null;
		try {
			favorite = simpleJdbcTemplate.queryForObject(SELECT_FAVORITE_SQL, new RowMapper<Favorite>() {
				@Override
				public Favorite mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					Favorite fav = new Favorite();
					fav.setPrincipalId(String.valueOf(rs.getLong(COL_FAVORITE_PRINCIPAL_ID)));
					fav.setEntityId(KeyFactory.keyToString(rs.getLong(COL_FAVORITE_NODE_ID)));
					fav.setCreatedOn(new Date(rs.getLong(COL_FAVORITE_CREATED_ON)));
					return fav;
				}
			}, params); 
			return favorite;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No favorite found with: principalId="+principalId+", entityId="+entityId);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOFavorite.class);
	}

	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData(
			long offset, long limit, boolean includeDependencies)
			throws DatastoreException {
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
		// get one 'page' of Activities (just their IDs and Etags)
		List<MigratableObjectData> ods = null;
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OFFSET_PARAM_NAME, offset);
			param.addValue(LIMIT_PARAM_NAME, limit);
			ods = simpleJdbcTemplate.query(SELECT_FOR_RANGE_SQL, new RowMapper<MigratableObjectData>() {
				@Override
				public MigratableObjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
					Long principalId = rs.getLong(COL_FAVORITE_PRINCIPAL_ID);
					Long nodeId = rs.getLong(COL_FAVORITE_NODE_ID);
					String id = UserProfileUtils.getFavoriteId(String.valueOf(principalId), String.valueOf(nodeId));
					MigratableObjectData objectData = new MigratableObjectData();
					MigratableObjectDescriptor od = new MigratableObjectDescriptor();
					od.setId(id);
					od.setType(MigratableObjectType.FAVORITE);
					objectData.setId(od);
					objectData.setDependencies(new HashSet<MigratableObjectDescriptor>(0));
					return objectData;
				}
			
			}, param);
		}
		
		// return the 'page' of objects, along with the total result count		
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}

	@Override
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.FAVORITE;
	}


}
