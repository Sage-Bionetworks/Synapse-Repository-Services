package org.sagebionetworks.repo.model.query.entity;

/**
 * This transformer will not change the value of the given input.
 *
 */
public class DefaultValueTransformer implements ValueTransformer {

	@Override
	public Object transform(Object input) {
		return input;
	}

}
