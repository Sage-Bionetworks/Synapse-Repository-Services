package org.sagebionetworks.javadoc.testclasses;

import java.util.List;

import org.sagebionetworks.schema.HasEffectiveSchema;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Generic class used to encapsulate a list of requests of objects of any type that can be accepted by a controller.
 * <p>
 * 
 * This class has been annotated to produce XML in addition to JSON.
 * <p>
 * 
 * @param <T> the type of result to paginate
 */
public class GenericList<T extends JSONEntity> implements JSONEntity, HasEffectiveSchema {

	private static final String CONCRETE_TYPE = "concreteType";
	
	public final static String EFFECTIVE_SCHEMA = "{\"id\":\"org.sagebionetworks.repo.model.GenericList\",\"title\":\"GenericList\",\"description\":\"A list of objects.\",\"name\":\"GenericList\",\"properties\":{\"list\":{\"items\":{\"type\":\"string\"},\"type\":\"array\",\"description\":\"The list\"}}}";


	private List<T> list;

	private Class<? extends T> clazz;

	public static <T extends JSONEntity> GenericList<T> wrap(List<T> list, Class<? extends T> clazz) {
		return new GenericList<T>(list, clazz);
	}

	public static <T extends JSONEntity> List<T> unwrap(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		GenericList<T> results = new GenericList<T>();
		results.initializeFromJSONObject(adapter);
		return results.getList();
	}

	public static <T extends JSONEntity> List<T> unwrap(JSONObjectAdapter adapter, Class<? extends T> clazz)
			throws JSONObjectAdapterException {
		GenericList<T> results = new GenericList<T>(clazz);
		results.initializeFromJSONObject(adapter);
		return results.getList();
	}

	public GenericList() {
	}

	/**
	 * Default constructor
	 */
	public GenericList(Class<? extends T> clazz) {
		this.clazz = clazz;
	}

	public GenericList(List<T> list, Class<? extends T> clazz) {
		this.list = list;
		this.clazz = clazz;
	}

	/**
	 * @return the list
	 */
	public List<T> getList() {
		return list;
	}

	/**
	 * @param list
	 */
	public void setList(List<T> list) {
		this.list = list;
	}

	@Override
	public String toString() {
		return "GenericList [list=" + list + "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if (adapter == null) {
			throw new IllegalArgumentException("Adapter cannot be null");
		}
		return adapter;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if (adapter == null)
			throw new IllegalArgumentException("Adapter cannot be null");
		adapter.put(CONCRETE_TYPE, clazz.getName());
		if (this.list != null) {
			JSONArrayAdapter array = adapter.createNewArray();
			adapter.put("list", array);
			int index = 0;
			for (JSONEntity entity : list) {
				JSONObjectAdapter entityAdapter = entity.writeToJSONObject(adapter.createNew());
				array.put(index, entityAdapter);
				index++;
			}
		}
		return adapter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericList<T> other = (GenericList<T>) obj;
		if (clazz != other.clazz)
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		return true;
	}

	@Override
	public String getEffectiveSchema() {
		return EFFECTIVE_SCHEMA;
	}
}