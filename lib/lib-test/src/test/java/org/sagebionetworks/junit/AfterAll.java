package org.sagebionetworks.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * When writing parallel tests, it is common to find that several tests need similar objects created before they can
 * run, but after spring initializes. Annotating a <code>public void</code> method with <code>&#064;BeforeAll</code>
 * causes that method to be run before all the {@link org.junit.Test} methods. The <code>&#064;BeforeAll</code> methods
 * of superclasses will be run before the one on the subclass.
 * </p>
 * 
 * Here is a simple example:
 * 
 * <pre>
 * public class Example {
 * 	ConcurrentList empty;
 * 
 * 	&#064;BeforeAll
 * 	public void initializeAll() {
 * 		empty = new ArrayList();
 * 	}
 * 
 * 	&#064;Before
 * 	public void initialize() {
 * 	}
 * 
 * 	&#064;Test public void size() {
 *       ...
 *       empty.add();
 *    }
 * 
 * 	&#064;Test public void remove() {
 *       ...
 *    }
 * }
 * </pre>
 * 
 * @see org.junit.BeforeClass
 * @see org.junit.After
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterAll {
}

