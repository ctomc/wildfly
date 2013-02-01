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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Handles removing the ejb remote service
 *
 * @author Stuart Douglas
 */
public class EJB3RemoteConnectorRemove extends AbstractRemoveStepHandler {


    /**
     * In a certain (deprecated/old) model, the EJB3 subsystem allowed just one remote-connector being configured and that connector
     * wasn't expected to have any name. This flag is to identify such cases. If this is true then it represents the case where the
     * remote connector is unnamed.
     */
    private final boolean unnamedRemoteConnector;

    /**
     *
     * @param unnamedConnector In a certain (deprecated/old) model, the EJB3 subsystem allowed just one remote-connector being
     *                         configured and that connector wasn't expected to have any name. This flag is to identify such cases.
     *                         If this is true then it represents the case where the remote connector is unnamed.
     * @deprecated Use {@link #EJB3RemoteConnectorRemove()} instead.
     */
    @Deprecated
    EJB3RemoteConnectorRemove(final boolean unnamedConnector) {
        this.unnamedRemoteConnector = unnamedConnector;
    }

    /**
     * This constructor is meant for the new model which expects the remote connector(s) to be named
     */

    EJB3RemoteConnectorRemove() {
        this.unnamedRemoteConnector = false;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
            removeRuntimeService(context, operation);
        } else {
            context.reloadRequired();
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        if (context.isResourceServiceRestartAllowed()) {
           this.getRemoteConnectorAddHandler().installRuntimeServices(context, operation, model, null);
        } else {
            context.revertReloadRequired();
        }
    }

    void removeRuntimeService(OperationContext context, ModelNode operation) {
        final PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        final ServiceName serviceName = this.getRemoteConnectorAddHandler().getEJB3RemoteConnectorServiceName(pathAddress);
        context.removeService(serviceName);
    }

    /**
     * Returns an appropriate {@link EJB3RemoteConnectorAdd} instance
     * @return
     */
    EJB3RemoteConnectorAdd getRemoteConnectorAddHandler() {
        // if this corresponds to an old model which allowed unnamed connectors, then return a EJB3RemoteConnectorAdd which supports that
        // notion
        if (this.unnamedRemoteConnector) {
            return new EJB3RemoteConnectorAdd(true);
        }
        return new EJB3RemoteConnectorAdd();
    }
}
