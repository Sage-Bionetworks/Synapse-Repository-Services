package org.sagebionetworks.repo.model.query.entity;

import java.util.List;

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
		}else if(input instanceof List){
			List list = (List)input;
			if(list.isEmpty()){
				return list;
			}
			Object first = list.get(0);
			if(first instanceof String){
				return KeyFactory.stringToKey(list);
			}else if(first instanceof Long){
				return list;
			}else if(first instanceof Integer){
				return list;
			}else{
				throw new IllegalArgumentException("Unknown List type: "+first.getClass().getName());
			}
		}else{
			throw new IllegalArgumentException("Unknown input type: "+input.getClass().getName());
		}
	}

}
