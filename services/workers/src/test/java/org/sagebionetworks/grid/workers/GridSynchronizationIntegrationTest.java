package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridSynchronizationIntegrationTest {

	public static final long MAX_WAIT_MS = 120_000;

	@Autowired
	private GridService gridService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private EntityManager entityManager;

	private UserInfo admin;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		jsonSchemaManager.truncateAll();
		entityManager.truncateAll();

	}

	@Test
	public void testSynchronize() throws Exception {

		assertThrows(NotFoundException.class, ()->{
			asynchronousJobWorkerHelper.assertJobResponse(admin, new SynchronizeGridRequest().setGridSessionId("123"),
					(r) -> {
						System.out.println(r);
					}, MAX_WAIT_MS);
		});

	}

}
