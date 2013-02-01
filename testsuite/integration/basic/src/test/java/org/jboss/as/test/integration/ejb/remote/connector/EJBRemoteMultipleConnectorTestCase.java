/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.connector;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.remoting.RemotingExtension;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.Properties;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Tests that the EJB3 subsystem can be configured for multiple remoting connectors
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(EJBRemoteMultipleConnectorTestCase.EJBRemoteConnectorSetup.class)
public class EJBRemoteMultipleConnectorTestCase {

    private static final Logger logger = Logger.getLogger(EJBRemoteMultipleConnectorTestCase.class);

    private static final String MODULE_NAME = "ejb-multiple-remote-connector-test";
    private static final String TEST_EJB_CHANNEL_NAME = "test-ejb-channel";
    private static final String USER_ONE = "user1";
    private static final String USER_ONE_PASSWORD = "password1";
    private static final String UNSECURED_REMOTE_CONNECTOR_PORT = "4448";

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(SecurityTestRemoteView.class.getPackage());
        return jar;
    }

    /**
     * Creates an EJB client context backed a receiver which connects to a remoting connector which *isn't* configured for security.
     * The EJB client context is then used to invoke on a secure and unsecured EJB and test the outcome. The goal of this test is to
     * make sure that we can connect to a specific EJB remoting connector that's been setup on the server
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testUnSecureRemoteConnector() throws Exception {
        final EJBClientContext ejbClientContext = this.createEJBClientContextForUnSecureRemoteConnector();
        final ContextSelector<EJBClientContext> previousContextSelector = EJBClientContext.setConstantContext(ejbClientContext);
        try {
            final Hashtable props = new Hashtable();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context jndiCtx = new InitialContext(props);
            // call unsecured bean
            final SecurityTestRemoteView unsecureBean = (SecurityTestRemoteView) jndiCtx.lookup("ejb:/" + MODULE_NAME + "//" + UnSecureBean.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
            final String principal = unsecureBean.getPrincipalName();
            logger.info("Client received principal " + principal + " from unsecured bean");
            Assert.assertEquals("Unexpected caller principal on unsecured bean", "anonymous", principal);
            // call secured bean
            final SecurityTestRemoteView securedBean = (SecurityTestRemoteView) jndiCtx.lookup("ejb:/" + MODULE_NAME + "//" + SecuredBean.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
            try {
                final String securedBeanPrincipal = securedBean.getPrincipalName();
                Assert.fail("Call on secured bean was expected to fail since the invocation is expected to use a unsecured remoting connector");
            } catch (EJBAccessException eae) {
                logger.info("Received the expected security exception", eae);
            }

        } finally {
            EJBClientContext.setSelector(previousContextSelector);
        }
    }

    /**
     * Creates an EJB client context backed a receiver which connects to a remoting connector which *is* configured for security.
     * The EJB client context is then used to invoke on a secure and unsecured EJB and test the outcome. The goal of this test is to
     * make sure that we can connect to a specific EJB remoting connector that's been setup on the server
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testSecureRemoteConnector() throws Exception {
        final EJBClientContext ejbClientContext = this.createEJBClientContextForDefaultRemoteConnector();
        final ContextSelector<EJBClientContext> previousContextSelector = EJBClientContext.setConstantContext(ejbClientContext);
        try {
            final Hashtable props = new Hashtable();
            props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            final Context jndiCtx = new InitialContext(props);
            // call unsecured bean
            final SecurityTestRemoteView unsecureBean = (SecurityTestRemoteView) jndiCtx.lookup("ejb:/" + MODULE_NAME + "//" + UnSecureBean.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
            final String principal = unsecureBean.getPrincipalName();
            logger.info("Client received principal " + principal + " from unsecured bean");
            Assert.assertEquals("Unexpected caller principal on unsecured bean", "anonymous", principal);
            // call secured bean
            final SecurityTestRemoteView securedBean = (SecurityTestRemoteView) jndiCtx.lookup("ejb:/" + MODULE_NAME + "//" + SecuredBean.class.getSimpleName() + "!" + SecurityTestRemoteView.class.getName());
            final String securedBeanPrincipal = securedBean.getPrincipalName();
            logger.info("Client received principal " + securedBeanPrincipal + " from secured bean");
            Assert.assertEquals("Unexpected caller principal on unsecured bean", USER_ONE, securedBeanPrincipal);
        } finally {
            EJBClientContext.setSelector(previousContextSelector);
        }

    }

    private EJBClientContext createEJBClientContextForDefaultRemoteConnector() {
        Properties props = new Properties();
        final String node = System.getProperty("node0", "127.0.0.1");
        props.put("endpoint.name", "ejb-multiple-remote-connector-test-client-endpoint");
        props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");

        props.put("remote.connections", "main");
        props.put("remote.connection.main.host", node);
        props.put("remote.connection.main.port", "4447");
        props.put("remote.connection.main.username", USER_ONE);
        props.put("remote.connection.main.password", USER_ONE_PASSWORD);
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");

        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(props);
        final ConfigBasedEJBClientContextSelector configBasedEJBClientContextSelector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        return configBasedEJBClientContextSelector.getCurrent();
    }

    private EJBClientContext createEJBClientContextForUnSecureRemoteConnector() {
        Properties props = new Properties();
        final String node = System.getProperty("node0", "127.0.0.1");
        props.put("endpoint.name", "ejb-multiple-remote-connector-test-client-endpoint2");
        props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");

        props.put("remote.connections", "main");
        props.put("remote.connection.main.host", node);
        props.put("remote.connection.main.port", UNSECURED_REMOTE_CONNECTOR_PORT);
        props.put("remote.connection.main.ejb-channel-name", TEST_EJB_CHANNEL_NAME);
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");


        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(props);
        final ConfigBasedEJBClientContextSelector configBasedEJBClientContextSelector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        return configBasedEJBClientContextSelector.getCurrent();
    }


    static class EJBRemoteConnectorSetup implements ServerSetupTask {

        private static final String unsecuredRemotingConnectorName = "unsecured-remoting-connector";
        private static final String ejbRemotingConnectorName = "ejb-multiple-remote-connector-test-connector";
        private static final String socketBindingName = "ejb-multiple-remote-connector-test-socket-binding";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // create a socket binding
            final ModelNode addSocketBindingOp = new ModelNode();
            addSocketBindingOp.get(OP).set(ADD);
            PathAddress socketbindingAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SOCKET_BINDING_GROUP, "standard-sockets"),
                    PathElement.pathElement(ModelDescriptionConstants.SOCKET_BINDING, socketBindingName));
            addSocketBindingOp.get(PORT).set(UNSECURED_REMOTE_CONNECTOR_PORT);
            addSocketBindingOp.get(OP_ADDR).set(socketbindingAddress.toModelNode());
            ManagementOperations.executeOperation(managementClient.getControllerClient(), addSocketBindingOp);
            logger.info("Executed op: " + addSocketBindingOp);

            // create a remoting connector in the remoting subsystem and leave that connector unsecured (i.e. no security-realm)
            final ModelNode addRemotingConnectorOp = new ModelNode();
            // /subsystem=remoting/connector=<name>:add(socket-binding=remoting)
            final PathAddress addRemotingConnectorAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                    PathElement.pathElement("connector", unsecuredRemotingConnectorName));
            addRemotingConnectorOp.get(ModelDescriptionConstants.OP_ADDR).set(addRemotingConnectorAddress.toModelNode());
            addRemotingConnectorOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            addRemotingConnectorOp.get(ModelDescriptionConstants.SOCKET_BINDING).set(socketBindingName);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), addRemotingConnectorOp);
            logger.info("Executed op: " + addRemotingConnectorOp);

            // now create a EJB remoting connector in EJB3 subsystem, which refers to the unsecured remoting connector that we just created
            final ModelNode addEJBRemoteConnectorOp = new ModelNode();
            // /subsystem=ejb3/service=connector/remote=foo-connector:add(ejb-channel-name=test-ejb-channel, thread-pool-name=default, connector-ref=unsecure-remoting-connector)
            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    PathElement.pathElement(EJB3SubsystemModel.SERVICE, EJB3SubsystemModel.CONNECTORS),
                    PathElement.pathElement(EJB3SubsystemModel.REMOTE, ejbRemotingConnectorName));
            addEJBRemoteConnectorOp.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
            addEJBRemoteConnectorOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            addEJBRemoteConnectorOp.get(EJB3SubsystemModel.EJB_CHANNEL_NAME).set(TEST_EJB_CHANNEL_NAME);
            addEJBRemoteConnectorOp.get(EJB3SubsystemModel.THREAD_POOL_NAME).set("default");
            addEJBRemoteConnectorOp.get(EJB3SubsystemModel.CONNECTOR_REF).set(unsecuredRemotingConnectorName);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), addEJBRemoteConnectorOp);
            logger.info("Executed op: " + addEJBRemoteConnectorOp);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // remove the EJB remoting connector
            final ModelNode removeEJBRemotingConnectorOp = new ModelNode();
            removeEJBRemotingConnectorOp.get(OP).set(REMOVE);
            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    PathElement.pathElement(EJB3SubsystemModel.SERVICE, EJB3SubsystemModel.CONNECTORS),
                    PathElement.pathElement(EJB3SubsystemModel.REMOTE, ejbRemotingConnectorName));
            removeEJBRemotingConnectorOp.get(OP_ADDR).set(address.toModelNode());
            ManagementOperations.executeOperation(managementClient.getControllerClient(), removeEJBRemotingConnectorOp);
            logger.info("Executed op: " + removeEJBRemotingConnectorOp);

        }
    }

}
