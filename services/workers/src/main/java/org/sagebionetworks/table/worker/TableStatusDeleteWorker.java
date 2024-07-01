package org.sagebionetworks.table.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TableStatusDeleteWorker implements ChangeMessageDrivenRunner {
    private static final Log LOG = LogFactory.getLog(TableStatusDeleteWorker.class);

    @Autowired
    private TableManagerSupport tableManagerSupport;
    @Autowired
    private NodeDAO nodeDao;

    @Override
    public void run(ProgressCallback progressCallback, ChangeMessage message) throws RecoverableMessageException, Exception {
        if (ObjectType.ENTITY.equals((message.getObjectType())) && ChangeType.DELETE.equals(message.getChangeType())) {
            IdAndVersion idAndVersion = KeyFactory.idAndVersion(message.getObjectId(), message.getObjectVersion());
            EntityType entityType = nodeDao.getNodeTypeById(idAndVersion.getId().toString());
            Optional<TableType> tableType = TableType.lookupByEntityType(entityType);
            if (tableType.isEmpty() || TableType.virtualtable.equals(tableType.get())) {
                return;
            }
            try {
                tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback,
                        new LockContext(LockContext.ContextType.TableStatusDelete, idAndVersion), idAndVersion , (ProgressCallback innerCallback)->
                {
                    tableManagerSupport.deleteTableStatus(idAndVersion);
                    return null;
                });

            }catch (LockUnavilableException e) {
                // try again later.
                throw new RecoverableMessageException();
            } catch (Throwable e) {
                LOG.error("Could not delete table " + idAndVersion, e);
                throw e;
            }

        }
    }
}
