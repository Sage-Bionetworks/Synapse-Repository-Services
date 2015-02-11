package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.FILES_CANNOT_HAVE_CHILDREN;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.LOCATIONABLE_HAS_MORE_THAN_ONE_LOCATION;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.LOCATIONABLE_HAS_NO_LOCATIONS;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.LOCATION_DATA_NOT_IN_S3;
import static org.sagebionetworks.repo.manager.EntityTypeConvertionError.NOT_LOCATIONABLE;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class EntityTypeConverterImpl implements EntityTypeConverter {

	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	NodeDAO nodeDao;	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	LocationHelper locationHelper;
	@Autowired
	UserManager userManager;
	
	/**
	 * For spring
	 */
	public EntityTypeConverterImpl(){};
	
	/**
	 * For tests
	 * @param nodeDao
	 * @param authorizationManager
	 */
	public EntityTypeConverterImpl(NodeDAO nodeDao,
			AuthorizationManager authorizationManager, EntityManager entityManager) {
		super();
		this.nodeDao = nodeDao;
		this.authorizationManager = authorizationManager;
		this.entityManager = entityManager;
	}



	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Entity convertOldTypeToNew(UserInfo user, Entity entity) throws UnauthorizedException, DatastoreException, NotFoundException {
		if(entity == null){
			throw new IllegalArgumentException("Entity cannot be null");
		}
		if(!(entity instanceof Locationable)){
			NOT_LOCATIONABLE.throwException();
		}
		Locationable locationable = (Locationable) entity;
		UserInfo.validateUserInfo(user);
		// Must have update permission on the entity.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(user, entity.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
	
		// First get the version for the entity.
		String newEtag = nodeDao.lockNodeAndIncrementEtag(entity.getId(), entity.getEtag(), ChangeType.UPDATE);
		// Studies are converted to folders, while all other types are converted to files.
		if(entity instanceof Study){
			return convertToFolder(user, locationable, newEtag);
		}else{
			return convertToFile(user, locationable, newEtag);
		}
	}

	/**
	 * Convert to a File.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	private FileEntity convertToFile(UserInfo user, Locationable entity, String newEtag) throws DatastoreException, UnauthorizedException, NotFoundException {
		// We can only convert to files if this entity has no children.
		if(!nodeDao.getChildrenIds(entity.getId()).isEmpty()){
			FILES_CANNOT_HAVE_CHILDREN.throwException();
		}
		// Create a file handle for each version.
		List<VersionData> pairs = createFileHandleForForEachVersion(user, entity, true);
		for(VersionData pair: pairs){
			Long verionNumber = pair.getVersionNumber();
			String fileHandleId = pair.getFileHandle().getId();
			NamedAnnotations newAnnos = convertAnnotations(entity, verionNumber);
			// replace this version
			nodeDao.replaceVersion(entity.getId(), verionNumber, newAnnos, fileHandleId);
		}
		// the last step is to change the type
		nodeDao.changeNodeType(entity.getId(), newEtag, "file");
		// get the changed entity.
		return entityManager.getEntity(user, entity.getId(), FileEntity.class);
	}

	/**
	 * Convert to a Folder.
	 * @param user
	 * @param entity
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	private Folder convertToFolder(UserInfo user, Locationable entity, String newEtag) throws DatastoreException, UnauthorizedException, NotFoundException {
		// Create a file handle for each version.
		List<VersionData> pairs = createFileHandleForForEachVersion(user, entity, false);
		// The latest version is fist but we need to process these in the original order.
		FileEntity child = new FileEntity();
		child.setParentId(entity.getId());
		child.setName(entity.getName());
		Collections.reverse(pairs);
		for(VersionData pair: pairs){
			Long verionNumber = pair.getVersionNumber();
			String fileHandleId = pair.getFileHandle().getId();
			NamedAnnotations newAnnos = convertAnnotations(entity, verionNumber);
			// replace this version
			nodeDao.replaceVersion(entity.getId(), verionNumber, newAnnos, null);
			// Get the user that modified this version.
			UserInfo modifiedUser = userManager.getUserInfo(Long.parseLong(pair.getModifiedBy()));
			//  create the child
			child.setDataFileHandleId(fileHandleId);
			child.setVersionComment(pair.getVersionComments());
			child.setVersionLabel(pair.getVersionLabel());
			if(child.getId() == null){
				// Create the child
				String id = entityManager.createEntity(modifiedUser, child, null);
				child = entityManager.getEntity(user, id, FileEntity.class);
			}else{
				// update the child
				entityManager.updateEntity(modifiedUser, child, true, null);
				child = entityManager.getEntity(user,child.getId(), FileEntity.class);
			}
		}
		// the last step is to change the type
		nodeDao.changeNodeType(entity.getId(), newEtag, "folder");
		// Return the entity in its new form.s
		return entityManager.getEntity(user, entity.getId(), Folder.class);
	}

	public NamedAnnotations convertAnnotations(Locationable entity,
			Long verionNumber) throws NotFoundException {
		// Re-write this version
		NamedAnnotations annos = nodeDao.getAnnotationsForVersion(entity.getId(), verionNumber);
		NamedAnnotations newAnnos = new NamedAnnotations();
		// Copy the additional annos first
		newAnnos.getAdditionalAnnotations().addAll(annos.getAdditionalAnnotations());
		// Override with any of the primary annotations.
		newAnnos.getAdditionalAnnotations().addAll(annos.getPrimaryAnnotations());
		return newAnnos;
	}

	/**
	 * Create a FileHandle for each version.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<VersionData> createFileHandleForForEachVersion(UserInfo user, Locationable entity, boolean atLeastOneLocationRequired) throws DatastoreException,
			NotFoundException {
		// We need to create file handle for each version.
		List<VersionData> pairs = new LinkedList<VersionData>();
		List<Long> versions = nodeDao.getVersionNumbers(entity.getId());
		if(versions != null){
			for(Long versionNumber: versions){
				Locationable location = (Locationable) entityManager.getEntityForVersion(user, entity.getId(), versionNumber, entity.getClass());
				if(location.getLocations() == null || location.getLocations().isEmpty()){
					if(atLeastOneLocationRequired){
						LOCATIONABLE_HAS_NO_LOCATIONS.throwException();
					}else{
						return new LinkedList<VersionData>();
					}
				}
				if(location.getLocations().size() > 1){
					LOCATIONABLE_HAS_MORE_THAN_ONE_LOCATION.throwException();
				}
				LocationData data = location.getLocations().get(0);
				FileHandle fileHandle = null;
				if(LocationTypeNames.awss3.equals(data.getType())){
					// S3 file handle.
					S3FileHandle s3Handle = new S3FileHandle();
					s3Handle.setKey(locationHelper.getS3KeyFromS3Url(data.getPath()));
					s3Handle.setBucketName(StackConfiguration.getS3Bucket());
					// Lookup the file size in s3
					try {
						ObjectMetadata meta = s3Client.getObjectMetadata(s3Handle.getBucketName(), s3Handle.getKey());
						s3Handle.setContentSize(meta.getContentLength());
					} catch (Exception e) {
						LOCATION_DATA_NOT_IN_S3.throwException();
					}
					s3Handle.setContentType(location.getContentType());
					setCreatedOnAndBy(location, s3Handle);
					s3Handle.setFileName(location.getName());
					s3Handle.setContentMd5(location.getMd5());
					fileHandle = fileHandleDao.createFile(s3Handle);
				}else{
					// external file handle
					ExternalFileHandle efh = new ExternalFileHandle();
					efh.setContentType(location.getContentType());
					efh.setExternalURL(data.getPath());
					efh.setFileName(location.getName());
					setCreatedOnAndBy(location, efh);
					fileHandle = fileHandleDao.createFile(efh);
				}
				pairs.add(new VersionData(versionNumber, fileHandle, location.getModifiedBy(), location.getVersionLabel(), location.getVersionComment()));
			}
		}
		return pairs;
	}

	/**
	 * 
	 * CreatedOn needs to match the when the version was modified. CreatedBy
	 * need to match the user that created the version.
	 * 
	 * @param location
	 * @param efh
	 */
	public void setCreatedOnAndBy(Locationable location, FileHandle efh) {
		efh.setCreatedBy(location.getModifiedBy());
		efh.setCreatedOn(location.getModifiedOn());
	}
	
}
