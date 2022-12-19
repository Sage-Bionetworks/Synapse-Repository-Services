package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileSummary;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.migration.DatasetBackfillingResponse;
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
public class DatasetBackFilling {
    private static final long PAGE_SIZE_LIMIT = 1000;
    private static final String SELECT_ALL_DATASET = "SELECT N.ID, R.NUMBER, R.ENTITY_PROPERTY_ANNOTATIONS FROM " + TABLE_NODE + " N JOIN " + TABLE_REVISION + " R ON N." + COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE
            + " WHERE N." + COL_NODE_TYPE + " ='dataset'  LIMIT ? OFFSET ?";
    private static final RowMapper<DatasetBackFillDTO> ENTITY_SPECIFIC_INFO_ROW_MAPPER = (rs, rowNum) -> {
        long id = rs.getLong(COL_NODE_ID);
        long version = rs.getLong(COL_REVISION_NUMBER);
        IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(id).setVersion(version).build();
        org.sagebionetworks.repo.model.Annotations entityPropertyAnnotation = null;
        byte[] bytes = rs.getBytes(COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB);
        if (bytes != null) {
            try {
                entityPropertyAnnotation = AnnotationUtils.decompressedAnnotationsV1(bytes);
            } catch (IOException e) {
                throw new DatastoreException(e);
            }
        }

        return new DatasetBackFillDTO(idAndVersion, entityPropertyAnnotation);
    };

    private JdbcTemplate jdbcTemplate;

    private NodeDAO nodeDAO;
    private EntityManager entityManager;

    private TransactionTemplate readCommitedTransactionTemplate;

    @Autowired
    public DatasetBackFilling(JdbcTemplate jdbcTemplate, NodeDAO nodeDAO, EntityManager entityManager, TransactionTemplate readCommitedTransactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.nodeDAO = nodeDAO;
        this.entityManager = entityManager;
        this.readCommitedTransactionTemplate = readCommitedTransactionTemplate;
    }

    public List<DatasetBackFillDTO> getAllDatasetEntity(long limit, long offset) {
        return jdbcTemplate.query(SELECT_ALL_DATASET, ENTITY_SPECIFIC_INFO_ROW_MAPPER, limit, offset);
    }

    public DatasetBackfillingResponse datasetFileSummary(UserInfo userInfo) {
        AtomicInteger countRows = new AtomicInteger();
        try {
            PaginationIterator<DatasetBackFillDTO> iterator = new PaginationIterator<DatasetBackFillDTO>(
                    (long limit, long offset) -> getAllDatasetEntity(limit, offset),
                    PAGE_SIZE_LIMIT);
            iterator.forEachRemaining(entityInfo -> {
                Annotations annotation = entityInfo.getEntityPropertyAnnotations();
                if (annotation != null) {
                    Object checksum = annotation.getSingleValue("checksum");
                    Object size = annotation.getSingleValue("size");
                    Object count = annotation.getSingleValue("count");
                    if (checksum == null || size == null || count == null) {
                        updateDatasetAnnotationInTransaction(userInfo, entityInfo);
                        countRows.getAndIncrement();
                    }
                }
            });
        } finally {
            DatasetBackfillingResponse datasetBackfillingResponse = new DatasetBackfillingResponse().setCount(countRows.doubleValue());
            return datasetBackfillingResponse;
        }
    }

    private void updateDatasetAnnotationInTransaction(UserInfo userInfo, DatasetBackFillDTO datasetBackFillDTO) {
        this.readCommitedTransactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                Dataset dataset = entityManager.getEntityForVersion(userInfo, datasetBackFillDTO.getId().getId().toString(),
                        datasetBackFillDTO.getId().getVersion().get(), Dataset.class);
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
        private IdAndVersion id;
        private Annotations entityPropertyAnnotations;

        public DatasetBackFillDTO(IdAndVersion id, Annotations entityPropertyAnnotations) {
            this.id = id;
            this.entityPropertyAnnotations = entityPropertyAnnotations;
        }

        public IdAndVersion getId() {
            return id;
        }

        public void setId(IdAndVersion id) {
            this.id = id;
        }

        public Annotations getEntityPropertyAnnotations() {
            return entityPropertyAnnotations;
        }

        public void setEntityPropertyAnnotations(Annotations entityPropertyAnnotations) {
            this.entityPropertyAnnotations = entityPropertyAnnotations;
        }
    }
}
