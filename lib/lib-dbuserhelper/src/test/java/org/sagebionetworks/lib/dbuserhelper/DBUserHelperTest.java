package org.sagebionetworks.lib.dbuserhelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DBUserHelperTest {

	@Mock
	StackConfiguration mockConfig;
	@Mock
	JdbcTemplate mockTemplate;

	@Test
	void createReadOnlyUser() {
		when(mockConfig.getDbReadOnlyUserName()).thenReturn("userName");
		when(mockConfig.getDbReadOnlyPassword()).thenReturn("userPassword");
		when(mockConfig.getStack()).thenReturn("dev");
		when(mockConfig.getStackInstance()).thenReturn("101");
		when(mockTemplate.update(anyString())).thenReturn(1, 1);
		DBUserHelper dbUserHelper = new DBUserHelper(mockConfig);
		// call under test
		dbUserHelper.createDbReadOnlyUser(mockTemplate);

		verify(mockTemplate).update("CREATE USER IF NOT EXISTS 'userName'@'%' IDENTIFIED BY 'userPassword'");
		verify(mockTemplate).update("GRANT PROCESS, SELECT ON *.* TO 'userName'@'%'");
		verify(mockTemplate).update("GRANT EXECUTE ON dev101.* TO 'userName'@'%'");
	}
}