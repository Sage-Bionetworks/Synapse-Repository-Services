package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.sagebionetworks.repo.manager.AuthenticationManagerImpl.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.password.InvalidPasswordException;
import org.sagebionetworks.repo.manager.password.PasswordValidatorImpl;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.auth.AuthenticationReceiptDAO;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationManagerImplUnitTest {

	@InjectMocks
	private AuthenticationManagerImpl authManager;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private AuthenticationReceiptDAO mockAuthReceiptDAO;
	@Mock
	private PasswordValidatorImpl mockPassswordValidator;
	@Mock
	private AuthenticationManagerUtil mockAuthenticationManagerUtil;


	final Long userId = 12345L;
	//	final String username = "AuthManager@test.org";
	final String password = "gro.tset@reganaMhtuA";
	final String synapseSessionToken = "synapsesessiontoken";

	@Before
	public void setUp() throws Exception {
		when(mockAuthDAO.changeSessionToken(eq(userId), eq((String) null))).thenReturn(synapseSessionToken);

		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		ug.setIsIndividual(true);
		when(mockUserGroupDAO.get(userId)).thenReturn(ug);
		when(mockAuthenticationManagerUtil.checkPasswordWithLock(userId, password)).thenReturn(true);
		when(mockAuthenticationManagerUtil.checkPassword(userId, password)).thenReturn(true);
	}

	@Test
	public void testGetSessionToken() throws Exception {
		Session session = authManager.getSessionToken(userId);
		assertEquals(synapseSessionToken, session.getSessionToken());

		verify(mockAuthDAO, times(1)).getSessionTokenIfValid(eq(userId));
		verify(mockAuthDAO, times(1)).changeSessionToken(eq(userId), eq((String) null));
	}

	@Test
	public void testCheckSessionToken() throws Exception {
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		Long principalId = authManager.checkSessionToken(synapseSessionToken, true);
		assertEquals(userId, principalId);

		// Token matches, but terms haven't been signed
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(false);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (TermsOfUseException e) {
		}

		// Nothing matches the token
		when(mockAuthDAO.getPrincipalIfValid(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(null);
		when(mockAuthDAO.hasUserAcceptedToU(eq(userId))).thenReturn(true);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("invalid"));
		}

		// Token matches, but has expired
		when(mockAuthDAO.getPrincipal(eq(synapseSessionToken))).thenReturn(userId);
		try {
			authManager.checkSessionToken(synapseSessionToken, true).toString();
			fail();
		} catch (UnauthenticatedException e) {
			assertTrue(e.getMessage().contains("expired"));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnseeTermsOfUse() throws Exception {
		authManager.setTermsOfUseAcceptance(userId, null);
	}

	@Test
	public void testSetPasswordWithInvalidPassword() {
		String bannedPassword = "password123";
		doThrow(InvalidPasswordException.class).when(mockPassswordValidator).validatePassword(bannedPassword);
		try {
			authManager.setPassword(userId, bannedPassword);
		} catch (InvalidPasswordException e) {
			verify(mockPassswordValidator).validatePassword(bannedPassword);
			verify(mockAuthDAO, never()).changePassword(anyLong(), anyString());
		}
	}

	@Test
	public void testSetPasswordWithValidPassword() {
		String validPassword = UUID.randomUUID().toString();
		authManager.setPassword(userId, validPassword);
		verify(mockPassswordValidator).validatePassword(validPassword);
		verify(mockAuthDAO).changePassword(anyLong(), anyString());
	}

	@Test
	public void testLoginWithoutReceipt() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);

		//method under test
		authManager.login(userId, password, null);

		verify(mockAuthenticationManagerUtil, never()).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO).createNewReceipt(userId);
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}

	@Test
	public void testLoginWithInvalidReceipt() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);

		//method under test
		authManager.login(userId, password, receipt);

		verify(mockAuthenticationManagerUtil, never()).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO).createNewReceipt(userId);
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}

	@Test
	public void testLoginWithInvalidReceiptAndWrongPassword() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);
		when(mockAuthenticationManagerUtil.checkPasswordWithLock(userId, password)).thenReturn(false);


		try {
			//method under test
			authManager.login(userId, password, receipt);
			fail("expected exception to be thrown");
		} catch (UnauthenticatedException e) {
			//expected the exception to be thrown
		}

		verify(mockAuthenticationManagerUtil, never()).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO, never()).createNewReceipt(anyLong());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}


	@Test
	public void testLoginWithInvalidReceiptAndOverReceiptLimit() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(AUTHENTICATION_RECEIPT_LIMIT);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(false);

		//method under test
		authManager.login(userId, password, receipt);

		verify(mockAuthenticationManagerUtil, never()).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockAuthReceiptDAO, never()).replaceReceipt(userId, receipt);
	}

	@Test
	public void testLoginWithValidReceipt() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);


		authManager.login(userId, password, receipt);

		verify(mockAuthenticationManagerUtil).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil, never()).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO, never()).createNewReceipt(userId);
		verify(mockAuthReceiptDAO).replaceReceipt(userId, receipt);
	}

	@Test
	public void testLoginWithValidReceiptAndWrongPassword() {
		when(mockAuthReceiptDAO.countReceipts(userId)).thenReturn(0L);
		String receipt = "receipt";
		when(mockAuthReceiptDAO.isValidReceipt(userId, receipt)).thenReturn(true);
		when(mockAuthenticationManagerUtil.checkPassword(userId, password)).thenReturn(false);


		try {
			//method under test
			authManager.login(userId, password, receipt);
			fail("expected exception to be thrown");
		} catch (UnauthenticatedException e) {
			//expected the exception to be thrown
		}

		verify(mockAuthenticationManagerUtil).checkPassword(userId, password);
		verify(mockAuthenticationManagerUtil, never()).checkPasswordWithLock(userId, password);
		verify(mockAuthReceiptDAO).deleteExpiredReceipts(eq(userId), anyLong());
		verify(mockAuthReceiptDAO, never()).createNewReceipt(anyLong());
		verify(mockAuthReceiptDAO, never()).replaceReceipt(anyLong(), anyString());
	}



}
