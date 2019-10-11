package org.sagebionetworks.table.cluster.columntranslation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.apigateway.model.Op;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.sagebionetworks.repo.model.table.ColumnModel;

public class ColumnTranslationReferenceLookup {
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
		return Optional.ofNullable(userQueryColumnNameMap.get(userQueryColumnName));
	}

	public Optional<ColumnTranslationReference> forTranslatedColumnName(String translatedColumnName){
		return Optional.ofNullable(translatedColumnNameMap.get(translatedColumnName));
	}
}
