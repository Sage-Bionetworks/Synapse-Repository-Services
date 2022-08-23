package org.sagebionetworks.repo.manager.drs;

import com.google.common.collect.Lists;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;

import java.util.ArrayList;
import java.util.List;

import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

public class CreateProjectHierarchyTestDataUtil {
    private EntityManager entityManager;
    private AccessControlListObjectHelper aclDaoHelper;
    private FileHandleObjectHelper fileHandleObjectHelper;
    public Project project;
    public final List<Folder> folders = new ArrayList<>();
    public final List<FileEntity> fileEntities = new ArrayList<>();
    public final List<String> entitiesToDelete = Lists.newArrayList();

    public CreateProjectHierarchyTestDataUtil(EntityManager entityManager, AccessControlListObjectHelper aclDaoHelper,
                                              FileHandleObjectHelper fileHandleObjectHelper) {
        this.entityManager = entityManager;
        this.aclDaoHelper = aclDaoHelper;
        this.fileHandleObjectHelper = fileHandleObjectHelper;

    }

    public void createProjectHierachy(int numberOfFiles, UserInfo createdByUserInfo, UserInfo permissionGrantedTo) {
        project = entityManager.getEntity(createdByUserInfo, createProject(createdByUserInfo), Project.class);
        entitiesToDelete.add(project.getId());
        final Folder folderOne = entityManager.getEntity(createdByUserInfo, entityManager.createEntity(createdByUserInfo,
                new Folder().setName("folder one").setParentId(project.getId()), null), Folder.class);
        folders.add(folderOne);
        entitiesToDelete.add(folderOne.getId());
        // grant the user read on the project
        aclDaoHelper.update(project.getId(), ObjectType.ENTITY, a -> {
            a.getResourceAccess().add(createResourceAccess(permissionGrantedTo.getId(), ACCESS_TYPE.READ));
        });

        for (int i = 0; i < numberOfFiles; i++) {
            final int index = i;
            S3FileHandle fileHandle = fileHandleObjectHelper.createS3(f -> {
                f.setFileName("f" + index);
            });
            FileEntity file = entityManager
                    .getEntity(createdByUserInfo,
                            entityManager.createEntity(createdByUserInfo, new FileEntity().setName("file_" + index)
                                    .setParentId(folderOne.getId()).setDataFileHandleId(fileHandle.getId()), null),
                            FileEntity.class);
            fileEntities.add(file);
            entitiesToDelete.add(file.getId());
        }

    }

    private String createProject(final UserInfo createdByUser) {
        return entityManager.createEntity(createdByUser, new Project().setName(null), null);
    }
}
