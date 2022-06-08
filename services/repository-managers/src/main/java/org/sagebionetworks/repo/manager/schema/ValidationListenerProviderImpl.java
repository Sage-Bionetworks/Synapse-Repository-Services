package org.sagebionetworks.repo.manager.schema;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidationListenerProviderImpl implements ValidationListenerProvider {

	private final AnnotationsTranslator annotationsTranslator;

	@Autowired
	public ValidationListenerProviderImpl(AnnotationsTranslator annotationsTranslator) {
		this.annotationsTranslator = annotationsTranslator;
	}

	@Override
	public DerivedAnnotationVisitor createNewVisitor(Schema schema, JSONObject subjectJson) {
		return new DerivedAnnotationVisitorImpl(annotationsTranslator, schema, subjectJson);
	}

}
