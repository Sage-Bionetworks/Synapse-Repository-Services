package org.sagebionetworks.repo.model.dbo.migration;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.json.JavaJSONUtil;

import com.thoughtworks.xstream.io.StreamException;

public class MigrationTypeProviderImpl implements MigrationTypeProvider {

	private static final String INPUT_CONTAINED_NO_DATA = "input contained no data";

	private final List<MigratableDatabaseObject> objects;
	private final Map<BackupAliasType, UnmodifiableXStream> xStreamMap;
	private final Map<MigrationType, MigratableDatabaseObject> objectMap;

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
	public void writeObjects(List<?> backupObjects, Writer writer) {
		/*
		 * Note we write each object to JSON separately to reduce memory spikes (see:
		 * https://sagebionetworks.jira.com/browse/PLFM-8540 ). Also, we do not write
		 * anything if the provided list is empty or contains no actual data.
		 */
		try {
			if (!backupObjects.isEmpty()) {
				boolean hasData = false;
				for (Object o : backupObjects) {
					Optional<JSONObject> option = JavaJSONUtil.writeToJSON(o);
					if (option.isPresent()) {
						if (!hasData) {
							writer.append("[\n");
						} else {
							writer.append(",\n");
						}
						writer.append(option.get().toString(2));
						hasData = true;
					}
				}
				if (hasData) {
					writer.append("]\n");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		try {
			return Optional.of(
					JavaJSONUtil.readFromJSON(clazz, new JSONArray(IOUtils.toString(new InputStreamReader(input)))));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
