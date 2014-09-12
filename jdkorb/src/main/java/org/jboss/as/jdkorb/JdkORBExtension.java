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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * <p>
 * The JdkORB extension implementation.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class JdkORBExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jdkorb";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement PATH_ORB = PathElement.pathElement(JdkORBSubsystemConstants.ORB , "default");
    protected static final PathElement PATH_TCP = PathElement.pathElement(JdkORBSubsystemConstants.ORB_TCP,
            JdkORBSubsystemConstants.ORB_TCP);
    protected static final PathElement PATH_INITILIZERS = PathElement.pathElement("initializers", "default" );
    protected static final PathElement PATH_NAMING = PathElement.pathElement(JdkORBSubsystemConstants.NAMING,
            JdkORBSubsystemConstants.DEFAULT);
    protected static final PathElement PATH_SECURITY = PathElement.pathElement(JdkORBSubsystemConstants.SECURITY,
            JdkORBSubsystemConstants.DEFAULT);
    protected static final PathElement PATH_IOR_SETTINGS = PathElement.pathElement(JdkORBSubsystemConstants.IOR_SETTINGS,
            JdkORBSubsystemConstants.DEFAULT);
    protected static final PathElement PATH_CLIENT_TRANSPORT = PathElement.pathElement(
            JdkORBSubsystemConstants.CLIENT_TRANSPORT, JdkORBSubsystemConstants.DEFAULT);

    private static final String RESOURCE_NAME = JdkORBExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(JdkORBExtension.SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append(".").append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                JdkORBExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(JdkORBSubsystemResource.INSTANCE);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(JdkORBSubsystemParser.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME,Namespace.JdkORB_1_0.getUriString(), JdkORBSubsystemParser.INSTANCE);
    }
}
