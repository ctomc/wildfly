/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CONNECTORS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.REMOTE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;


/**
 */
public class EJB3Subsystem14Parser extends EJB3Subsystem13Parser {

    public static final EJB3Subsystem14Parser INSTANCE = new EJB3Subsystem14Parser();

    protected EJB3Subsystem14Parser() {
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case DEFAULT_SECURITY_DOMAIN: {
                parseDefaultSecurityDomain(reader, ejb3SubsystemAddOperation);
                break;
            }
            case REMOTE: {
                final String namespaceURI = reader.getNamespaceURI();
                final EJB3SubsystemNamespace ejb3SubsystemNamespace = EJB3SubsystemNamespace.forUri(namespaceURI);
                if (this.allowsRemoteConnectorOutsideOfConnectorsElement(ejb3SubsystemNamespace)) {
                    super.parseRemote(reader, operations);
                } else {
                    throw unexpectedElement(reader);
                }
                break;
            }
            case CONNECTORS: {
                // parse the <connectors> element
                parseConnectors(reader, operations);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_1_4;
    }

    private void parseDefaultSecurityDomain(final XMLExtendedStreamReader reader, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    /**
     * Parses the <code>connectors</code> element in the EJB3 subsystem
     *
     * @param reader The XML reader
     * @param operations The management operations to which any new operation(s), created out of the parsed xml content, will be added
     * @throws XMLStreamException
     */
    private void parseConnectors(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        // we don't expect any attributes for the connectors element
        requireNoAttributes(reader);

        // create an ADD operation for /subsystem=ejb3/service=connectors
        final PathAddress connectorsAddress = SUBSYSTEM_PATH.append(SERVICE, CONNECTORS);
        final ModelNode addConnectorsOperation = Util.createAddOperation(connectorsAddress);
        // add this ADD operation to the list of management operations
        operations.add(addConnectorsOperation);

        // parse the child element(s)
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case REMOTE: {
                    // parse the <remote> element
                    parseRemote(reader, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * Parses the <code>remote</code> element within the <code>connectors</code> element
     *
     * @param reader The XML reader
     * @param operations The management operations to which any new operation(s) will be added
     * @throws XMLStreamException
     */
    @Override
    protected void parseRemote(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> required = EnumSet.of(EJB3SubsystemXMLAttribute.NAME, EJB3SubsystemXMLAttribute.CONNECTOR_REF, EJB3SubsystemXMLAttribute.THREAD_POOL_NAME);
        // create an ADD operation whose address will be filled in later after we parse the "name" of the
        // remote connector
        ModelNode addRemoteConnectorOperation = Util.createEmptyOperation(ModelDescriptionConstants.ADD, null);
        String connectorName = null;
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    connectorName = value;
                    break;
                case CONNECTOR_REF:
                    EJB3RemoteResourceDefinition.CONNECTOR_REF.parseAndSetParameter(value, addRemoteConnectorOperation, reader);
                    break;
                case THREAD_POOL_NAME:
                    EJB3RemoteResourceDefinition.THREAD_POOL_NAME.parseAndSetParameter(value, addRemoteConnectorOperation, reader);
                    break;
                case EJB_CHANNEL_NAME:
                    EJB3RemoteResourceDefinition.EJB_CHANNEL_NAME.parseAndSetParameter(value, addRemoteConnectorOperation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // set the ADDRESS of the ADD operation, now that we know the path to the resource
        // /subsystem=ejb3/service=connectors/remote=<connectorName>
        final PathAddress connectorAddress = SUBSYSTEM_PATH.append(SERVICE, CONNECTORS).append(REMOTE, connectorName);
        addRemoteConnectorOperation.get(ModelDescriptionConstants.ADDRESS).set(connectorAddress.toModelNode());

        // add this ADD operation to the list of management operations
        operations.add(addRemoteConnectorOperation);

        // set the address for this operation
        final Set<EJB3SubsystemXMLElement> parsedElements = new HashSet<EJB3SubsystemXMLElement>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CHANNEL_CREATION_OPTIONS: {
                    if (parsedElements.contains(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS)) {
                        throw unexpectedElement(reader);
                    }
                    parsedElements.add(EJB3SubsystemXMLElement.CHANNEL_CREATION_OPTIONS);
                    this.parseChannelCreationOptions(reader, connectorAddress, operations);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private boolean allowsRemoteConnectorOutsideOfConnectorsElement(final EJB3SubsystemNamespace ejb3SubsystemNamespace) {
        if (ejb3SubsystemNamespace == EJB3SubsystemNamespace.EJB3_1_0 || ejb3SubsystemNamespace == EJB3SubsystemNamespace.EJB3_1_1
                || ejb3SubsystemNamespace == EJB3SubsystemNamespace.EJB3_1_2 || ejb3SubsystemNamespace == EJB3SubsystemNamespace.EJB3_1_3) {
            return true;
        }
        return false;
    }
}
