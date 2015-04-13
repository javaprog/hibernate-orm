//$Id$
/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.connection;

import java.io.File;
import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.HibernatePersistence;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class DataSourceInjectionTest {
    @Test
	public void testDatasourceInjection() throws Exception {
		File current = new File(".");
		File sub = new File(current, "puroot");
		sub.mkdir();
		PersistenceUnitInfoImpl info = new PersistenceUnitInfoImpl( sub.toURI().toURL(), new String[]{} );
		try {
			EntityManagerFactory emf = new HibernatePersistence().createContainerEntityManagerFactory( info, null );
			try {
				emf.createEntityManager().createQuery( "select i from Item i" ).getResultList();
			}
			finally {
				try {
					emf.close();
				}
				catch (Exception ignore) {
				}
			}
			Assert.fail( "FakeDatasource should have been used" );
		}
		catch (FakeDataSourceException fde) {
			//success
		}
		finally {
			sub.delete();
		}
	}
}
