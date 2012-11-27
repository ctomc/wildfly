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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ResourceDefinition} implementation for the core mod-cluster configuration resource.
 * <p/>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ModClusterConfigResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition ADVERTISE_SOCKET = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SOCKET, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition CONNECTOR = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CONNECTOR, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // TODO: Convert into xs:list of outbound socket binding names
    static final SimpleAttributeDefinition PROXY_LIST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_LIST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition PROXY_URL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROXY_URL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("/"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ADVERTISE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition ADVERTISE_SECURITY_KEY = SimpleAttributeDefinitionBuilder.create(CommonAttributes.ADVERTISE_SECURITY_KEY, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // TODO: Convert into an xs:list of host:context
    static final SimpleAttributeDefinition EXCLUDED_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.EXCLUDED_CONTEXTS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("ROOT,invoker,jbossws,juddi,console"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition AUTO_ENABLE_CONTEXTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.AUTO_ENABLE_CONTEXTS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STOP_CONTEXT_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STOP_CONTEXT_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SOCKET_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SOCKET_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(20))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_REMOVE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_REMOVE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition STICKY_SESSION_FORCE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.STICKY_SESSION_FORCE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition WORKER_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WORKER_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition MAX_ATTEMPTS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.MAX_ATTEMPTS, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_PACKETS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_PACKETS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition FLUSH_WAIT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.FLUSH_WAIT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition PING = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PING, ModelType.INT, true)
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition SMAX = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SMAX, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TTL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.TTL, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition NODE_TIMEOUT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.NODE_TIMEOUT, ModelType.INT, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setValidator(new IntRangeValidator(-1, true, true))
            .setCorrector(ZeroToNegativeOneParameterCorrector.INSTANCE)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition BALANCER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.BALANCER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition LOAD_BALANCING_GROUP = SimpleAttributeDefinitionBuilder.create(CommonAttributes.LOAD_BALANCING_GROUP, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .addAlternatives("domain")
            .build();

    static final SimpleAttributeDefinition SIMPLE_LOAD_PROVIDER = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, ModelType.INT, true)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(CommonAttributes.FACTOR)
                    //.setDefaultValue(new ModelNode(1))
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    // order here controls the order of writing into xml, should follow xsd schema
    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            ADVERTISE_SOCKET,
            PROXY_LIST,
            PROXY_URL,
            BALANCER,
            ADVERTISE,
            ADVERTISE_SECURITY_KEY,
            EXCLUDED_CONTEXTS,
            AUTO_ENABLE_CONTEXTS,
            STOP_CONTEXT_TIMEOUT,
            SOCKET_TIMEOUT,
            STICKY_SESSION,
            STICKY_SESSION_REMOVE,
            STICKY_SESSION_FORCE,
            WORKER_TIMEOUT,
            MAX_ATTEMPTS,
            FLUSH_PACKETS,
            FLUSH_WAIT,
            PING,
            SMAX,
            TTL,
            NODE_TIMEOUT,
            LOAD_BALANCING_GROUP, // was called "domain" in the 1.0 xsd
            CONNECTOR, // not in the 1.0 xsd
    };


    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTES_BY_NAME;

    /**
     * Runtime only.
     */
    public static final SimpleAttributeDefinition PORT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PORT, ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();
    public static final SimpleAttributeDefinition HOST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.HOST, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();
    public static final SimpleAttributeDefinition VIRUTAL_HOST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.VIRUTAL_HOST, ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();
    public static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CONTEXT, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition WAIT_TIME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WAIT_TIME, ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    private final boolean runtimeOnly;


    static {
        Map<String, SimpleAttributeDefinition> attrs = new HashMap<String, SimpleAttributeDefinition>();
        for (AttributeDefinition attr : ATTRIBUTES) {
            attrs.put(attr.getName(), (SimpleAttributeDefinition) attr);
        }
        ATTRIBUTES_BY_NAME = Collections.unmodifiableMap(attrs);
    }

    public ModClusterConfigResourceDefinition(boolean runtimeOnly) {
        super(ModClusterExtension.SUBSYSTEM_PATH,
                ModClusterExtension.getResourceDescriptionResolver(),
                ModClusterSubsystemAdd.INSTANCE,
                ModClusterSubsystemRemove.INSTANCE);
        this.runtimeOnly = runtimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
        resourceRegistration.registerReadWriteAttribute(SIMPLE_LOAD_PROVIDER, null, new ReloadRequiredWriteAttributeHandler(SIMPLE_LOAD_PROVIDER));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        // Register default operations. Note that "connector" is a required parameter on subsystem ADD operation.
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        // Metric for the  dynamic-load-provider
        EnumSet<OperationEntry.Flag> runtimeOnlyFlags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);

        final DescriptionProvider addMetric = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_METRIC, rootResolver, LoadMetricDefinition.ATTRIBUTES);
        resourceRegistration.registerOperationHandler(CommonAttributes.ADD_METRIC, ModClusterAddMetric.INSTANCE, addMetric, false, runtimeOnlyFlags);

        final DescriptionProvider addCustomMetric = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_CUSTOM_METRIC, rootResolver, CustomLoadMetricDefinition.ATTRIBUTES);
        resourceRegistration.registerOperationHandler(CommonAttributes.ADD_CUSTOM_METRIC, ModClusterAddCustomMetric.INSTANCE, addCustomMetric, false, runtimeOnlyFlags);

        final DescriptionProvider removeMetric = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_METRIC, rootResolver, LoadMetricDefinition.TYPE);
        resourceRegistration.registerOperationHandler(CommonAttributes.REMOVE_METRIC, ModClusterRemoveMetric.INSTANCE, removeMetric, false, runtimeOnlyFlags);

        final DescriptionProvider removeCustomMetric = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_CUSTOM_METRIC, rootResolver, CustomLoadMetricDefinition.CLASS);
        resourceRegistration.registerOperationHandler(CommonAttributes.REMOVE_CUSTOM_METRIC, ModClusterRemoveCustomMetric.INSTANCE, removeCustomMetric, false, runtimeOnlyFlags);

        if (runtimeOnly) {
            registerRuntimeOperations(resourceRegistration);
        }
    }

    public void registerRuntimeOperations(ManagementResourceRegistration registration) {
        EnumSet<OperationEntry.Flag> runtimeOnlyFlags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        final DescriptionProvider listProxiesDescription = new DefaultOperationDescriptionProvider(CommonAttributes.LIST_PROXIES, rootResolver);
        registration.registerOperationHandler(CommonAttributes.LIST_PROXIES, ModClusterListProxies.INSTANCE, listProxiesDescription, false, runtimeOnlyFlags);

        final DescriptionProvider readProxiesInfoDescription = new DefaultOperationDescriptionProvider(CommonAttributes.READ_PROXIES_INFO, rootResolver);
        registration.registerOperationHandler(CommonAttributes.READ_PROXIES_INFO, ModClusterGetProxyInfo.INSTANCE, readProxiesInfoDescription, false, runtimeOnlyFlags);

        final DescriptionProvider readProxiesInfoConfiguration = new DefaultOperationDescriptionProvider(CommonAttributes.READ_PROXIES_CONFIGURATION, rootResolver);
        registration.registerOperationHandler(CommonAttributes.READ_PROXIES_CONFIGURATION, ModClusterGetProxyConfiguration.INSTANCE, readProxiesInfoConfiguration, false, runtimeOnlyFlags);

        // TODO: These seem to be modifying the state so don't add the runtimeOnly stuff for now
        final DescriptionProvider addProxy = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.ADD_PROXY, ModClusterAddProxy.INSTANCE, addProxy, false);

        final DescriptionProvider removeProxy = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.REMOVE_PROXY, ModClusterRemoveProxy.INSTANCE, removeProxy, false);

        // Node related operations
        final DescriptionProvider refresh = new DefaultOperationDescriptionProvider(CommonAttributes.REFRESH, rootResolver);
        registration.registerOperationHandler(CommonAttributes.REFRESH, ModClusterRefresh.INSTANCE, refresh, false, runtimeOnlyFlags);

        final DescriptionProvider reset = new DefaultOperationDescriptionProvider(CommonAttributes.RESET, rootResolver);
        registration.registerOperationHandler(CommonAttributes.RESET, ModClusterReset.INSTANCE, reset, false, runtimeOnlyFlags);

        // Node (all contexts) related operations
        final DescriptionProvider enable = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.ENABLE, ModClusterEnable.INSTANCE, enable, false);

        final DescriptionProvider disable = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.DISABLE, ModClusterDisable.INSTANCE, disable, false);

        final DescriptionProvider stop = new DefaultOperationDescriptionProvider(CommonAttributes.STOP, rootResolver);
        registration.registerOperationHandler(CommonAttributes.STOP, ModClusterStop.INSTANCE, stop, false);

        // Context related operations
        final DescriptionProvider enableContext = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.ENABLE_CONTEXT, ModClusterEnableContext.INSTANCE, enableContext, false);

        final DescriptionProvider disableContext = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.DISABLE_CONTEXT, ModClusterDisableContext.INSTANCE, disableContext, false);

        final DescriptionProvider stopContext = new DefaultOperationDescriptionProvider(CommonAttributes.STOP_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT, WAIT_TIME);
        registration.registerOperationHandler(CommonAttributes.STOP_CONTEXT, ModClusterStopContext.INSTANCE, stopContext, false);
    }
}
