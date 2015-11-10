/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.ws;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.inject.Inject;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * Some tests on changes to the model requiring reload
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class ReloadRequiringChangesTestCase {

    private static final String DEPLOYMENT_URI = "jaxws-manual-pojo";
    private static final String DEPLOYMENT = "jaxws-manual-pojo.war";

    @Inject
    protected ServerController containerController;

    protected ManagementClient client;


    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT).addClasses(
                EndpointIface.class, PojoEndpoint.class);
    }

    @Before
    public void startContainer() throws Exception {
        containerController.start();
        if (containerController.isStarted()) {
            containerController.deploy(deployment(), DEPLOYMENT);
        }
        client = containerController.getClient();
    }

    @Test
    public void testWSDLHostChangeRequiresReloadAndDoesNotAffectRuntime() throws Exception {
        Assert.assertTrue(containerController.isStarted());

        String initialWsdlHost = null;
        try {
            initialWsdlHost = getWsdlHost(client.getControllerClient());

            //change wsdl-host to "foo-host" and reload
            final String hostname = "foo-host";
            setWsdlHost(client.getControllerClient(), hostname);
            containerController.reload();
            client = containerController.getClient();

            //change wsdl-host to "bar-host" and verify deployment still uses "foo-host"
            setWsdlHost(client.getControllerClient(), "bar-host");
            URL wsdlURL = new URL(client.getWebUri().toURL(), '/' + DEPLOYMENT_URI + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostname);
        } finally {
            if (initialWsdlHost != null) {
                setWsdlHost(client.getControllerClient(), initialWsdlHost);
            }

        }
    }

    @Test
    public void testWSDLHostUndefineRequiresReloadAndDoesNotAffectRuntime() throws Exception {
        Assert.assertTrue(containerController.isStarted());

        String initialWsdlHost = null;
        try {
            initialWsdlHost = getWsdlHost(client.getControllerClient());

            //change wsdl-host to "my-host" and reload
            final String hostname = "my-host";
            setWsdlHost(client.getControllerClient(), hostname);
            containerController.reload();
            client = containerController.getClient(); //get new client after reload

            //undefine wsdl-host and verify deployment still uses "foo-host"
            setWsdlHost(client.getControllerClient(), null);
            URL wsdlURL = new URL(client.getWebUri().toURL(), '/' + DEPLOYMENT_URI + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostname);
        } finally {
            if (initialWsdlHost != null) {
                setWsdlHost(client.getControllerClient(), initialWsdlHost);
            }

        }
    }

    @After
    public void stopContainer() throws ServerDeploymentHelper.ServerDeploymentException {
        if (containerController.isStarted()) {
            containerController.undeploy(DEPLOYMENT);
        }
        if (containerController.isStarted()) {
            containerController.stop();
        }
    }

    private String getWsdlHost(final ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode("subsystem=webservices/", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set("wsdl-host");
        final ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.assertTrue(result.hasDefined(RESULT));
            return result.get(RESULT).asString();
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    private void setWsdlHost(final ModelControllerClient client, final String wsdlHost) throws Exception {
        ModelNode op;
        if (wsdlHost != null) {
            op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
            op.get(VALUE).set(wsdlHost);
        } else {
            op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
        }
        applyUpdate(client, op);
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();
        // set address
        ModelNode list = op.get(ADDRESS).setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static ModelNode applyUpdate(final ModelControllerClient client, final ModelNode update) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            Assert.assertTrue(result.hasDefined(RESPONSE_HEADERS));
            ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
            Assert.assertTrue(responseHeaders.hasDefined(OPERATION_REQUIRES_RELOAD));
            Assert.assertEquals("true", responseHeaders.get(OPERATION_REQUIRES_RELOAD).asString());
            return result;
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }


    private void checkWsdl(URL wsdlURL, String host) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(200, connection.getResponseCode());
            connection.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("address location")) {
                    Assert.assertTrue(line.contains(host));
                    return;
                }
            }
            fail("Could not check soap:address!");
        } finally {
            connection.disconnect();
        }
    }
}
