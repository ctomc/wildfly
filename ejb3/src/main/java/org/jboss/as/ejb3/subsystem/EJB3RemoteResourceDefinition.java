/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the EJB remote service
 * <p/>
 * @author : Jaikiran Pai
 */
public class EJB3RemoteResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition CONNECTOR_REF =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CONNECTOR_REF, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition EJB_CHANNEL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EJB_CHANNEL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(EJBRemoteConnectorService.DEFAULT_EJB_CHANNEL_NAME))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();


    private static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(CONNECTOR_REF.getName(), CONNECTOR_REF);
        map.put(THREAD_POOL_NAME.getName(), THREAD_POOL_NAME);
        map.put(EJB_CHANNEL_NAME.getName(), EJB_CHANNEL_NAME);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    private final EJB3RemoteConnectorAdd ejb3RemoteConnectorAddHandler;

    /**
     * @deprecated Use {@link #EJB3RemoteResourceDefinition(org.jboss.as.controller.PathElement, org.jboss.as.controller.descriptions.ResourceDescriptionResolver, EJB3RemoteConnectorAdd, EJB3RemoteConnectorRemove)}
     *              instead
     */
    @Deprecated
    EJB3RemoteResourceDefinition() {
        this(EJB3SubsystemModel.REMOTE_SERVICE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.REMOTE),
                new EJB3RemoteConnectorAdd(true), new EJB3RemoteConnectorRemove(true));
    }

    EJB3RemoteResourceDefinition(final PathElement path, final ResourceDescriptionResolver descriptionResolver,
                                    final EJB3RemoteConnectorAdd addHandler, final EJB3RemoteConnectorRemove removeHandler) {
        super(path, descriptionResolver, addHandler, removeHandler);
        this.ejb3RemoteConnectorAddHandler = addHandler;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            // TODO: Make this read-write attribute
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // register channel-creation-options as sub model for EJB remote service
        resourceRegistration.registerSubModel(new ChannelCreationOptionResource(this.ejb3RemoteConnectorAddHandler));
    }

    static void registerTransformers_1_1_0(ResourceTransformationDescriptionBuilder transformationBuilder) {
        transformationBuilder.addChildRedirection(PathElement.pathElement(EJB3SubsystemModel.SERVICE, EJB3SubsystemModel.CONNECTORS), new RemoteConnectorPathAddressTransformer());

        // for 1.1.0 discard the "ejb-channel-name" attribute which was introduced in a later version
        final ResourceTransformationDescriptionBuilder remoteConnectorTransformationBuilder = transformationBuilder.addChildResource(EJB3SubsystemModel.REMOTE_SERVICE_PATH);
        remoteConnectorTransformationBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED, EJB_CHANNEL_NAME)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(EJBRemoteConnectorService.DEFAULT_EJB_CHANNEL_NAME)), EJB_CHANNEL_NAME);
        ChannelCreationOptionResource.registerTransformers_1_1_0(remoteConnectorTransformationBuilder);
    }

    private static class RemoteConnectorPathAddressTransformer implements PathAddressTransformer {

        @Override
        public PathAddress transform(PathElement current, Builder builder) {
            return PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    PathElement.pathElement(EJB3SubsystemModel.SERVICE, EJB3SubsystemModel.REMOTE));
        }
    }
}
