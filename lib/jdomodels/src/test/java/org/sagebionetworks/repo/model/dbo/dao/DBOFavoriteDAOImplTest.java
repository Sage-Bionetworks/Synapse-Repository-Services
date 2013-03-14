package org.sagebionetworks.repo.model.dbo.dao;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Tests non-database related business logic in the DAO
 * This is more efficient than the Autowired test
 * @author dburdick
 *
 */
public class DBOFavoriteDAOImplTest {
	
	FavoriteDAO favoriteDAO;
	TagMessenger mockTagMessenger;
	DBOBasicDao mockBasicDao;	
	SimpleJdbcTemplate mockSimpleJdbcTemplate;
	IdGenerator mockIdGenerator;
	
	@Before
	public void before() {
		mockTagMessenger = mock(TagMessenger.class);
		mockBasicDao = mock(DBOBasicDao.class);
		mockSimpleJdbcTemplate = mock(SimpleJdbcTemplate.class);
		mockIdGenerator = mock(IdGenerator.class);
				 	
		favoriteDAO = new DBOFavoriteDAOImpl(mockTagMessenger, mockBasicDao, mockSimpleJdbcTemplate);		
	}
	

}
