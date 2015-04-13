/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.cfgxml.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.xml.XsdException;

import org.jboss.logging.Logger;

import org.xml.sax.SAXException;

/**
 * @author Steve Ebersole
 */
public class JaxbCfgProcessor {
	private static final Logger log = Logger.getLogger( JaxbCfgProcessor.class );

	public static final String HIBERNATE_CONFIGURATION_URI = "http://www.hibernate.org/xsd/orm/cfg";

	private final ClassLoaderService classLoaderService;

	public JaxbCfgProcessor(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	public JaxbCfgHibernateConfiguration unmarshal(InputStream stream, Origin origin) {
		try {
			XMLEventReader staxReader = staxFactory().createXMLEventReader( stream );
			try {
				return unmarshal( staxReader, origin );
			}
			finally {
				try {
					staxReader.close();
				}
				catch ( Exception ignore ) {
				}
			}
		}
		catch ( XMLStreamException e ) {
			throw new HibernateException( "Unable to create stax reader", e );
		}
	}

	private XMLInputFactory staxFactory;

	private XMLInputFactory staxFactory() {
		if ( staxFactory == null ) {
			staxFactory = buildStaxFactory();
		}
		return staxFactory;
	}

	@SuppressWarnings( { "UnnecessaryLocalVariable" })
	private XMLInputFactory buildStaxFactory() {
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		staxFactory.setXMLResolver( LocalXmlResourceResolver.INSTANCE );
		return staxFactory;
	}

	@SuppressWarnings( { "unchecked" })
	private JaxbCfgHibernateConfiguration unmarshal(XMLEventReader staxEventReader, final Origin origin) {
		XMLEvent event;
		try {
			event = staxEventReader.peek();
			while ( event != null && !event.isStartElement() ) {
				staxEventReader.nextEvent();
				event = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new HibernateException( "Error accessing stax stream", e );
		}

		if ( event == null ) {
			throw new HibernateException( "Could not locate root element" );
		}

		if ( !isNamespaced( event.asStartElement() ) ) {
			// if the elements are not namespaced, wrap the reader in a reader which will namespace them as pulled.
			log.debug( "cfg.xml document did not define namespaces; wrapping in custom event reader to introduce namespace information" );
			staxEventReader = new NamespaceAddingEventReader( staxEventReader, HIBERNATE_CONFIGURATION_URI );
		}

		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( JaxbCfgHibernateConfiguration.class );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( schema() );
			unmarshaller.setEventHandler( handler );
			return (JaxbCfgHibernateConfiguration) unmarshaller.unmarshal( staxEventReader );
		}
		catch ( JAXBException e ) {
			throw new ConfigurationException(
					"Unable to perform unmarshalling at line number " + handler.getLineNumber()
							+ " and column " + handler.getColumnNumber()
							+ " in " + origin.getType().name() + " " + origin.getName()
							+ ". Message: " + handler.getMessage(), e
			);
		}
	}

	private boolean isNamespaced(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
	}

	private Schema schema;

	private Schema schema() {
		if ( schema == null ) {
			schema = resolveLocalSchema( "org/hibernate/hibernate-configuration-4.0.xsd" );
		}
		return schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
		URL url = classLoaderService.locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource( url.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( schemaLanguage );
				return schemaFactory.newSchema( source );
			}
			catch ( SAXException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			catch ( IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					log.debugf( "Problem closing schema stream [%s]", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaName );
		}
	}

	static class ContextProvidingValidationEventHandler implements ValidationEventHandler {
		private int lineNumber;
		private int columnNumber;
		private String message;

		@Override
		public boolean handleEvent(ValidationEvent validationEvent) {
			ValidationEventLocator locator = validationEvent.getLocator();
			lineNumber = locator.getLineNumber();
			columnNumber = locator.getColumnNumber();
			message = validationEvent.getMessage();
			return false;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public int getColumnNumber() {
			return columnNumber;
		}

		public String getMessage() {
			return message;
		}
	}

	public class NamespaceAddingEventReader extends EventReaderDelegate {
		private final XMLEventFactory xmlEventFactory;
		private final String namespaceUri;

		public NamespaceAddingEventReader(XMLEventReader reader, String namespaceUri) {
			this( reader, XMLEventFactory.newInstance(), namespaceUri );
		}

		public NamespaceAddingEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory, String namespaceUri) {
			super( reader );
			this.xmlEventFactory = xmlEventFactory;
			this.namespaceUri = namespaceUri;
		}

		private StartElement withNamespace(StartElement startElement) {
			// otherwise, wrap the start element event to provide a default namespace mapping
			final List<Namespace> namespaces = new ArrayList<Namespace>();
			namespaces.add( xmlEventFactory.createNamespace( "", namespaceUri ) );
			Iterator<?> originalNamespaces = startElement.getNamespaces();
			while ( originalNamespaces.hasNext() ) {
				namespaces.add( (Namespace) originalNamespaces.next() );
			}
			return xmlEventFactory.createStartElement(
					new QName( namespaceUri, startElement.getName().getLocalPart() ),
					startElement.getAttributes(),
					namespaces.iterator()
			);
		}

		@Override
		public XMLEvent nextEvent() throws XMLStreamException {
			XMLEvent event = super.nextEvent();
			if ( event.isStartElement() ) {
				return withNamespace( event.asStartElement() );
			}
			return event;
		}

		@Override
		public XMLEvent peek() throws XMLStreamException {
			XMLEvent event = super.peek();
			if ( event.isStartElement() ) {
				return withNamespace( event.asStartElement() );
			}
			else {
				return event;
			}
		}
	}
}
