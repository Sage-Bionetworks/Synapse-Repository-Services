package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.repo.manager.grid.synch.io.DiskPointer;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemWriter;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Read/rules side of a grid session sourced from an entity view. The rows are the
 * result of {@code select * from <view>}, streamed to a disk index and keyed by
 * row id. Its paired {@link SourceWriter} ({@link InPlaceAnnotationSourceWriter})
 * writes cell changes back as annotations; row/column membership is query-driven
 * and cannot be modified from the grid.
 */
@Component
@Scope("prototype")
public class EntityViewSourceHandler implements SourceHandler {

	private final AsyncJobProgressCallback callback;
	private final UserInfo user;
	private final GridSession session;
	private final TableQueryManager tableQueryManager;
	private final GridAuthorizationManager gridAuthorizationManager;
	private final FileProvider fileProvider;
	private final AnnotationWriter annotationWriter;
	private final JsonSchemaManager jsonSchemaManager;
	private final AnnotationsTranslator annotationsTranslator;
	private final Set<Long> collectedBenefactorIds;
	private List<ColumnModel> schema;
	private List<DiskPointer> diskPointers;
	private File tempFile;
	private Map<String, Translator> translators;
	private Set<String> requiredColumnNames;

	public EntityViewSourceHandler(AsyncJobProgressCallback callback, UserInfo user, GridSession session,
			TableQueryManager tableQueryManager, GridAuthorizationManager gridAuthorizationManager,
			FileProvider fileProvider, AnnotationWriter annotationWriter, JsonSchemaManager jsonSchemaManager,
			AnnotationsTranslator annotationsTranslator) throws NotFoundException, LockUnavilableException, TableUnavailableException, TableFailedException, IOException {
		this.callback = callback;
		this.user = user;
		this.session = session;
		this.tableQueryManager = tableQueryManager;
		this.gridAuthorizationManager = gridAuthorizationManager;
		this.fileProvider = fileProvider;
		this.annotationWriter = annotationWriter;
		this.jsonSchemaManager = jsonSchemaManager;
		this.annotationsTranslator = annotationsTranslator;
		this.collectedBenefactorIds = new HashSet<>();
		initialize();
	}

	void initialize() throws NotFoundException, LockUnavilableException, TableUnavailableException,
			TableFailedException, IOException {
		JsonSchema jsonSchema = session.getGridJsonSchema$Id() != null ? jsonSchemaManager.getValidationSchema(session.getGridJsonSchema$Id())
				: null;

		requiredColumnNames = jsonSchema != null && jsonSchema.getRequired() != null
				? new HashSet<>(jsonSchema.getRequired())
				: Collections.emptySet();

		tempFile = fileProvider.createTempFile("Source-" + session.getSourceEntityId(), ".bin");
		diskPointers = new ArrayList<>();
		try (RowSourceItemWriter writer = createRowWriter(tempFile)) {
			UserInfo sessionOwner = gridAuthorizationManager.getRowLevelFilterUserInfo(user, session.getSessionId());
			Query query = new Query().setSql("select * from " + session.getSourceEntityId());

			schema = new ArrayList<>();
			tableQueryManager.runQueryAsStream(callback, sessionOwner, query, t -> {
				schema.addAll(t.getMainQuery().getTranslator().getSchemaOfSelect());
				translators = schema.stream().collect(Collectors.toMap(ColumnModel::getName,
						cm -> ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator()));
				return row -> diskPointers.add(writer.nextRow(createSynchRow(row)));
			}, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE);
		}
	}

	RowSourceItemWriter createRowWriter(File temp) throws FileNotFoundException {
		return new RowSourceItemWriter(fileProvider.createFileOutputStream(tempFile));
	}

	RowSourceItem createSynchRow(Row row) {
		if (row.getBenefactorId() != null) {
			collectedBenefactorIds.add(row.getBenefactorId());
		}
		String key = IdAndVersion.newBuilder().setId(row.getRowId()).build().toString();
		TreeMap<String, ConValue> data = new TreeMap<>();
		for (int i = 0; i < schema.size(); i++) {
			String columnName = schema.get(i).getName();
			ConValue conValue = translators.get(columnName).translateNullable(row.getValues().get(i),
					requiredColumnNames.contains(columnName));
			data.put(columnName, conValue);
		}
		return new RowSourceItem(data, key, new SynapseRow().setRowId(row.getRowId())
				.setVersionNumber(row.getVersionNumber()).setEtag(row.getEtag()));
	}

	@Override
	public Set<Long> getBenefactorIds() {
		return collectedBenefactorIds;
	}

	@Override
	public SourceWriter createSourceWriter(SyncType syncType) {
		return new InPlaceAnnotationSourceWriter(user, annotationWriter, annotationsTranslator);
	}

	@Override
	public RowSourceItemReader getSourceRowReader() throws IOException {
		return new RowSourceItemReader(diskPointers, fileProvider.createRandomAccessFile(tempFile, "r"));
	}

	@Override
	public String getRowKey(RowCopyItem rowView) {
		SynapseRow synRow = rowView.getSynapseRow()
				.orElseThrow(() -> new IllegalArgumentException("Expected Synapse rows"));
		return IdAndVersion.newBuilder().setId(synRow.getRowId()).build().toString();
	}

	@Override
	public List<String> getCurrentSourceSchema() {
		return schema.stream().map(ColumnModel::getName).collect(Collectors.toList());
	}

	/**
	 * EntityView sources support only {@link SyncType#PULL_PUSH}. PULL is rejected
	 * because entity-view row membership is query-driven and cannot be written back
	 * to the source independently of a push.
	 */
	@Override
	public void validateSyncType(SyncType syncType) {
		ValidateArgument.required(syncType, "syncType");
		switch (syncType) {
            case PULL -> throw new IllegalArgumentException("PULL synchronization is not supported for EntityView-based grid sessions.");
            case PULL_PUSH -> {
				// allowed
            }
        }
	}

	@Override
	public void close() {
		if (tempFile != null) {
			tempFile.delete();
		}
	}

}
