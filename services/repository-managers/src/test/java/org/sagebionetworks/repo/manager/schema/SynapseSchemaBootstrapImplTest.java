package org.sagebionetworks.repo.manager.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.springframework.beans.factory.annotation.Autowired;

@ExtendWith(MockitoExtension.class)
public class SynapseSchemaBootstrapImplTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Autowired
	private UserManager mockUserManager;

	@InjectMocks
	private SynapseSchemaBootstrapImpl bootstrap;
	
	ObjectSchemaImpl objectSchema;
	
	@BeforeEach
	public void before() {

	}


}
