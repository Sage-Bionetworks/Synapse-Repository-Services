package org.sagebionetworks.repo.model.registry;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class MigrationSpecDataLoaderImpl implements MigrationSpecDataLoader {

	public static final String JSON_FILE_NAME = "MigrationSpecData.json";
	
	@Override
	public MigrationSpecData loadMigrationSpecData() {
		try{
			ClassLoader classLoader = EntityType.class.getClassLoader();
			InputStream in = classLoader.getResourceAsStream(JSON_FILE_NAME);
			if(in == null)
				throw new IllegalStateException("Cannot find the "+JSON_FILE_NAME+" file on the classpath");
			String jsonString = readToString(in);
			MigrationSpec ms =  EntityFactory.createEntityFromJSONString(jsonString, MigrationSpec.class);
			MigrationSpecData msd = new MigrationSpecData();
			msd.setData(ms);
			return msd;
		}catch(IllegalArgumentException e){
			// Convert to a runtime
			throw new IllegalArgumentException("Failed to load: "+JSON_FILE_NAME+" from the classpath.  Error: "+e.getMessage());
		}catch(Exception e){
			// Convert to a runtime
			throw new RuntimeException(e);
		}
	}
	
	private static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	
}
