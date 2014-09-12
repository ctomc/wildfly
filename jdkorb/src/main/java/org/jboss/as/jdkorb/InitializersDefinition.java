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
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class InitializersDefinition extends PersistentResourceDefinition {
    private static final ModelNode DEFAULT_DISABLED_PROPERTY = new ModelNode().set("off");

    static final SensitivityClassification JDKORB_SECURITY =
            new SensitivityClassification(JdkORBExtension.SUBSYSTEM_NAME, "jdkorb-security", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition JDKORB_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(JDKORB_SECURITY);
    protected static final SimpleAttributeDefinition SECURITY = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_INIT_SECURITY, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(new EnumValidator<SecurityAllowedValues>(SecurityAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .addAccessConstraint(JDKORB_SECURITY_DEF)
            .build();

    protected static final SimpleAttributeDefinition TRANSACTIONS = new SimpleAttributeDefinitionBuilder(
            JdkORBSubsystemConstants.ORB_INIT_TRANSACTIONS, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(new EnumValidator<TransactionsAllowedValues>(TransactionsAllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .build();

    static final InitializersDefinition INSTANCE = new InitializersDefinition();

    private static final List<SimpleAttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
            SECURITY, TRANSACTIONS));

    private InitializersDefinition() {
        super(JdkORBExtension.PATH_ORB, JdkORBExtension.getResourceDescriptionResolver(JdkORBSubsystemConstants.ORB,
                JdkORBSubsystemConstants.ORB_INIT), new AbstractAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection)ATTRIBUTES;
    }
}
