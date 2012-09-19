package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.sql.BatchUpdateException;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author deflaux
 *
 */
public class BaseControllerTest {


	@Test
	public void testDeadlockError(){
		EntityController controller = new EntityController();
		HttpServletRequest request = new MockHttpServletRequest();
		ErrorResponse response = controller.handleDeadlockExceptions(new DeadlockLoserDataAccessException("Message", new BatchUpdateException()), request);
		assertEquals(BaseController.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}

}
