package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for CompetitionBackupDriverImpl.
 * @author bkng
 */
public class CompetitionBackupDriverTest {
	
	CompetitionBackupDriver sourceDriver = null;
	CompetitionBackupDriver destinationDriver = null;
	
	Map<String, Participant> srcParts;
	Map<String, Competition> srcComps;
	Map<String, Participant> dstParts;
	Map<String, Competition> dstComps;
	
	private CompetitionDAO createCompetitionDAO(final Map<String, Competition> comps) {
		return (CompetitionDAO)Proxy.newProxyInstance(CompetitionBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{CompetitionDAO.class},
                new InvocationHandler() {
					private long nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(CompetitionDAO.class.getMethod("get", String.class))) {
							Competition comp = comps.get((String)args[0]);
							if (comp==null) throw new NotFoundException();
							return comp;
						} else if (method.equals(CompetitionDAO.class.getMethod("create", Competition.class, Long.class))) {
							Competition comp = (Competition)args[0];
							if (comp.getId()==null) {
								if (comps.containsKey(""+nextKey)) throw new IllegalStateException();
								comp.setId("" + (nextKey++));
							} else {
								if (comps.containsKey(comp.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(comp.getId())+1;
							}
							comps.put(comp.getId(), comp);
							return comp.getId();
						} else if (method.equals(CompetitionDAO.class.getMethod("updateFromBackup", Competition.class))) {
							Competition comp = (Competition)args[0];
							if (comp.getId()==null || !comps.containsKey(comp.getId())) throw new RuntimeException("doesn't exist");
							comps.put(comp.getId(), comp);
							return null;
						} else {
							throw new IllegalArgumentException(method.getName());
						}
					}
		});
	}
	
	private ParticipantDAO createParticipantDAO(final Map<String, Participant> parts) {
		return (ParticipantDAO)Proxy.newProxyInstance(CompetitionBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{ParticipantDAO.class},
                new InvocationHandler() {
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(ParticipantDAO.class.getMethod("create", Participant.class))) {
					Participant p = (Participant)args[0];
					parts.put(p.getUserId(), p);
					return null;
				} else if (method.equals(ParticipantDAO.class.getMethod("getAllByCompetition", String.class, long.class, long.class))) {
					String compId = (String)args[0];
					List<Participant> result = new ArrayList<Participant>();
					for (Participant p : parts.values()) if (compId.equals(p.getCompetitionId())) result.add(p);
					return result;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private Competition createCompetition() throws Exception {
		Competition comp = new Competition();
		comp.setContentSource("contentSource");
		comp.setCreatedOn(new Date());
		comp.setDescription("description");
		comp.setEtag("eTag");
		comp.setId("123");
		comp.setName("name");
		comp.setOwnerId("456");
		comp.setStatus(CompetitionStatus.CLOSED);
		return comp;
	}
	
	private Participant createParticipant(String compId, String userId) throws Exception {
		Participant part = new Participant();
		part.setCompetitionId(compId);
		part.setCreatedOn(new Date());
		part.setUserId(userId);
		return part;
	}
	
	@Before
	public void before() throws Exception {
		srcParts = new HashMap<String, Participant>();
		srcComps = new HashMap<String, Competition>();
		dstParts = new HashMap<String, Participant>();
		dstComps = new HashMap<String, Competition>();
		CompetitionDAO srcCompetitionDAO = createCompetitionDAO(srcComps);
		Competition comp = createCompetition();
		String id = srcCompetitionDAO.create(comp, Long.parseLong(comp.getOwnerId()));
		assertNotNull(id);
		ParticipantDAO srcParticipantDAO = createParticipantDAO(srcParts);
		int numParts = 3;
		for (int i = 0; i < numParts; i++) {
			srcParticipantDAO.create(createParticipant(comp.getId(), "" + i));
		}
		assertEquals(1, srcComps.size());
		assertEquals(numParts, srcParts.size());
		CompetitionDAO dstCompetitionDAO = createCompetitionDAO(dstComps);
		ParticipantDAO dstParticipantDAO = createParticipantDAO(dstParts);
		sourceDriver = new CompetitionBackupDriver(srcCompetitionDAO, srcParticipantDAO);
		destinationDriver = new CompetitionBackupDriver(dstCompetitionDAO, dstParticipantDAO);
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException, ConflictingUpdateException{
		// Create a temp file
		File temp = File.createTempFile("CompetitionBackupDriverTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			Set<String> ids = new HashSet<String>(); 
			for (String key : srcComps.keySet()) ids.add(key);
			sourceDriver.writeBackup(temp, progress, ids);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertTrue(dstParts.isEmpty());
			assertTrue(dstComps.isEmpty());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(srcParts, dstParts);
			assertEquals(srcComps, dstComps);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}	

}
