package org.sagebionetworks.tool.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import static org.junit.Assert.*;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;

import com.thoughtworks.xstream.XStream;

// this class to be deleted after the 0.12 migration
public class PrincipalRetriever012Test {

	@Test
	public void testSerialization() throws Exception {
		File file=null;
		try {
			Collection<PrincipalBackup> pbs = new HashSet<PrincipalBackup>();
			PrincipalBackup pb = new PrincipalBackup();
			UserGroup ug = new UserGroup();
			UserProfile up = new UserProfile();
			pb.setUserGroup(ug);
			pb.setUserProfile(up);
			pbs.add(pb);
			file = File.createTempFile("foo", ".zip");
			PrincipalRetriever012.writePrincipalBackups(pbs, file);
			// now see if it can be read back...
			InputStream in = new FileInputStream(file);
			ZipInputStream zis = new ZipInputStream(in);
			assertNotNull(zis.getNextEntry());
			InputStreamReader reader = new InputStreamReader(zis);
			XStream xstream = new XStream();
			Collection<PrincipalBackup> pbs2 = (Collection<PrincipalBackup>)xstream.fromXML(reader);
			in.close();
			assertEquals(pbs, pbs2);
		} finally {
			if (file!=null) file.delete();
		}
	}

}
