package org.sagebionetworks.repo.model.query.entity;

import java.util.Collection;

import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * Transforms 'syn123' to '123'
 *
 */
public class SynapseIdTransfromer implements ValueTransformer {

	@Override
	public Object transform(Object input) {
		if(input == null){
			return null;
		}
		if(input instanceof String){
			return KeyFactory.stringToKey((String)input);
		}else if(input instanceof Long){
			return input;
		}else if(input instanceof Integer){
			return input;
		}else if(input instanceof Collection){
			Collection collection = (Collection)input;
			if(collection.isEmpty()){
				return collection;
			}
			Object first = collection.iterator().next();
			if(first instanceof String){
				return KeyFactory.stringToKey(collection);
			}else if(first instanceof Long){
				return collection;
			}else if(first instanceof Integer){
				return collection;
			}else{
				throw new IllegalArgumentException("Unknown List type: "+first.getClass().getName());
			}
		}else{
			throw new IllegalArgumentException("Unknown input type: "+input.getClass().getName());
		}
	}

}
