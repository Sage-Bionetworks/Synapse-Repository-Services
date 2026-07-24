package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.NodeDAO;
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
	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;
	private final CsvFileHandleProvider csvFileHandleProvider;
	private final RecordSetSchemaResolver recordSetSchemaResolver;
	private final GridRecordSetExporter recordSetExporter;
	private final GridRowValidator gridRowValidator;
	private final NodeDAO nodeDao;

	public SourceHandlerProviderImpl(TableQueryManager tableQueryManager,
			GridAuthorizationManager gridAuthorizationManager, FileProvider fileProvider,
			AnnotationWriter annotationWriter, JsonSchemaManager jsonSchemaManager,
			AnnotationsTranslator annotationsTranslator, EntityManager entityManager,
			FileHandleManager fileHandleManager, CsvFileHandleProvider csvFileHandleProvider,
			RecordSetSchemaResolver recordSetSchemaResolver, GridRecordSetExporter recordSetExporter,
			GridRowValidator gridRowValidator, NodeDAO nodeDao) {
		super();
		this.tableQueryManager = tableQueryManager;
		this.gridAuthorizationManager = gridAuthorizationManager;
		this.fileProvider = fileProvider;
		this.annotationWriter = annotationWriter;
		this.jsonSchemaManager = jsonSchemaManager;
		this.annotationsTranslator = annotationsTranslator;
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
		this.csvFileHandleProvider = csvFileHandleProvider;
		this.recordSetSchemaResolver = recordSetSchemaResolver;
		this.recordSetExporter = recordSetExporter;
		this.gridRowValidator = gridRowValidator;
		this.nodeDao = nodeDao;
	}

	@Override
	public SourceHandler createNewHandler(AsyncJobProgressCallback callback, UserInfo user, GridSession session,
	                                      GridSource gridSource) throws Exception {
        return switch (gridSource.getType()) {
            case entityview ->
                    new EntityViewSourceHandler(callback, user, session, tableQueryManager, gridAuthorizationManager,
                            fileProvider, annotationWriter, jsonSchemaManager, annotationsTranslator);
            case recordset ->
                    new RecordSetSourceHandler(user, session, entityManager, fileHandleManager, csvFileHandleProvider,
                            recordSetSchemaResolver, fileProvider, recordSetExporter, gridRowValidator, nodeDao);
            default -> throw new IllegalArgumentException("Unsupported type: " + gridSource.getType());
        };
	}

}
