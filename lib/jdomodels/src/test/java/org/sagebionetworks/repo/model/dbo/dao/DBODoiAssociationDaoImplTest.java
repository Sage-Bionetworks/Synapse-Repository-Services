package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DBODoiAssociationDaoImplTest {

	/*
	 * This class exists to test the protected getDoiAssociation method.
	 * Public methods are tested in the autowired test
	 */
	private DBODoiAssociationDaoImpl doiAssociationDao;

	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;

	@Captor
	ArgumentCaptor<String> sqlQueryCaptor;

	@Captor
	ArgumentCaptor<MapSqlParameterSource> paramMapCaptor;

	@Captor
	ArgumentCaptor<RowMapper<DBODoi>> rowMapperArgumentCaptor;

	private DBODoi dbo;
	private String associatedById;
	private final String objectId = KeyFactory.keyToString(112233L);
	private final ObjectType objectType = ObjectType.ENTITY;
	private final Long versionNumber = 1L;


	@Before
	public void before() throws Exception {
		doiAssociationDao = new DBODoiAssociationDaoImpl();
		ReflectionTestUtils.setField(doiAssociationDao, "namedJdbcTemplate", mockNamedJdbcTemplate);
		associatedById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		// Create a DOI DTO with fields necessary to create a new one.

		dbo = new DBODoi();
		dbo.setCreatedBy(Long.valueOf(associatedById));
		dbo.setUpdatedBy(Long.valueOf(associatedById));
		dbo.setId(123L);
		dbo.setObjectType(objectType);
		dbo.setObjectVersion(versionNumber);
		dbo.setObjectId(KeyFactory.stringToKey(objectId));
	}

	@Test
	public void testGetDoiNotForUpdate() {
		boolean forUpdate = false;
		when(mockNamedJdbcTemplate.queryForObject(
				Mockito.any(String.class),
				Mockito.any(MapSqlParameterSource.class),
				Mockito.any(RowMapper.class))).thenReturn(dbo);

		// Call under test
		doiAssociationDao.getDoiAssociation(objectId,
					objectType, versionNumber, forUpdate);

		verify(mockNamedJdbcTemplate).queryForObject(sqlQueryCaptor.capture(),
				paramMapCaptor.capture(),
				rowMapperArgumentCaptor.capture());

		assertEquals(DBODoiAssociationDaoImpl.SELECT_DOI_BY_ASSOCIATED_OBJECT, sqlQueryCaptor.getValue());
	}

	@Test
	public void testGetDoiForUpdate() {
		boolean forUpdate = true;
		when(mockNamedJdbcTemplate.queryForObject(
				Mockito.any(String.class),
				Mockito.any(MapSqlParameterSource.class),
				Mockito.any(RowMapper.class))).thenReturn(dbo);

		// Call under test
		doiAssociationDao.getDoiAssociation(objectId,
				objectType, versionNumber, forUpdate);

		verify(mockNamedJdbcTemplate).queryForObject(sqlQueryCaptor.capture(),
				paramMapCaptor.capture(),
				rowMapperArgumentCaptor.capture());

		assertEquals(DBODoiAssociationDaoImpl.SELECT_DOI_BY_ASSOCIATED_OBJECT + " FOR UPDATE ", sqlQueryCaptor.getValue());
	}
}
