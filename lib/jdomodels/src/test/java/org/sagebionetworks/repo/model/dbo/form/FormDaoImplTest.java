package org.sagebionetworks.repo.model.dbo.form;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class FormDaoImplTest {

	@Autowired
	FormDao formDao;

	Long adminUserId;

	@BeforeEach
	public void before() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
	

	@Test
	public void testCreateGroupDto() {
		DBOFormGroup dbo = new DBOFormGroup();
		dbo.setCreatedBy(123L);
		dbo.setCreatedOn(new Timestamp(1));
		dbo.setName("someName");
		dbo.setGroupId(456L);
		
		// call under test
		FormGroup dto = FormDaoImpl.createGroupDto(dbo);
		assertNotNull(dto);
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getCreatedOn().getTime(), dto.getCreatedOn().getTime());
		assertEquals(dbo.getName(), dto.getName());
		assertEquals(dbo.getGroupId().toString(), dto.getGroupId());
	}

	@Test
	public void testCreate() {
		String name = UUID.randomUUID().toString();
		// call under test
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		assertNotNull(group);
		assertEquals(name, group.getName());
		assertEquals(adminUserId.toString(), group.getCreatedBy());
		assertNotNull(group.getCreatedOn());
	}

	@Test
	public void testCreateDuplicateName() {
		String name = UUID.randomUUID().toString();
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		assertNotNull(group);
		// try to create a group with the same name
		assertThrows(IllegalArgumentException.class, ()-> { 
			// call under test
			formDao.createFormGroup(adminUserId, name);
		});
	}
	
	@Test
	public void testLookupGroupByName() {
		String name = UUID.randomUUID().toString();
		// call under test
		Optional<FormGroup> optional = formDao.lookupGroupByName(name);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		// lookup again
		optional = formDao.lookupGroupByName(name);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(group, optional.get());
	}
}
