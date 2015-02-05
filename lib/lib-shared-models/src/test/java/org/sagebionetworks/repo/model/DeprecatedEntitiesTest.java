package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.TableEntity;

public class DeprecatedEntitiesTest {

	@Test
	public void testIsDeprecatedEntityTrue() {
		Entity[] depricated = new Entity[]
				{ new Study(),
				new Data(),
				new Preview(),
				new Analysis(),
				new Step(),
				new Code(),
				new PhenotypeData(),
				new GenotypeData(),
				new ExpressionData(),
				new RObject(),
				new GenomicData(),
				new Page(),
		};
		for(Entity e: depricated){
			assertTrue("Should be depricate: "+e.getClass().getName(),DeprecatedEntities.isDeprecated(e));
		}
	}
	
	@Test
	public void testIsDeprecatedEntityFalse() {
		Entity[] supported = new Entity[]
				{ new Project(),
				new Folder(),
				new FileEntity(),
				new TableEntity(),
				new Link(),
		};
		for(Entity e: supported){
			assertFalse("Should be supported: "+e.getClass().getName(),DeprecatedEntities.isDeprecated(e));
		}
	}
}
