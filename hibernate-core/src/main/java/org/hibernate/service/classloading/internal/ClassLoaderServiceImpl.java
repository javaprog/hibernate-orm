/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.service.classloading.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * Standard implementation of the service for interacting with class loaders
 *
 * @author Steve Ebersole
 */
public class ClassLoaderServiceImpl implements ClassLoaderService {
	private final LinkedHashSet<ClassLoader> classLoadingClassLoaders;
	private final ClassLoader resourcesClassLoader;

	public ClassLoaderServiceImpl(Map configVales) {
		this( determineClassLoaders( configVales ) );
	}

	private ClassLoaderServiceImpl(ClassLoader... classLoaders) {
		this( classLoaders[0], classLoaders[1], classLoaders[2], classLoaders[3] );
	}

	private static ClassLoader[] determineClassLoaders(Map configVales) {
		ClassLoader applicationClassLoader = (ClassLoader) configVales.get( AvailableSettings.APP_CLASSLOADER );
		ClassLoader resourcesClassLoader = (ClassLoader) configVales.get( AvailableSettings.RESOURCES_CLASSLOADER );
		ClassLoader hibernateClassLoader = (ClassLoader) configVales.get( AvailableSettings.HIBERNATE_CLASSLOADER );
		ClassLoader environmentClassLoader = (ClassLoader) configVales.get( AvailableSettings.ENVIRONMENT_CLASSLOADER );

		if ( hibernateClassLoader == null ) {
			hibernateClassLoader = ClassLoaderServiceImpl.class.getClassLoader();
		}

		if ( environmentClassLoader == null || applicationClassLoader == null ) {
			ClassLoader sysClassLoader = locateSystemClassLoader();
			ClassLoader tccl = locateTCCL();
			if ( environmentClassLoader == null ) {
				environmentClassLoader = sysClassLoader != null ? sysClassLoader : hibernateClassLoader;
			}
			if ( applicationClassLoader == null ) {
				applicationClassLoader = tccl != null ? tccl : hibernateClassLoader;
			}
		}

		if ( resourcesClassLoader == null ) {
			resourcesClassLoader = applicationClassLoader;
		}

		return new ClassLoader[] {
			applicationClassLoader,
			resourcesClassLoader,
			hibernateClassLoader,
			environmentClassLoader
		};
	}

	private static ClassLoader locateSystemClassLoader() {
		try {
			return ClassLoader.getSystemClassLoader();
		}
		catch ( Exception e ) {
			return null;
		}
	}

	private static ClassLoader locateTCCL() {
		try {
			return Thread.currentThread().getContextClassLoader();
		}
		catch ( Exception e ) {
			return null;
		}
	}

	public ClassLoaderServiceImpl(ClassLoader classLoader) {
		this( classLoader, classLoader, classLoader, classLoader );
	}

	public ClassLoaderServiceImpl(
			ClassLoader applicationClassLoader,
			ClassLoader resourcesClassLoader,
			ClassLoader hibernateClassLoader,
			ClassLoader environmentClassLoader) {
		this.classLoadingClassLoaders = new LinkedHashSet<ClassLoader>();
		classLoadingClassLoaders.add( applicationClassLoader );
		classLoadingClassLoaders.add( hibernateClassLoader );
		classLoadingClassLoaders.add( environmentClassLoader );

		this.resourcesClassLoader = resourcesClassLoader;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> Class<T> classForName(String className) {
		for ( ClassLoader classLoader : classLoadingClassLoaders ) {
			try {
				return (Class<T>) classLoader.loadClass( className );
			}
			catch ( Exception ignore) {
			}
		}
		throw new ClassLoadingException( "Unable to load class [" + className + "]" );
	}

	@Override
	public URL locateResource(String name) {
		// first we try name as a URL
		try {
			return new URL( name );
		}
		catch ( Exception ignore ) {
		}

		try {
			return resourcesClassLoader.getResource( name );
		}
		catch ( Exception ignore ) {
		}

		return null;
	}

	@Override
	public InputStream locateResourceStream(String name) {
		// first we try name as a URL
		try {
			return new URL( name ).openStream();
		}
		catch ( Exception ignore ) {
		}

		try {
			return resourcesClassLoader.getResourceAsStream( name );
		}
		catch ( Exception ignore ) {
		}

		return null;
	}

	@Override
	public List<URL> locateResources(String name) {
		ArrayList<URL> urls = new ArrayList<URL>();
		try {
			Enumeration<URL> urlEnumeration = resourcesClassLoader.getResources( name );
			if ( urlEnumeration != null && urlEnumeration.hasMoreElements() ) {
				while ( urlEnumeration.hasMoreElements() ) {
					urls.add( urlEnumeration.nextElement() );
				}
			}
		}
		catch ( Exception ignore ) {
		}

		return urls;
	}

}
