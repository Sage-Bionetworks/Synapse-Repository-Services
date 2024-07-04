package org.sagebionetworks.repo.model.dbo.migration;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.io.StreamException;

@Service
public class MigrationTypeProviderImpl implements MigrationTypeProvider {

	private static final String INPUT_CONTAINED_NO_DATA = "input contained no data";

	private final List<MigratableDatabaseObject> objects;
	private final Map<BackupAliasType, UnmodifiableXStream> xStreamMap;
	private final Map<MigrationType, MigratableDatabaseObject> objectMap;

	@Autowired
	public MigrationTypeProviderImpl(List<MigratableDatabaseObject> objects) {
		this.objects = objects;
		this.objectMap = new LinkedHashMap<>();
		objects.stream().forEach((o) -> objectMap.put(o.getMigratableTableType(), o));
		this.xStreamMap = MigrationXStreamBuilder.buildXStream(objects);
	}

	@Override
	public MigratableDatabaseObject getObjectForType(MigrationType type) {
		return objectMap.get(type);
	}

	@Override
	public List<MigratableDatabaseObject> getDatabaseObjectRegister() {
		return this.objects;
	}

	@Override
	public void writeObjects(BackupAliasType backupAliasType, MigrationType type, List<?> backupObjects, Writer writer) {
		String alias = BackupAliasType.MIGRATION_TYPE_NAME.equals(backupAliasType) ? type.name()
				: objectMap.get(type).getTableMapping().getTableName();
		JSONArray array = new JSONArray();
		for(Object object: backupObjects){
			array.put(writeToJson(alias, object));
		}
		try {
			writer.append(array.toString((5)));
		} catch (JSONException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static JSONObject writeToJson(String alias, Object object){
		JSONObject json = new JSONObject();
		json.put("_alias", alias);
		Class clazz = object.getClass();
		for(Field field: clazz.getDeclaredFields()){
			if(!Modifier.isStatic(field.getModifiers()) && Modifier.isPrivate(field.getModifiers())){
				try {
					field.setAccessible(true);
					Object value = field.get(object);
					if(value != null){
						json.put(field.getName(), value);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		};
		return json;
	}

	@Override
	public <B> Optional<List<B>> readObjects(Class<? extends B> clazz, BackupAliasType backupAliasType,
			InputStream input, MigrationFileType fileType) {
		switch (fileType) {
		case XML:
			return readXML(clazz, backupAliasType, input);
		case JSON:
			return readJSON(clazz, backupAliasType, input);
		default:
			throw new IllegalStateException("Unknown type: " + fileType);
		}
	}

	<B> Optional<List<B>> readJSON(Class<? extends B> clazz, BackupAliasType backupAliasType, InputStream input) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * This will be removed.
	 * 
	 */
	@Deprecated
	<B> Optional<List<B>> readXML(Class<? extends B> clazz, BackupAliasType backupAliasType, InputStream input) {
		try {
			List<B> backupObjects = (List<B>) xStreamMap.get(backupAliasType).fromXML(input);
			return Optional.of(backupObjects);
		} catch (StreamException e) {
			if (!(e.getCause() instanceof EOFException
					&& e.getCause().getMessage().contains(INPUT_CONTAINED_NO_DATA))) {
				throw new RuntimeException(e);
			}
			// This file is empty so move to the next file...
			return Optional.empty();
		}
	}

}
