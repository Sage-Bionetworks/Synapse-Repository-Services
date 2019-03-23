package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.loginlockout.DBOUnsuccessfulLoginLockout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@RunWith(MockitoJUnitRunner.class)
public class UnsuccessfulLoginLockoutDAOImplUnitTest {

	@Mock
	DBOBasicDao mockBasicDao;

	@Mock
	JdbcTemplate mockJdbcTemplate;

	@InjectMocks
	UnsuccessfulLoginLockoutDAOImpl dao;

	DBOUnsuccessfulLoginLockout dbo;

	UnsuccessfulLoginLockoutDTO dto;

	SqlParameterSource expectedSqlParameterSource;

	long userId = 1234L;
	long unsuccessfulLoginCount = 42;
	long lockoutExpirationMillis = 1231049984023L;


	@Before
	public void setUp(){
	 dbo = new DBOUnsuccessfulLoginLockout();
	 dbo.setUserId(userId);
	 dbo.setUnsuccessfulLoginCount(unsuccessfulLoginCount);
	 dbo.setLockoutExpiration(lockoutExpirationMillis);

	 dto = new UnsuccessfulLoginLockoutDTO(userId)
			 .withUnsuccessfulLoginCount(unsuccessfulLoginCount)
			 .withLockoutExpiration(lockoutExpirationMillis);

	 expectedSqlParameterSource = new SinglePrimaryKeySqlParameterSource(userId);
	}

	@Test
	public void testTranslateDTOToDBO(){
		DBOUnsuccessfulLoginLockout translatedDBO = UnsuccessfulLoginLockoutDAOImpl.translateDTOToDBO(dto);
		assertEquals(dbo, translatedDBO);
	}

	@Test
	public void testTranslateDBOToDTO(){
		UnsuccessfulLoginLockoutDTO translatedDTO = UnsuccessfulLoginLockoutDAOImpl.translateDBOToDTO(dbo);
		assertEquals(dto, translatedDTO);
	}

	@Test
	public void testTranslateDBOToDTO_nullDBO(){
		UnsuccessfulLoginLockoutDTO translatedDTO = UnsuccessfulLoginLockoutDAOImpl.translateDBOToDTO(null);
		assertNull(translatedDTO);
	}

	@Test
	public void testGetUnsuccessfulLoginLockoutInfoIfExist(){
		//method under test
		dao.getUnsuccessfulLoginLockoutInfoIfExist(userId);

		verify(mockJdbcTemplate).queryForObject("SELECT PRINCIPAL_ID FROM CREDENTIAL WHERE PRINCIPAL_ID=? FOR UPDATE", Long.class, userId);
		verify(mockBasicDao).getObjectByPrimaryKeyIfExists(DBOUnsuccessfulLoginLockout.class, expectedSqlParameterSource);
	}

	@Test
	public void getDatabaseTimestampMillis(){
		final long timestamp = 12345;
		when(mockBasicDao.getDatabaseTimestampMillis()).thenReturn(timestamp);

		//method under test
		assertEquals(timestamp, dao.getDatabaseTimestampMillis());

		verify(mockBasicDao).getDatabaseTimestampMillis();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateOrUpdateUnsuccessfulLoginLockoutInfo_nullDTO(){
		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(null);
	}

	@Test
	public void testCreateOrUpdateUnsuccessfulLoginLockoutInfo(){
		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(dto);

		verify(mockBasicDao).createOrUpdate(dbo);
	}
}

