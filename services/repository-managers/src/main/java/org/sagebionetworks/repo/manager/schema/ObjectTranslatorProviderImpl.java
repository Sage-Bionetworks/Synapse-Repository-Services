package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class ObjectTranslatorProviderImpl implements ObjectTranslatorProvider {	

	@Override
	public ObjectTranslator getTranslatorForConcreteType(String concreteType) {
		ValidateArgument.required(concreteType, "concreteType");
		return SchemaObjectType.valueOfConcreteType(concreteType).getObjectTranslator();
	}

}
