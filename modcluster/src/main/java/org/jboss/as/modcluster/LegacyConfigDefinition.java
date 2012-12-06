/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class LegacyConfigDefinition extends SimpleResourceDefinition {
    static final LegacyConfigDefinition INSTANCE = new LegacyConfigDefinition();

    private static final SimpleAttributeDefinition[] ATTRIBUTES = ModClusterConfigResourceDefinition.ATTRIBUTES; //todo not sure if they are the same

    public static final SimpleAttributeDefinition HOST = SimpleAttributeDefinitionBuilder.create(ModClusterConfigResourceDefinition.HOST)
            .addFlag(AttributeAccess.Flag.ALIAS)
            .build();

    private LegacyConfigDefinition() {
        super(ModClusterExtension.LEGACY_CONFIGURATION_PATH,
                ModClusterExtension.getResourceDescriptionResolver(),
                new DelegateAddOperation(),
                new DelegateRemoveOperation());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (SimpleAttributeDefinition current : ATTRIBUTES) {
            SimpleAttributeDefinition alias = SimpleAttributeDefinitionBuilder.create(current)
                    .addFlag(AttributeAccess.Flag.ALIAS)
                    .build();
            resourceRegistration.registerReadWriteAttribute(alias, new ConfigAliasAttributeReadHandler(alias), new ConfigAliasAttributeWriteHandler(alias));
        }
    }

    private static class DelegateAddOperation extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            for (SimpleAttributeDefinition attr : ATTRIBUTES) {
                ModelNode addOp = new ModelNode();
                addOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                addOp.get(OP_ADDR).set(PathAddress.pathAddress(ModClusterExtension.SUBSYSTEM_PATH).toModelNode());
                addOp.get(NAME).set(attr.getName());
                addOp.get(VALUE).set(operation.get(attr.getName()));
                context.addStep(addOp, WriteAttributeHandler.INSTANCE, OperationContext.Stage.IMMEDIATE);
            }
            context.stepCompleted();
        }
    }

    private static class DelegateRemoveOperation extends AbstractRemoveStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            for (SimpleAttributeDefinition attr : ATTRIBUTES) {
                ModelNode addOp = new ModelNode();
                addOp.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
                addOp.get(OP_ADDR).set(PathAddress.pathAddress(ModClusterExtension.SUBSYSTEM_PATH).toModelNode());
                addOp.get(NAME).set(attr.getName());
                context.addStep(addOp, WriteAttributeHandler.INSTANCE, OperationContext.Stage.IMMEDIATE);
            }
        }
    }

    private static class ConfigAliasAttributeWriteHandler implements OperationStepHandler {
        private SimpleAttributeDefinition attribute;

        private ConfigAliasAttributeWriteHandler(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode addOp = new ModelNode();
            addOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            addOp.get(OP_ADDR).set(PathAddress.pathAddress(ModClusterExtension.SUBSYSTEM_PATH).toModelNode());
            addOp.get(NAME).set(attribute.getName());
            addOp.get(VALUE).set(operation.get(attribute.getName()));
            context.addStep(addOp, WriteAttributeHandler.INSTANCE, OperationContext.Stage.IMMEDIATE);
            context.stepCompleted();
        }
    }

    private static class ConfigAliasAttributeReadHandler implements OperationStepHandler {
        private SimpleAttributeDefinition attribute;

        private ConfigAliasAttributeReadHandler(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            //todo maybe needs delegation too
            ModelNode resource = context.readResourceFromRoot(PathAddress.pathAddress(ModClusterExtension.SUBSYSTEM_PATH)).getModel();
            context.getResult().set(resource.get(attribute.getName()));
            context.stepCompleted();
        }

    }
}
