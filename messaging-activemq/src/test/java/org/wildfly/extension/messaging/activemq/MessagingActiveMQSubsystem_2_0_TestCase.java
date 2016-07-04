/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingActiveMQSubsystem_2_0_TestCase extends AbstractSubsystemBaseTest {

    public MessagingActiveMQSubsystem_2_0_TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_2_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-messaging-activemq_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/messaging-activemq.xml"
        };
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("messaging.cluster.user.name", "myClusterUser");
        properties.put("messaging.cluster.user.password", "myClusterPassword");
        return properties;
    }

    /////////////////////////////////////////
    //  Tests for HA Policy Configuration  //
    /////////////////////////////////////////

    @Test
    public void testHAPolicyConfiguration() throws Exception {
        standardSubsystemTest("subsystem_2_0_ha-policy.xml", false);
    }

    ///////////////////////
    // Transformer tests //
    ///////////////////////


    @Ignore
    @Test
    public void testTransformersWFLY_10_0_0() throws Exception {
        testTransformers(ModelTestControllerVersion.WILDFLY_10_0_0_FINAL, MessagingExtension.VERSION_1_0_0, null);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer) throws Exception {
        testTransformers(controllerVersion, messagingVersion, fixer, null);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, ModelFixer legacyModelFixer) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_2_0.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(MessagingDependencies.MESSAGING_ACTIVEMQ_MAVEN_GA + ":" + controllerVersion.getMavenGavVersion())
                //.addMavenResourceURL(MessagingDependencies.getArtemisDependencies(controllerVersion))
                .configureReverseControllerCheck(createAdditionalInitialization(), fixer)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion, legacyModelFixer);
    }

}
