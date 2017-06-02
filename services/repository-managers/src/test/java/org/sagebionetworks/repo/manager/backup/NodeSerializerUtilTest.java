package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementBackup;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;

/**
 * Unit test for the NodeSerializerUtil.
 * @author jmhill
 *
 */
public class NodeSerializerUtilTest {

	
	@Test 
	public void testRoundTripPrincipalBackup() throws Exception {
		Collection<PrincipalBackup> pbs = new HashSet<PrincipalBackup>();
		PrincipalBackup pb = new PrincipalBackup();
		UserGroup ug = new UserGroup();
		ug.setId("101");
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		pb.setUserGroup(ug);
		UserProfile up = new UserProfile();
		up.setOwnerId(ug.getId());
		pb.setUserProfile(up);
		pbs.add(pb);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		NodeSerializerUtil.writePrincipalBackups(pbs, baos);
		baos.close();
		String serialized = new String(baos.toByteArray());
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		Collection<PrincipalBackup> pbs2 = NodeSerializerUtil.readPrincipalBackups(bais);
		assertEquals(pbs, pbs2);
	}

	@Test 
	public void testRoundTripAccessRequirementBackup() throws Exception {
		Collection<PrincipalBackup> pbs = new HashSet<PrincipalBackup>();
		AccessRequirementBackup pb = new AccessRequirementBackup();

		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy("101");
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy("102");
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("103");
		rod.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		accessRequirement.setConcreteType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		accessRequirement.setId(104L);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		NodeSerializerUtil.writePrincipalBackups(pbs, baos);
		baos.close();
		String serialized = new String(baos.toByteArray());
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		Collection<PrincipalBackup> pbs2 = NodeSerializerUtil.readPrincipalBackups(bais);
		assertEquals(pbs, pbs2);
	}
	

	/**
	 * These files contain old version of the NodeBackup xml.  We must always be able to read these 
	 * old xml files.
	 */
	public static final FileDetails[] PREVIOUS_NODE_BACKUP_FILES = new FileDetails[]{
		new FileDetails("node-backupV0.xml",342 ,54)
	};
	
	/**
	 * These files contain old version of the NodeBackup xml.  We must always be able to read these 
	 * old xml files.
	 */
	public static final FileDetails[] PREVIOUS_NODE_REVISION_FILES = new FileDetails[]{
		new FileDetails("node-revisionV0.xml",932 ,54)
	};



	/**
	 * Simple data structure for holding blob file data.
	 * 
	 * @author jmhill
	 *
	 */
	public static class FileDetails {
		
		String fileName;
		long randomSeed;
		int count;
		
		
		public FileDetails(String fileName, long randomSeed, int count) {
			super();
			this.fileName = fileName;
			this.randomSeed = randomSeed;
			this.count = count;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		public long getRandomSeed() {
			return randomSeed;
		}
		public void setRandomSeed(long randomSeed) {
			this.randomSeed = randomSeed;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		@Override
		public String toString() {
			return "BlobData [fileName=" + fileName + ", randomSeed="
					+ randomSeed + ", count=" + count + "]";
		}
		
	}
}
