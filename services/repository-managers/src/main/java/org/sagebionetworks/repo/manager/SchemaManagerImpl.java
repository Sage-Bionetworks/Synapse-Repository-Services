package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Implementation of the schema manager.
 * 
 * @author jmhill
 * 
 */
public class SchemaManagerImpl implements SchemaManager {

	/**
	 * This auto-generated factory provides most of the entity mapping.
	 */
	AutoGenFactory autoGenFactory = new AutoGenFactory();

	/**
	 * 
	 */
	@Override
	public RestResourceList getRESTResources() {
		List<String> keys = new ArrayList<String>();
		Iterator<String> it = autoGenFactory.getKeySetIterator();
		while (it.hasNext()) {
			String id = it.next();
			keys.add(id);
		}
		RestResourceList rrList = new RestResourceList();
		rrList.setList(keys);
		return rrList;
	}

	@Override
	public ObjectSchema getEffectiveSchema(String resourceId)
			throws NotFoundException, DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("ResourceID cannot be null");
		// Look up this resource
		try {
			JSONEntity entity = autoGenFactory.newInstance(resourceId);
			return EntityFactory.createEntityFromJSONString(
					entity.getJSONSchema(), ObjectSchema.class);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("Could not find a schema for resourceId = "+ resourceId);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public ObjectSchema getFullSchema(String resourceId) throws NotFoundException, DatastoreException {
		if (resourceId == null)
			throw new IllegalArgumentException("ResourceID cannot be null");
		// Look up this resource
		resourceId = resourceId.replaceAll("\\.", "/");
		String fileName = "schema/" + resourceId + ".json";
		InputStream stream = AutoGenFactory.class.getClassLoader().getResourceAsStream(fileName);
		if (stream == null) throw new NotFoundException("JSON Schema cannot be found for: "+ fileName);
		String json;
		try {
			json = readStringFromStream(stream);
			return EntityFactory.createEntityFromJSONString(json, ObjectSchema.class);
		} catch (IOException e) {
			throw new DatastoreException(e);
		} catch (JSONObjectAdapterException e) {
			throw new DatastoreException(e);
		}

	}

	@Override
	public EntityRegistry getEntityRegistry() {
		return EntityType.getEntityRegistry();
	}

	/**
	 * Read a string from a stream.
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	private static String readStringFromStream(InputStream stream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Read the file
		try {
			byte[] buffer = new byte[1024];
			int length = -1;
			while ((length = stream.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			return new String(out.toByteArray(), "UTF-8");
		} finally {
			stream.close();
		}
	}
}
