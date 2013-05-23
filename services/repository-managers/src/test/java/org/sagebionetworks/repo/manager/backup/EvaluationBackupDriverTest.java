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
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is a unit test for CompetitionBackupDriverImpl.
 * @author bkng
 */
public class EvaluationBackupDriverTest {
	
	EvaluationBackupDriver sourceDriver = null;
	EvaluationBackupDriver destinationDriver = null;
	
	Map<String, Participant> srcParts;
	Map<String, Evaluation> srcComps;
	Map<String, Participant> dstParts;
	Map<String, Evaluation> dstComps;
	
	private EvaluationDAO createCompetitionDAO(final Map<String, Evaluation> comps) {
		return (EvaluationDAO)Proxy.newProxyInstance(EvaluationBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{EvaluationDAO.class},
                new InvocationHandler() {
					private long nextKey = 0;
					@Override
					public Object invoke(Object synapseClient, Method method, Object[] args)
							throws Throwable {
						if (method.equals(EvaluationDAO.class.getMethod("get", String.class))) {
							Evaluation comp = comps.get((String)args[0]);
							if (comp==null) throw new NotFoundException();
							return comp;
						} else if (method.equals(EvaluationDAO.class.getMethod("create", Evaluation.class, Long.class))) {
							Evaluation comp = (Evaluation)args[0];
							if (comp.getId()==null) {
								if (comps.containsKey(""+nextKey)) throw new IllegalStateException();
								comp.setId("" + (nextKey++));
							} else {
								if (comps.containsKey(comp.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(comp.getId())+1;
							}
							comps.put(comp.getId(), comp);
							return comp.getId();
						} else if (method.equals(EvaluationDAO.class.getMethod("createFromBackup", Evaluation.class, Long.class))) {
							Evaluation comp = (Evaluation)args[0];
							if (comp.getId()==null) {
								if (comps.containsKey(""+nextKey)) throw new IllegalStateException();
								comp.setId("" + (nextKey++));
							} else {
								if (comps.containsKey(comp.getId())) throw new  RuntimeException("already exists");
								nextKey = Long.parseLong(comp.getId())+1;
							}
							comps.put(comp.getId(), comp);
							return comp.getId();
						} else if (method.equals(EvaluationDAO.class.getMethod("updateFromBackup", Evaluation.class))) {
							Evaluation comp = (Evaluation)args[0];
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
		return (ParticipantDAO)Proxy.newProxyInstance(EvaluationBackupDriverTest.class.getClassLoader(),
				new Class<?>[]{ParticipantDAO.class},
                new InvocationHandler() {
			@Override
			public Object invoke(Object synapseClient, Method method, Object[] args)
					throws Throwable {
				if (method.equals(ParticipantDAO.class.getMethod("create", Participant.class))) {
					Participant p = (Participant)args[0];
					parts.put(p.getUserId(), p);
					return Long.parseLong(p.getUserId());
				} else if (method.equals(ParticipantDAO.class.getMethod("getAllByEvaluation", String.class, long.class, long.class))) {
					String compId = (String)args[0];
					List<Participant> result = new ArrayList<Participant>();
					for (Participant p : parts.values()) if (compId.equals(p.getEvaluationId())) result.add(p);
					return result;
				} else {
					throw new IllegalArgumentException(method.getName());
				}
			}
		});
	}
	
	private Evaluation createCompetition() throws Exception {
		Evaluation comp = new Evaluation();
		comp.setContentSource("contentSource");
		comp.setCreatedOn(new Date());
		comp.setDescription("description");
		comp.setEtag("eTag");
		comp.setId("123");
		comp.setName("name");
		comp.setOwnerId("456");
		comp.setStatus(EvaluationStatus.CLOSED);
		return comp;
	}
	
	private Participant createParticipant(String compId, String userId) throws Exception {
		Participant part = new Participant();
		part.setEvaluationId(compId);
		part.setCreatedOn(new Date());
		part.setUserId(userId);
		return part;
	}
	
	@Before
	public void before() throws Exception {
		srcParts = new HashMap<String, Participant>();
		srcComps = new HashMap<String, Evaluation>();
		dstParts = new HashMap<String, Participant>();
		dstComps = new HashMap<String, Evaluation>();
		EvaluationDAO srcCompetitionDAO = createCompetitionDAO(srcComps);
		Evaluation comp = createCompetition();
		String id = srcCompetitionDAO.create(comp, Long.parseLong(comp.getOwnerId()));
		assertNotNull(id);
		ParticipantDAO srcParticipantDAO = createParticipantDAO(srcParts);
		int numParts = 3;
		for (int i = 0; i < numParts; i++) {
			srcParticipantDAO.create(createParticipant(comp.getId(), "" + i));
		}
		assertEquals(1, srcComps.size());
		assertEquals(numParts, srcParts.size());
		EvaluationDAO dstCompetitionDAO = createCompetitionDAO(dstComps);
		ParticipantDAO dstParticipantDAO = createParticipantDAO(dstParts);
		sourceDriver = new EvaluationBackupDriver(srcCompetitionDAO, srcParticipantDAO);
		destinationDriver = new EvaluationBackupDriver(dstCompetitionDAO, dstParticipantDAO);
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
