// $Id$
package org.hibernate.test.annotations.namingstrategy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import org.jboss.logging.Logger;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

import junit.framework.TestCase;

import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
public class NamingStrategyTest extends TestCase {
	private static final Logger log = Logger.getLogger( NamingStrategyTest.class );

	private ServiceRegistry serviceRegistry;

	@Override
    protected void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@Override
    protected void tearDown() {
        if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
	}

	public void testWithCustomNamingStrategy() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.setNamingStrategy(new DummyNamingStrategy());
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			config.buildSessionFactory( serviceRegistry );
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}

	public void testWithEJB3NamingStrategy() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.setNamingStrategy(EJB3NamingStrategy.INSTANCE);
			config.addAnnotatedClass(A.class);
			config.addAnnotatedClass(AddressEntry.class);
			config.buildSessionFactory( serviceRegistry );
			Mappings mappings = config.createMappings();
			boolean foundIt = false;

			for ( Iterator iter = mappings.iterateTables(); iter.hasNext();  ) {
				Table table = (Table) iter.next();
                log.info("testWithEJB3NamingStrategy table = " + table.getName());
				if ( table.getName().equalsIgnoreCase("A_ADDRESS")) {
					foundIt = true;
				}
				// make sure we use A_ADDRESS instead of AEC_address
				assertFalse("got table name mapped to: AEC_address (should be A_ADDRESS) which violates JPA-2 spec section 11.1.8 ([OWNING_ENTITY_NAME]_[COLLECTION_ATTRIBUTE_NAME])",table.getName().equalsIgnoreCase("AEC_address"));
			}
			assertTrue("table not mapped to A_ADDRESS which violates JPA-2 spec section 11.1.8",foundIt);
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}

	public void testWithoutCustomNamingStrategy() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Address.class);
			config.addAnnotatedClass(Person.class);
			config.buildSessionFactory( serviceRegistry );
		}
		catch( Exception e ) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}
}
