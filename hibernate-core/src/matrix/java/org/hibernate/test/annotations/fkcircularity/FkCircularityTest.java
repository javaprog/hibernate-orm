// $Id$
package org.hibernate.test.annotations.fkcircularity;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jboss.logging.Logger;

import junit.framework.TestCase;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest extends TestCase {
	private static final Logger log = Logger.getLogger( FkCircularityTest.class );

	private ServiceRegistry serviceRegistry;

	@Override
    protected void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@Override
    protected void tearDown() {
        if (serviceRegistry != null) ServiceRegistryBuilder.destroy(serviceRegistry);
	}

	public void testJoinedSublcassesInPK() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(A.class);
			config.addAnnotatedClass(B.class);
			config.addAnnotatedClass(C.class);
			config.addAnnotatedClass(D.class);
			config.buildSessionFactory( serviceRegistry );
			String[] schema = config
					.generateSchemaCreationScript(new SQLServerDialect());
			for (String s : schema) {
                log.debug(s);
			}
            log.debug("success");
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}

	public void testDeepJoinedSuclassesHierachy() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(ClassA.class);
			config.addAnnotatedClass(ClassB.class);
			config.addAnnotatedClass(ClassC.class);
			config.addAnnotatedClass(ClassD.class);
			config.buildSessionFactory( serviceRegistry );
			String[] schema = config
					.generateSchemaCreationScript(new HSQLDialect());
			for (String s : schema) {
                log.debug(s);
			}
            log.debug("success");
		} catch (Exception e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
            log.debug(writer.toString());
			fail(e.getMessage());
		}
	}
}