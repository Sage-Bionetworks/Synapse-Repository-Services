package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.util.FileProvider;
import org.springframework.stereotype.Component;

@Component
public class SourceHandlerProviderImpl implements SourceHandlerProvider {

	private final TableQueryManager tableQueryManager;
	private final GridAuthorizationManager gridAuthorizationManager;
	private final FileProvider fileProvider;
	private final AnnotationWriter annotationWriter;
	private final JsonSchemaManager jsonSchemaManager;
	private final AnnotationsTranslator annotationsTranslator;

	public SourceHandlerProviderImpl(TableQueryManager tableQueryManager,
			GridAuthorizationManager gridAuthorizationManager, FileProvider fileProvider,
			AnnotationWriter annotationWriter, JsonSchemaManager jsonSchemaManager,
			AnnotationsTranslator annotationsTranslator) {
		super();
		this.tableQueryManager = tableQueryManager;
		this.gridAuthorizationManager = gridAuthorizationManager;
		this.fileProvider = fileProvider;
		this.annotationWriter = annotationWriter;
		this.jsonSchemaManager = jsonSchemaManager;
		this.annotationsTranslator = annotationsTranslator;
	}

	@Override
	public SourceHandler createNewProvider(AsyncJobProgressCallback callback, UserInfo user, GridSession session,
			GridSource gridSource) throws Exception {
		switch (gridSource.getType()) {
		case entityview:
			return new EntityViewSourceHandler(callback, user, session, tableQueryManager, gridAuthorizationManager,
					fileProvider, annotationWriter, jsonSchemaManager, annotationsTranslator);
		default:
			throw new IllegalArgumentException("Unsupported type: " + gridSource.getType());
		}
	}

}
