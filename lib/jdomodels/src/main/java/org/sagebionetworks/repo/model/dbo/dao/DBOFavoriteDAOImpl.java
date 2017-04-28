package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FAVORITE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFavorite;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author dburdick
 *
 */
public class DBOFavoriteDAOImpl implements FavoriteDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private IdGenerator idGenerator;

	private static final String TRASH_FOLDER_ID = StackConfiguration.getTrashFolderEntityIdStatic();

	private static final String SELECT_GET_FAVORITES_SQL = "SELECT " + COL_FAVORITE_PRINCIPAL_ID +", "+ COL_FAVORITE_NODE_ID +", "+ COL_FAVORITE_CREATED_ON
															+ " FROM " + TABLE_FAVORITE 
															+ " WHERE " + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID
															+ " ORDER BY " + COL_FAVORITE_NODE_ID 
															+ " LIMIT :" + LIMIT_PARAM_NAME 
															+ " OFFSET :" + OFFSET_PARAM_NAME;
	private static final String COUNT_FAVORITES_SQL = "SELECT COUNT(" + COL_FAVORITE_PRINCIPAL_ID +")"
														+ "FROM "+ TABLE_FAVORITE +" f, "+ TABLE_NODE +" n "
														+ " WHERE f." + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID
														+ " AND f."+ COL_FAVORITE_NODE_ID +" = n."+ COL_NODE_ID +" "
														+ "AND n."+ COL_NODE_PARENT_ID +" <> " + TRASH_FOLDER_ID;
	private static final String SELECT_FAVORITE_SQL = "SELECT " + COL_FAVORITE_PRINCIPAL_ID +", "+ COL_FAVORITE_NODE_ID +", "+ COL_FAVORITE_CREATED_ON
														+ " FROM " + TABLE_FAVORITE 
														+ " WHERE " + COL_FAVORITE_PRINCIPAL_ID +"= :"+ COL_FAVORITE_PRINCIPAL_ID
														+ " AND " + COL_FAVORITE_NODE_ID + "= :" + COL_FAVORITE_NODE_ID;
	private static final String SELECT_FAVORITES_HEADERS_SQL = "SELECT n."+ COL_NODE_ID +", n."+ COL_NODE_NAME 
																+", n."+ COL_NODE_TYPE +", r."+ COL_REVISION_NUMBER +", r."+ COL_REVISION_LABEL +" " +
																"FROM "+ TABLE_FAVORITE +" f, "+ TABLE_NODE +" n, "+ TABLE_REVISION +" r " +
																"WHERE f."+ COL_FAVORITE_PRINCIPAL_ID +" = :"+ COL_FAVORITE_PRINCIPAL_ID +" " +
																"AND f."+ COL_FAVORITE_NODE_ID +" = n."+ COL_NODE_ID +" " +
																"AND n."+ COL_NODE_ID +" = r."+ COL_REVISION_OWNER_NODE +" " +
																"AND n."+ COL_CURRENT_REV +" = r."+ COL_REVISION_NUMBER +" " +
																"AND n."+ COL_NODE_PARENT_ID +" <> " + TRASH_FOLDER_ID;

	
	private static final RowMapper<Favorite> favoriteRowMapper = new RowMapper<Favorite>() {
		@Override
		public Favorite mapRow(ResultSet rs, int rowNum) throws SQLException {
			Favorite favorite = new Favorite();
			favorite.setPrincipalId(String.valueOf(rs.getLong(COL_FAVORITE_PRINCIPAL_ID)));
			favorite.setEntityId(KeyFactory.keyToString(rs.getLong(COL_FAVORITE_NODE_ID)));
			favorite.setCreatedOn(new Date(rs.getLong(COL_FAVORITE_CREATED_ON)));
			return favorite;
		}
	};

	@Override
	public Favorite add(Favorite dto) throws DatastoreException,
			InvalidModelException {
		if(dto == null) throw new IllegalArgumentException("Favotire dto can not be null");
		if(dto.getPrincipalId() == null) throw new IllegalArgumentException("Principal id can not be null");
		if(dto.getEntityId() == null) throw new IllegalArgumentException("Entity Id can not be null");
		DBOFavorite dbo = new DBOFavorite(); 
		dbo.setId(idGenerator.generateNewId(IdType.FAVORITE_ID));
		dbo.setCreatedOn(new Date().getTime());
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		basicDao.createNew(dbo);
		return getIndividualFavorite(dto.getPrincipalId(), dto.getEntityId());
	}

	@Override
	public void remove(String principalId, String entityId) throws DatastoreException {
		if(principalId == null) throw new IllegalArgumentException("Principal id can not be null");
		if(entityId == null) throw new IllegalArgumentException("Entity Id can not be null");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOFavorite.FIELD_COLUMN_ID_PRINCIPAL_ID, principalId);
		param.addValue(DBOFavorite.FIELD_COLUMN_ID_NODE_ID, KeyFactory.stringToKey(entityId));
		basicDao.deleteObjectByPrimaryKey(DBOFavorite.class, param);
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
		favorites = namedJdbcTemplate.query(SELECT_GET_FAVORITES_SQL, params, favoriteRowMapper);
			
		// return the page of objects, along with the total result count
		PaginatedResults<Favorite> queryResults = new PaginatedResults<Favorite>();
		queryResults.setResults(favorites);
		long totalCount = 0;
		try {
			totalCount = namedJdbcTemplate.queryForObject(COUNT_FAVORITES_SQL, params, Long.class);
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
			favorite = namedJdbcTemplate.queryForObject(SELECT_FAVORITE_SQL, params, favoriteRowMapper);
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
	public PaginatedResults<EntityHeader> getFavoritesEntityHeader(
			String principalId, int limit, int offset)
			throws DatastoreException, InvalidModelException, NotFoundException {
		if(principalId == null) throw new IllegalArgumentException("Principal id can not be null");
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
		// get one page of favorites
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_FAVORITE_PRINCIPAL_ID, principalId);
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<EntityHeader> favoritesHeaders = null;
		favoritesHeaders = namedJdbcTemplate.query(SELECT_FAVORITES_HEADERS_SQL, params, new RowMapper<EntityHeader>() {
			@Override
			public EntityHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
				EntityHeader header = new EntityHeader();
				header.setId(KeyFactory.keyToString(rs.getLong(COL_NODE_ID)));
				header.setName(rs.getString(COL_NODE_NAME));
				header.setType(EntityTypeUtils.getEntityTypeClassName(EntityType.valueOf(rs.getString(COL_NODE_TYPE))));
				header.setVersionNumber(rs.getLong(COL_REVISION_NUMBER));
				header.setVersionLabel(rs.getString(COL_REVISION_LABEL));
				return header;
			}
		});

		// return the page of objects, along with the total result count
		PaginatedResults<EntityHeader> queryResults = new PaginatedResults<EntityHeader>();
		queryResults.setResults(favoritesHeaders);
		long totalCount = 0;
		try {
			totalCount = namedJdbcTemplate.queryForObject(COUNT_FAVORITES_SQL, params, Long.class);
		} catch (EmptyResultDataAccessException e) {
			// count = 0
		}
		queryResults.setTotalNumberOfResults(totalCount);
		return queryResults;
	}

}
