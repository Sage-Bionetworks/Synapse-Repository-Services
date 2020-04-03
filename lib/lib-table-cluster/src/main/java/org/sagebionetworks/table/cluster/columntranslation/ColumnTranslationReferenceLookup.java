package org.sagebionetworks.table.cluster.columntranslation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.sagebionetworks.repo.model.table.ColumnModel;

public class ColumnTranslationReferenceLookup {
	private static final Map<String, RowMetadataColumnTranslationReference> METADATA_COLUMN_MAP =
			Collections.unmodifiableMap(Arrays.stream(RowMetadataColumnTranslationReference.values())
			.collect(Collectors.toMap(
						RowMetadataColumnTranslationReference::getUserQueryColumnName,
						Function.identity(),
						(key, val) -> { throw new IllegalStateException("duplicate row metadata key: " + key);},
						CaseInsensitiveMap::new)));

	private Map<String, ColumnTranslationReference> userQueryColumnNameMap;
	private Map<String, ColumnTranslationReference> translatedColumnNameMap;


	public ColumnTranslationReferenceLookup(Collection<ColumnModel> columnModels){
		this.userQueryColumnNameMap = new HashMap<>();
		this.translatedColumnNameMap = new HashMap<>();

		//always add the row metadata columns
		for(RowMetadataColumnTranslationReference rowMetadata : RowMetadataColumnTranslationReference.values()){
			addToMaps(rowMetadata);
		}

		//add translation references for each column in the schema
		for(ColumnModel columnModel : columnModels){
			addToMaps(new SchemaColumnTranslationReference(columnModel));
		}
	}

	private void addToMaps(ColumnTranslationReference columnTranslationReference){
		userQueryColumnNameMap.put(columnTranslationReference.getUserQueryColumnName(), columnTranslationReference);
		translatedColumnNameMap.put(columnTranslationReference.getTranslatedColumnName(), columnTranslationReference);
	}

	public Optional<ColumnTranslationReference> forUserQueryColumnName(String userQueryColumnName){
		return checkForKey(userQueryColumnNameMap, userQueryColumnName);
	}

	public Optional<ColumnTranslationReference> forTranslatedColumnName(String translatedColumnName){
		return checkForKey(translatedColumnNameMap, translatedColumnName);
	}

	private static Optional<ColumnTranslationReference> checkForKey(Map<String, ColumnTranslationReference> map , String key){
		ColumnTranslationReference result = METADATA_COLUMN_MAP.get(key);
		if (result == null){
			result = map.get(key);
		}
		return Optional.ofNullable(result);
	}
}
