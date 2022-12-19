package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileSummary;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.migration.DatasetBackfillResponse;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

@TemporaryCode(author = "sandhra.sokhal@sagebase.org", comment = "This allows back filling of field checksum,size and count for dataset")
@Service
public class DatasetBackFillFileSummary {
    private static final long PAGE_SIZE_LIMIT = 1000;
    private static final String SELECT_ALL_DATASET = "SELECT N.ID, R.NUMBER, R.ENTITY_PROPERTY_ANNOTATIONS FROM " + TABLE_NODE + " N JOIN " + TABLE_REVISION + " R ON N."
            + COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE + " WHERE N." + COL_NODE_TYPE + " ='dataset' ORDER BY N.ID, R.NUMBER LIMIT ? OFFSET ?";
    private static final RowMapper<DatasetBackFillDTO> DATASET_BACK_FILL_ROW_MAPPER = (rs, rowNum) -> {
        String id = rs.getString(COL_NODE_ID);
        long version = rs.getLong(COL_REVISION_NUMBER);
        org.sagebionetworks.repo.model.Annotations entityPropertyAnnotation = null;
        byte[] bytes = rs.getBytes(COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);
        if (bytes != null) {
            try {
                entityPropertyAnnotation = AnnotationUtils.decompressedAnnotationsV1(bytes);
            } catch (IOException e) {
                throw new DatastoreException(e);
            }
        }

        return new DatasetBackFillDTO(id, version, entityPropertyAnnotation);
    };

    private JdbcTemplate jdbcTemplate;

    private NodeDAO nodeDAO;
    private EntityManager entityManager;

    private TransactionTemplate readCommitedTransactionTemplate;

    @Autowired
    public DatasetBackFillFileSummary(JdbcTemplate jdbcTemplate, NodeDAO nodeDAO, EntityManager entityManager, TransactionTemplate readCommitedTransactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.nodeDAO = nodeDAO;
        this.entityManager = entityManager;
        this.readCommitedTransactionTemplate = readCommitedTransactionTemplate;
    }

    public List<DatasetBackFillDTO> getAllDatasetEntity(long limit, long offset) {
        return jdbcTemplate.query(SELECT_ALL_DATASET, DATASET_BACK_FILL_ROW_MAPPER, limit, offset);
    }

    public DatasetBackfillResponse backFillDatasetFileSummary(UserInfo userInfo) {
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
                    checksum = annotation.getSingleValue("checksum");
                    size = annotation.getSingleValue("size");
                    count = annotation.getSingleValue("count");
                }

                if (checksum == null && size == null && count == null) {
                    updateDatasetAnnotationInTransaction(userInfo, datasetBackFillDTO);
                    countRows.getAndIncrement();
                }
            });
        } finally {
            DatasetBackfillResponse datasetBackFillResponse = new DatasetBackfillResponse().setCount(countRows.longValue());
            return datasetBackFillResponse;
        }
    }

    private void updateDatasetAnnotationInTransaction(UserInfo userInfo, DatasetBackFillDTO datasetBackFillDTO) {
        this.readCommitedTransactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                Dataset dataset = entityManager.getEntityForVersion(userInfo, datasetBackFillDTO.getId(),
                        datasetBackFillDTO.getVersion(), Dataset.class);
                FileSummary fileSummary = nodeDAO.getFileSummary(dataset.getItems());
                dataset.setChecksum(fileSummary.getChecksum());
                dataset.setSize(fileSummary.getSize());
                dataset.setCount(fileSummary.getCount());
                entityManager.updateEntity(userInfo, dataset, false, null);
                return null;
            }
        });
    }

    public static class DatasetBackFillDTO {
        private String id;
        private Long version;
        private Annotations entityPropertyAnnotations;

        public DatasetBackFillDTO(String id, Long version, Annotations entityPropertyAnnotations) {
            this.id = id;
            this.version = version;
            this.entityPropertyAnnotations = entityPropertyAnnotations;
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
    }
}
