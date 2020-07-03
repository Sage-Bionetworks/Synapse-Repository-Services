package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.FileEntity;

public class ObjectTranslatorProviderImplTest {

	ObjectTranslatorProviderImpl provider;

	@BeforeEach
	public void before() {
		provider = new ObjectTranslatorProviderImpl();
	}

	@Test
	public void test() {
		// call under test
		ObjectTranslator translator = provider.getTranslatorForConcreteType(FileEntity.class.getName());
		assertNotNull(translator);
		assertTrue(translator instanceof EntityObjectTranslator);
	}

}
