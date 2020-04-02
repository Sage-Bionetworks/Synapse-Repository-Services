package org.sagebionetworks.repo.model.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class OrganizationDaoImplTest {

	@Autowired
	OrganizationDao organizationDao;

	Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	String name;

	@BeforeEach
	public void before() {
		name = "Foo.Bar";
	}

	@Test
	public void testCreateGetDelete() {
		// Call under test
		Organization created = organizationDao.createOrganization(name, adminUserId);
		assertNotNull(created);
		// name should be lower
		assertEquals(name.toLowerCase(), created.getName());
		assertNotNull(created.getId());
		assertNotNull(created.getCreatedOn());
		assertEquals(""+adminUserId, created.getCreatedBy());

		// call under test
		Organization fetched = organizationDao.getOrganizationByName(name);
		assertEquals(created, fetched);
		// call under test
		organizationDao.deleteOrganization(fetched.getId());
	}

	@Test
	public void testCreateOrganizationDuplicateName() {
		// Call under test
		Organization created = organizationDao.createOrganization(name, adminUserId);
		assertNotNull(created);
		// Attempt to create a duplicate name.
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			organizationDao.createOrganization(name, adminUserId);
		}).getMessage();
		assertEquals("An Organization with the name: 'foo.bar' already exists", message);
	}

	@Test
	public void testGetOrganizationByNameNotFound() {
		String name = "Foo.Bar";
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			organizationDao.getOrganizationByName(name);
		}).getMessage();
		assertEquals("Orgnaization with name: 'foo.bar' not found", message);
	}

	@Test
	public void testDeleteOrganizationNotFound() {
		String id = "-123";
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			organizationDao.deleteOrganization(id);
		}).getMessage();
		assertEquals("Orgnaization with id: '-123' not found", message);
	}

	@AfterEach
	public void afterEach() {
		organizationDao.truncateAll();
	}
}
