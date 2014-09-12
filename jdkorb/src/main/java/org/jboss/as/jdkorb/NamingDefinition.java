/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jdkorb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class NamingDefinition extends PersistentResourceDefinition {

    private static final ModelNode DEFAULT_ENABLED_PROPERTY = new ModelNode().set("on");

    private static final ParameterValidator ON_OFF_VALIDATOR = new EnumValidator<TransactionsAllowedValues>(
            TransactionsAllowedValues.class, true, false, TransactionsAllowedValues.ON, TransactionsAllowedValues.OFF);

    static final SensitivityClassification JDKORB_SECURITY = new SensitivityClassification(JdkORBExtension.SUBSYSTEM_NAME,
            "jdkorb-security", false, false, true);

    protected static final SimpleAttributeDefinition ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(JdkORBSubsystemConstants.JDKORB_NAME_SERVICE_INIT_REF))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true).build();

    protected static final SimpleAttributeDefinition EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.NAMING_EXPORT_CORBALOC, ModelType.STRING, true).setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).setAllowExpression(true)
            .build();

    static final NamingDefinition INSTANCE = new NamingDefinition();

    private static final List<SimpleAttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
            ROOT_CONTEXT, EXPORT_CORBALOC));

    private NamingDefinition() {
        super(JdkORBExtension.PATH_NAMING, JdkORBExtension.getResourceDescriptionResolver(JdkORBSubsystemConstants.NAMING),
                new AbstractAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection)ATTRIBUTES;
    }
}
