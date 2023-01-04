package org.sagebionetworks.repo.manager.migration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileSummary;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.migration.DatasetBackfillResponse;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ITEMS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

@TemporaryCode(author = "sandhra.sokhal@sagebase.org", comment = "This allows back filling of field checksum,size and count for dataset")
@Service
public class DatasetBackFillFileSummary {
    private static final long PAGE_SIZE_LIMIT = 1000;
    private static final String CHECKSUM = "checksum";
    private static final String SIZE = "size";
    private static final String COUNT = "count";
    private static final String SQL_UPDATE_ETAG = "UPDATE " + TABLE_NODE + " SET " + COL_NODE_ETAG
            + " = ? WHERE " + COL_NODE_ID + " = ?";
    private static final String SQL_UPDATE_ANNOTATION = "UPDATE " + TABLE_REVISION + " SET " + COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB
            + " = ? WHERE " + COL_REVISION_OWNER_NODE + " = ? AND " + COL_REVISION_NUMBER + " =?";
    private static final String SELECT_ALL_DATASET = "SELECT N.ID, R.NUMBER, R.ENTITY_PROPERTY_ANNOTATIONS, R.ITEMS FROM " + TABLE_NODE + " N JOIN " + TABLE_REVISION + " R ON N."
            + COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE + " WHERE N." + COL_NODE_TYPE + " ='dataset' ORDER BY N.ID, R.NUMBER LIMIT ? OFFSET ?";
    private static final RowMapper<DatasetBackFillDTO> DATASET_BACK_FILL_ROW_MAPPER = (rs, rowNum) -> {
        String id = rs.getString(COL_NODE_ID);
        long version = rs.getLong(COL_REVISION_NUMBER);
        org.sagebionetworks.repo.model.Annotations entityPropertyAnnotation = null;
        List<EntityRef> items;
        byte[] bytes = rs.getBytes(COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);

        try {
            if (bytes != null) {
                entityPropertyAnnotation = AnnotationUtils.decompressedAnnotationsV1(bytes);
            }

            items = NodeUtils.readJsonToItems(rs.getString(COL_REVISION_ITEMS));
            if(items == null){
                items = Collections.emptyList();
            }
        } catch (IOException e) {
            throw new DatastoreException(e);
        }

        return new DatasetBackFillDTO(id, version, entityPropertyAnnotation, items);
    };
    static private Logger log = LogManager.getLogger(DatasetBackFillFileSummary.class);
    private JdbcTemplate jdbcTemplate;

    private NodeDAO nodeDAO;

    private TransactionTemplate readCommitedTransactionTemplate;

    private TransactionalMessenger transactionalMessenger;

    @Autowired
    public DatasetBackFillFileSummary(JdbcTemplate jdbcTemplate, NodeDAO nodeDAO,
                                      TransactionTemplate readCommitedTransactionTemplate, TransactionalMessenger transactionalMessenger) {
        this.jdbcTemplate = jdbcTemplate;
        this.nodeDAO = nodeDAO;
        this.readCommitedTransactionTemplate = readCommitedTransactionTemplate;
        this.transactionalMessenger = transactionalMessenger;
    }

    public List<DatasetBackFillDTO> getAllDatasetEntity(long limit, long offset) {
        return jdbcTemplate.query(SELECT_ALL_DATASET, DATASET_BACK_FILL_ROW_MAPPER, limit, offset);
    }

    public void updateNodeEtag(String etag, String nodId) {
        jdbcTemplate.update(SQL_UPDATE_ETAG, etag, nodId);
    }

    public void updateAnnotation(Annotations annotations, Long id, Long version) {
        try {
            // Compress the annotations.
            byte[] newAnnos = AnnotationUtils.compressAnnotationsV1(annotations);
            jdbcTemplate.update(SQL_UPDATE_ANNOTATION, newAnnos, id, version);
        } catch (IOException e) {
            throw new DatastoreException(e);
        }
    }

    public DatasetBackfillResponse backFillDatasetFileSummary(UserInfo user) {
        ValidateArgument.required(user, "User");
        validateUser(user);
        AtomicInteger countRows = new AtomicInteger();
        try {
            PaginationIterator<DatasetBackFillDTO> iterator = new PaginationIterator<DatasetBackFillDTO>(
                    (long limit, long offset) -> getAllDatasetEntity(limit, offset),
                    PAGE_SIZE_LIMIT);
            iterator.forEachRemaining(datasetBackFillDTO -> {
                Annotations annotation = datasetBackFillDTO.getEntityPropertyAnnotations();
                Object checksum = null;
                Object size = null;
                Object count = null;

                if (annotation != null) {
                    checksum = annotation.getSingleValue(CHECKSUM);
                    size = annotation.getSingleValue(SIZE);
                    count = annotation.getSingleValue(COUNT);
                }

                if (checksum == null && size == null && count == null) {
                    updateDatasetAnnotationInTransaction(user, datasetBackFillDTO);
                    countRows.getAndIncrement();
                }
            });

            DatasetBackfillResponse datasetBackFillResponse = new DatasetBackfillResponse()
                    .setCount(countRows.longValue());
            return datasetBackFillResponse;
        } catch (Exception exception) {
            log.error("Job failed:", exception);
            throw exception;
        }
    }

    /**
     * Validate that the user is an administrator.
     *
     * @param user
     */
    private void validateUser(UserInfo user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (!user.isAdmin()) throw new UnauthorizedException("Only an administrator may access this service.");
    }

    private void updateDatasetAnnotationInTransaction(UserInfo userInfo, DatasetBackFillDTO datasetBackFillDTO) {
        this.readCommitedTransactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                log.info(" updating dataset: " + datasetBackFillDTO.getId() + "." + datasetBackFillDTO.getVersion());
                FileSummary fileSummary = nodeDAO.getFileSummary(datasetBackFillDTO.getItems());
                String newEtag = UUID.randomUUID().toString();
                Annotations annotations = new Annotations();
                if (datasetBackFillDTO.getEntityPropertyAnnotations() != null) {
                    annotations = datasetBackFillDTO.getEntityPropertyAnnotations();
                }
                annotations.addAnnotation(CHECKSUM, fileSummary.getChecksum());
                annotations.addAnnotation(SIZE, fileSummary.getSize());
                annotations.addAnnotation(COUNT, fileSummary.getCount());
                annotations.setEtag(newEtag);

                updateNodeEtag(newEtag, datasetBackFillDTO.getId());
                updateAnnotation(annotations, Long.parseLong(datasetBackFillDTO.getId()), datasetBackFillDTO.getVersion());
                transactionalMessenger.sendMessageAfterCommit(new MessageToSend().withObjectId(datasetBackFillDTO.getId())
                        .withObjectType(ObjectType.ENTITY).withChangeType(ChangeType.UPDATE).withUserId(userInfo.getId()));
                return null;
            }
        });
    }

    public static class DatasetBackFillDTO {
        private String id;
        private Long version;
        private Annotations entityPropertyAnnotations;
        private List<EntityRef> items;

        public DatasetBackFillDTO(String id, Long version, Annotations entityPropertyAnnotations, List<EntityRef> items) {
            this.id = id;
            this.version = version;
            this.entityPropertyAnnotations = entityPropertyAnnotations;
            this.items = items;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public Annotations getEntityPropertyAnnotations() {
            return entityPropertyAnnotations;
        }

        public void setEntityPropertyAnnotations(Annotations entityPropertyAnnotations) {
            this.entityPropertyAnnotations = entityPropertyAnnotations;
        }

        public List<EntityRef> getItems() {
            return items;
        }

        public void setItems(List<EntityRef> items) {
            this.items = items;
        }
    }
}
