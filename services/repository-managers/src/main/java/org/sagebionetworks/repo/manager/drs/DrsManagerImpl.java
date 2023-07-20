package org.sagebionetworks.repo.manager.drs;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.drs.AccessId;
import org.sagebionetworks.repo.model.drs.AccessMethod;
import org.sagebionetworks.repo.model.drs.AccessMethodType;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.Checksum;
import org.sagebionetworks.repo.model.drs.ChecksumType;
import org.sagebionetworks.repo.model.drs.Content;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.DrsObjectId;
import org.sagebionetworks.repo.model.drs.OrganizationInformation;
import org.sagebionetworks.repo.model.drs.PackageInformation;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class DrsManagerImpl implements DrsManager {
    public static final String REVERSE_DOMAIN_NOTATION = "org.sagebase.prod.repo-prod";
    public static final String SERVICE_NAME = "Sage Bionetworks Synapse DRS API";
    public static final String CONTACT_URL = "https://sagebionetworks.jira.com/servicedesk/customer/portal/9";
    public static final String DOCUMENTATION_URL = "https://help.synapse.org/docs/";
    public static final String REGISTERED_HOSTNAME = "repo-prod.prod.sagebase.org";
    public static final String HTTPS = "https";
    public static final String DRS = "drs";
    public static final String DRS_GROUP = "org.ga4gh";
    public static final String DRS_ARTIFACT = "drs";
    public static final String DRS_VERSION = "1.2.0";
    public static final String ORGANIZATION_NAME = "Sage Bionetworks";
    public static final String DRS_URI = DRS + "://" + REGISTERED_HOSTNAME + "/";
    public static final String ORGANIZATION_URL = "https://www.sagebionetworks.org";
    public static final String DESCRIPTION = "This service provides implementation of DRS specification for " +
            "accessing FileEntities and Datasets within Synapse.";
    public static final String ILLEGAL_ARGUMENT_ERROR_MESSAGE = "Only FileEntity, Datasets, and File Handle IDs are supported.";
    private static final LocalDate localDate = LocalDate.of(2022, 8, 1);
    public static final Date CREATED_AT = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    public static final Date UPDATED_AT = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
    private final StackConfiguration stackConfiguration;
    private final FileHandleManager fileHandleManager;
    private final EntityManager entityManager;
    private final UserManager userManager;

    @Autowired
    public DrsManagerImpl(final StackConfiguration stackConfiguration, final FileHandleManager fileHandleManager,
                          final EntityManager entityManager, final UserManager userManager) {
        super();
        this.stackConfiguration = stackConfiguration;
        this.fileHandleManager = fileHandleManager;
        this.entityManager = entityManager;
        this.userManager = userManager;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        final ServiceInformation result = new ServiceInformation();
        result.setId(REVERSE_DOMAIN_NOTATION);
        result.setName(SERVICE_NAME);
        final PackageInformation drsPackageInformation = new PackageInformation();
        drsPackageInformation.setGroup(DRS_GROUP);
        drsPackageInformation.setArtifact(DRS_ARTIFACT);
        drsPackageInformation.setVersion(DRS_VERSION);
        result.setType(drsPackageInformation);
        result.setDescription(DESCRIPTION);
        final OrganizationInformation organization = new OrganizationInformation();
        organization.setName(ORGANIZATION_NAME);
        organization.setUrl(ORGANIZATION_URL);
        result.setOrganization(organization);
        result.setContactUrl(CONTACT_URL);
        result.setDocumentationUrl(DOCUMENTATION_URL);
        result.setCreatedAt(CREATED_AT);
        result.setUpdatedAt(UPDATED_AT);
        result.setEnvironment(stackConfiguration.getStack());
        result.setVersion(stackConfiguration.getStackInstance());
        return result;
    }

    @Override
    public DrsObject getDrsObject(final Long userId, final String objectId, final boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException, UnsupportedOperationException {
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(objectId, "objectId");
        final FileHandle fileHandle;
        final UserInfo userInfo = userManager.getUserInfo(userId);
        final DrsObjectId drsObjectId = DrsObjectId.parse(objectId);

        switch (drsObjectId.getType()) {
            case FILE_HANDLE:
                Long fileHandleId = drsObjectId.getFileHandleId();
                fileHandle = fileHandleManager.getRawFileHandle(userInfo, fileHandleId.toString());
                return createDrsObject(fileHandle);
            case ENTITY:
                final IdAndVersion idAndVersion = drsObjectId.getEntityId();
                final Entity entity = entityManager.getEntityForVersion(userInfo, idAndVersion.getId().toString(),
                        idAndVersion.getVersion().get(), null);

                if (entity instanceof FileEntity) {
                    final FileEntity file = (FileEntity) entity;
                    // Drs user are allowed to see metadata so access check is not required and
                    // moreover the metadata provided is not sensitive like s3 url etc.
                    fileHandle = fileHandleManager.getRawFileHandleUnchecked(file.getDataFileHandleId());
                    return createDrsObject(file, fileHandle, idAndVersion);
                } else if (entity instanceof Dataset) {
                    if (expand) {
                        throw new IllegalArgumentException("Nesting of bundle is not supported.");
                    }

                    final Dataset dataset = (Dataset) entity;
                    return createDrsObject(dataset, idAndVersion);
                } else {
                    throw new IllegalArgumentException(ILLEGAL_ARGUMENT_ERROR_MESSAGE);
                }
            default:
                throw new UnsupportedOperationException("Unexpected DRS Object type.");
        }
    }

    @Override
    public AccessUrl getAccessUrl(final Long userId, final String objectId, final String accessId)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException {
        ValidateArgument.required(userId, "userId");
        ValidateArgument.required(objectId, "objectId");
        ValidateArgument.required(accessId, "accessId");
        final FileHandleUrlRequest urlRequest;
        final AccessId accessIdObject;
        final UserInfo userInfo = userManager.getUserInfo(userId);

        final DrsObjectId drsObjectIdAndType = DrsObjectId.parse(objectId);

        switch (drsObjectIdAndType.getType()) {
            case FILE_HANDLE:
                final Long fileHandleIdFromObjectId = drsObjectIdAndType.getFileHandleId();
                accessIdObject = AccessId.decode(accessId);
                if (!fileHandleIdFromObjectId.toString().equals(accessIdObject.getFileHandleId())) {
                    throw new IllegalArgumentException("AccessId and ObjectId contain different file handle IDs.");
                }
                urlRequest = new FileHandleUrlRequest(userInfo, accessIdObject.getFileHandleId());
                break;
            case ENTITY:
                accessIdObject = AccessId.decode(accessId);
                final IdAndVersion drsObjectId = IdAndVersion.parse(objectId);
                validateAccessIdHasObjectId(drsObjectId, accessIdObject.getSynapseIdWithVersion());
                urlRequest = new FileHandleUrlRequest(userInfo, accessIdObject.getFileHandleId())
                        .withAssociation(accessIdObject.getAssociateType(), drsObjectId.getId().toString());
                break;
            default:
                throw new UnsupportedOperationException("Unexpected DRS Object type.");
        }

        final String url = fileHandleManager.getRedirectURLForFileHandle(urlRequest);
        final AccessUrl accessURL = new AccessUrl();
        accessURL.setUrl(url);
        return accessURL;
    }

    private void validateAccessIdHasObjectId(final IdAndVersion objectIdInRequest, final IdAndVersion objectIdASPartOfAccessId) {
        if (!objectIdASPartOfAccessId.equals(objectIdInRequest)) {
            throw new IllegalArgumentException("AccessId contains different drsObject Id.");
        }
    }

    static DrsObject createDrsObject(FileEntity file, FileHandle fileHandle, IdAndVersion idAndVersion) {
        final DrsObject result = new DrsObject();
        result.setId(idAndVersion.toString());
        result.setName(file.getName());
        result.setSelf_uri(DRS_URI + idAndVersion.toString());
        result.setVersion(idAndVersion.getVersion().get().toString());
        result.setCreated_time(file.getCreatedOn());
        result.setUpdated_time(file.getModifiedOn());
        result.setDescription(file.getDescription());
        final List<AccessMethod> accessMethods = new ArrayList<>();
        final AccessId accessId = new AccessId.Builder().setAssociateType(FileHandleAssociateType.FileEntity)
                .setSynapseIdWithVersion(idAndVersion).setFileHandleId(file.getDataFileHandleId()).build();
        final AccessMethod accessMethod = new AccessMethod();
        accessMethod.setType(AccessMethodType.https);
        accessMethod.setAccess_id(accessId.encode());
        accessMethods.add(accessMethod);
        result.setAccess_methods(accessMethods);
        result.setSize(fileHandle.getContentSize());
        result.setMime_type(fileHandle.getContentType());
        final List<Checksum> checksums = new ArrayList<>();
        final Checksum checksum = new Checksum();
        checksum.setChecksum(fileHandle.getContentMd5());
        checksum.setType(ChecksumType.md5);
        checksums.add(checksum);
        result.setChecksums(checksums);
        return result;
    }

    static DrsObject createDrsObject(Dataset dataset, IdAndVersion idAndVersion) {
        final DrsObject result = new DrsObject();
        result.setId(idAndVersion.toString());
        result.setName(dataset.getName());
        result.setSelf_uri(DRS_URI + idAndVersion.toString());
        result.setVersion(idAndVersion.getVersion().get().toString());
        result.setCreated_time(dataset.getCreatedOn());
        result.setUpdated_time(dataset.getModifiedOn());
        result.setDescription(dataset.getDescription());
        final List<Content> contentList = new ArrayList<>();
        if (dataset.getItems() != null) {
            dataset.getItems().forEach(entityRef -> {
                final String fileIdWithVersion = KeyFactory.idAndVersion(entityRef.getEntityId(), entityRef.getVersionNumber()).toString();
                final Content content = new Content();
                content.setId(fileIdWithVersion);
                // Name of file should be unique in the bundle, So file id with version is used as name.
                content.setName(fileIdWithVersion);
                content.setDrs_uri(DRS_URI + fileIdWithVersion);
                contentList.add(content);

            });
        }
        result.setContents(contentList);
        result.setSize(dataset.getSize());
        result.setChecksums(Collections.singletonList(
                new Checksum().setChecksum(dataset.getChecksum()).setType(ChecksumType.md5)));
        return result;
    }

    static DrsObject createDrsObject(final FileHandle fileHandle) {
        final DrsObject result = new DrsObject();
        result.setId("fh" + fileHandle.getId());
        result.setName(fileHandle.getFileName());
        result.setSelf_uri(DRS_URI + "fh" + fileHandle.getId());
        result.setCreated_time(fileHandle.getCreatedOn());
        result.setUpdated_time(fileHandle.getModifiedOn());
        result.setVersion(null);
        result.setDescription(null);
        final List<AccessMethod> accessMethods = new ArrayList<>();
        final AccessId accessId = new AccessId.Builder().setFileHandleId(fileHandle.getId()).build();
        final AccessMethod accessMethod = new AccessMethod();
        accessMethod.setType(AccessMethodType.https);
        accessMethod.setAccess_id(accessId.encode());
        accessMethods.add(accessMethod);
        result.setAccess_methods(accessMethods);
        result.setSize(fileHandle.getContentSize());
        result.setMime_type(fileHandle.getContentType());
        final List<Checksum> checksums = new ArrayList<>();
        final Checksum checksum = new Checksum();
        checksum.setChecksum(fileHandle.getContentMd5());
        checksum.setType(ChecksumType.md5);
        checksums.add(checksum);
        result.setChecksums(checksums);
        return result;
    }
}
