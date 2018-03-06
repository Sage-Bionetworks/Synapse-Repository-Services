package org.sagebionetworks.repo.model;

import java.util.ArrayList;
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
public class ListWrapper<T extends JSONEntity> implements JSONEntity, HasEffectiveSchema {

	private static final String CONCRETE_TYPE = "concreteType";
	
	public final static String EFFECTIVE_SCHEMA = "{\"id\":\"org.sagebionetworks.repo.model.ListWrapper\",\"description\":\"A list of objects\",\"name\":\"ListWrapper\",\"properties\":{\"list\":{\"items\":{\"id\":\"org.sagebionetworks.repo.model.Entity\",\"description\":\"This is the base interface that all Entities should implement\",\"name\":\"Entity\",\"properties\":{\"id\":{\"description\":\"The unique immutable ID for this entity.  A new ID will be generated for new Entities.  Once issued, this ID is guaranteed to never change or be re-issued\",\"type\":\"string\"},\"createdOn\":{\"description\":\"The date this entity was created.\",\"format\":\"date-time\",\"type\":\"string\"},\"modifiedOn\":{\"description\":\"The date this entity was last modified.\",\"format\":\"date-time\",\"type\":\"string\"},\"parentId\":{\"description\":\"The ID of the parent of this entity\",\"type\":\"string\"},\"etag\":{\"description\":\"Synapse employs an Optimistic Concurrency Control (OCC) scheme to handle concurrent updates. Since the E-Tag changes every time an entity is updated it is used to detect when a client's current representation of an entity is out-of-date.\",\"type\":\"string\"},\"createdBy\":{\"description\":\"The user that created this entity.\",\"type\":\"string\"},\"accessControlList\":{\"description\":\"The URI to get to this entity's access control list\",\"transient\":true,\"type\":\"string\"},\"description\":{\"description\":\"The description of this entity.\",\"type\":\"string\"},\"modifiedBy\":{\"description\":\"The user that last modified this entity.\",\"type\":\"string\"},\"name\":{\"description\":\"The name of this entity\",\"type\":\"string\"},\"annotations\":{\"description\":\"The URI to get to this entity's annotations\",\"transient\":true,\"type\":\"string\"},\"uri\":{\"description\":\"The Uniform Resource Identifier (URI) for this entity.\",\"transient\":true,\"type\":\"string\"}},\"type\":\"interface\"},\"description\":\"The list of objects\",\"type\":\"array\"}},\"type\":\"object\"}";

	private List<T> list;

	private Class<? extends T> clazz;

	public static <T extends JSONEntity> ListWrapper<T> wrap(List<T> list, Class<? extends T> clazz) {
		return new ListWrapper<T>(list, clazz);
	}

	public static <T extends JSONEntity> List<T> unwrap(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		ListWrapper<T> results = new ListWrapper<T>();
		results.initializeFromJSONObject(adapter);
		return results.getList();
	}

	public static <T extends JSONEntity> List<T> unwrap(JSONObjectAdapter adapter, Class<? extends T> clazz)
			throws JSONObjectAdapterException {
		ListWrapper<T> results = new ListWrapper<T>(clazz);
		results.initializeFromJSONObject(adapter);
		return results.getList();
	}

	public ListWrapper() {
	}

	/**
	 * Default constructor
	 */
	public ListWrapper(Class<? extends T> clazz) {
		this.clazz = clazz;
	}

	public ListWrapper(List<T> list, Class<? extends T> clazz) {
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
		return "ListWrapper [list=" + list + "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
		if (adapter == null) {
			throw new IllegalArgumentException("Adapter cannot be null");
		}
		String defaultClass = adapter.getString(CONCRETE_TYPE);
		if (defaultClass != null) {
			try {
				clazz = (Class<? extends T>) Class.forName(defaultClass);
			} catch (ClassNotFoundException e) {
				throw new JSONObjectAdapterException("Class not found: " + e.getMessage(), e);
			}
		}

		if (!adapter.isNull("list")) {
			this.list = new ArrayList<T>();
			JSONArrayAdapter array = adapter.getJSONArray("list");
			for (int i = 0; i < array.length(); i++) {
				JSONObjectAdapter childAdapter = array.getJSONObject(i);
				try {
					T newInstance;
					if (!childAdapter.isNull(CONCRETE_TYPE)) {
						// child has an even more concrete type
						String childClass = childAdapter.getString(CONCRETE_TYPE);
						newInstance = (T) Class.forName(childClass).newInstance();
					} else {
						newInstance = (T) Class.forName(clazz.getName()).newInstance();
					}
					newInstance.initializeFromJSONObject(childAdapter);
					this.list.add(newInstance);
				} catch (Exception e) {
					throw new JSONObjectAdapterException(e);
				}
			}
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
		ListWrapper<T> other = (ListWrapper<T>) obj;
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