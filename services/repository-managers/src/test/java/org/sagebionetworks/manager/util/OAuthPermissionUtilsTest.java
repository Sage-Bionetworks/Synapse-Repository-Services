package org.sagebionetworks.manager.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.OAuthScope;

import com.google.common.collect.ImmutableList;

class OAuthPermissionUtilsTest {

	@Test
	void testScopeAllowsAccess() {
		assertTrue(OAuthPermissionUtils.scopeAllowsAccess(ImmutableList.of(OAuthScope.view), ACCESS_TYPE.READ));
		
		assertFalse(OAuthPermissionUtils.scopeAllowsAccess(ImmutableList.of(OAuthScope.openid), ACCESS_TYPE.READ));
		assertFalse(OAuthPermissionUtils.scopeAllowsAccess(Collections.EMPTY_LIST, ACCESS_TYPE.READ));
	}
	
	@Test
	void testCheckScopeAllowsAccess() {
		
		// no exception
		OAuthPermissionUtils.checkScopeAllowsAccess(ImmutableList.of(OAuthScope.view), ACCESS_TYPE.READ);
		
		assertThrows(UnauthorizedException.class, ()->{OAuthPermissionUtils.checkScopeAllowsAccess(ImmutableList.of(OAuthScope.openid), ACCESS_TYPE.READ);});
	}
	
	@Test
	void testAllAccessTypes() {
		for (ACCESS_TYPE accessType : ACCESS_TYPE.values()) {
			if (accessType==ACCESS_TYPE.PARTICIPATE) {
				continue;
			}
			// if there is no mapping, an exception will be thrown
			OAuthPermissionUtils.scopeAllowsAccess(Collections.EMPTY_LIST, accessType);
		}
	}
	

}
