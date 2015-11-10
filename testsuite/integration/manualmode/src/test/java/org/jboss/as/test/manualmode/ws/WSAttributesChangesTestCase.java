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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
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
 * Some tests on changes to the model that are applied immediately to the runtime
 * when there's no WS deployment on the server.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class WSAttributesChangesTestCase {

    private static final String DEP_1 = "jaxws-manual-pojo-1";
    private static final String DEP_2 = "jaxws-manual-pojo-2";

    @Inject
    protected ServerController container;

    protected ManagementClient managementClient;

    public static WebArchive deployment1() {
        return ShrinkWrap.create(WebArchive.class, DEP_1 + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
    }

    public static WebArchive deployment2() {
        return ShrinkWrap.create(WebArchive.class, DEP_2 + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
    }

    @Before
    public void startContainer() throws Exception {
        container.start();
        managementClient = container.getClient();
    }

    @Test
    public void testWsdlHostChanges() throws Exception {
        performWsdlHostAttributeTest(false);
        performWsdlHostAttributeTest(true);
    }

    private void performWsdlHostAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(container.isStarted());
        String initialWsdlHost = null;
        try {
            initialWsdlHost = getAttribute("wsdl-host", managementClient.getControllerClient());

            final String hostnameA = "foo-host-a";

            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-host");
            op.get(VALUE).set(hostnameA);
            applyUpdate(managementClient.getControllerClient(), op, false); //update successful, no need to reload

            //now we deploy an endpoint...
            container.deploy(deployment1(), DEP_1 + ".war");

            //verify the updated wsdl host is used...
            URL wsdlURL = new URL(container.getClient().getWebUri().toURL(), '/' + DEP_1 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, hostnameA);

            if (checkUpdateWithDeployedEndpoint) {
                final String hostnameB = "foo-host-b";

                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-host");
                opB.get(VALUE).set(hostnameB);
                applyUpdate(managementClient.getControllerClient(), opB, true); //update again, but we'll need to reload, as there's an active deployment

                //check the wsdl host is still the one we updated to before
                checkWsdl(wsdlURL, hostnameA);

                //and check that still applies even if we undeploy and redeploy the endpoint
                container.undeploy(DEP_1 + ".war");
                container.deploy(deployment1(), DEP_1 + ".war");
                checkWsdl(wsdlURL, hostnameA);
            }
        } finally {
            try {
                container.undeploy(DEP_1 + ".war");
            } catch (Throwable t) {
                //ignore
            }
            if (initialWsdlHost != null) {
                ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                op.get(NAME).set("wsdl-host");
                op.get(VALUE).set(initialWsdlHost);
                op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                applyUpdate(managementClient.getControllerClient(), op, checkUpdateWithDeployedEndpoint);
            }

        }
    }

    @Test
    public void testWsdlPortChanges() throws Exception {
        performWsdlPortAttributeTest(false);
        performWsdlPortAttributeTest(true);
    }

    private void performWsdlPortAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(container.isStarted());

        ModelControllerClient client = managementClient.getControllerClient();
        try {
            final String portA = "55667";

            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-port");
            op.get(VALUE).set(portA);
            applyUpdate(client, op, false); //update successful, no need to reload

            //now we deploy an endpoint...
            container.deploy(deployment2(), DEP_2 + ".war");

            //verify the updated wsdl port is used...
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEP_2 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, portA);

            if (checkUpdateWithDeployedEndpoint) {
                final String portB = "55668";

                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-port");
                opB.get(VALUE).set(portB);
                applyUpdate(client, opB, true); //update again, but we'll need to reload, as there's an active deployment

                //check the wsdl port is still the one we updated to before
                checkWsdl(wsdlURL, portA);

                //and check that still applies even if we undeploy and redeploy the endpoint
                container.undeploy(DEP_2 + ".war");
                container.deploy(deployment2(), DEP_2 + ".war");
                checkWsdl(wsdlURL, portA);
            }
        } finally {
            try {
                container.undeploy(DEP_2 + ".war");
            } catch (Throwable t) {
                //ignore
            }
            ModelNode op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-port");
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            applyUpdate(client, op, checkUpdateWithDeployedEndpoint);

        }
    }

    @Test
    public void testWsdlUriSchemeChanges() throws Exception {
        performWsdlUriSchemeAttributeTest(false);
        performWsdlUriSchemeAttributeTest(true);
    }

    private void performWsdlUriSchemeAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(container.isStarted());

        ModelControllerClient client = managementClient.getControllerClient();
        String initialWsdlUriScheme = null;
        try {
            //save initial wsdl-uri-schema value to restore later
            initialWsdlUriScheme = getAttribute("wsdl-uri-scheme", client, false);
            //set wsdl-uri-scheme value to https
            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-uri-scheme");
            op.get(VALUE).set("https");
            applyUpdate(client, op, false);
            container.deploy(deployment1(), DEP_1 + ".war");
            //check if it works for the deployed endpoint url
            checkWSDLUriScheme(client, DEP_1 + ".war", "https");
            container.undeploy(DEP_1 + ".war");

            //set wsdl-uri-scheme value to http
            ModelNode op2 = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op2.get(NAME).set("wsdl-uri-scheme");
            op2.get(VALUE).set("http");
            applyUpdate(client, op2, false);
            container.deploy(deployment1(), DEP_1 + ".war");
            //check if the uri scheme of soap address is modified to http
            checkWSDLUriScheme(client, DEP_1 + ".war", "http");
            if (checkUpdateWithDeployedEndpoint) {
                //set wsdl-uri-schema value to http
                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-uri-scheme");
                opB.get(VALUE).set("https");
                applyUpdate(client, opB, true);
                //check this doesn't apply to endpointed which are deployed before this change
                checkWSDLUriScheme(client, DEP_1 + ".war", "http");
                container.undeploy(DEP_1 + ".war");
                container.deploy(deployment1(), DEP_1 + ".war");
                //check this will take effect to redeployed endpoint
                checkWSDLUriScheme(client, DEP_1 + ".war", "http");
            }
        } finally {
            try {
                container.undeploy(DEP_1 + ".war");
            } catch (Throwable t) {
                //ignore
            }
            //restore the value of wsdl-uri-scheme attribute
            ModelNode op = null;
            if ("undefined".equals(initialWsdlUriScheme)) {
                op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
                op.get(NAME).set("wsdl-uri-scheme");
            } else {
                op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                op.get(NAME).set("wsdl-uri-scheme");
                op.get(VALUE).set(initialWsdlUriScheme);
            }
            applyUpdate(client, op, checkUpdateWithDeployedEndpoint);

        }
    }

    @Test
    public void testWsdlPathRewriteRuleChanges() throws Exception {
        performWsdlPathRewriteRuleAttributeTest(false);
        performWsdlPathRewriteRuleAttributeTest(true);
    }


    private void performWsdlPathRewriteRuleAttributeTest(boolean checkUpdateWithDeployedEndpoint) throws Exception {
        Assert.assertTrue(container.isStarted());

        ModelControllerClient client = managementClient.getControllerClient();

        try {

            final String expectedContext = "xx/jaxws-manual-pojo-1";
            final String sedCmdA = "s/jaxws-manual-pojo-1/xx\\/jaxws-manual-pojo-1/g";

            ModelNode op = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-path-rewrite-rule");
            op.get(VALUE).set(sedCmdA);
            applyUpdate(client, op, false); //update successful, no need to reload

            //now we deploy an endpoint...
            container.deploy(deployment1(), DEP_1 + ".war");

            //verify the updated wsdl host is used...
            URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEP_1 + "/POJOService?wsdl");
            checkWsdl(wsdlURL, expectedContext);

            if (checkUpdateWithDeployedEndpoint) {
                //final String hostnameB = "foo-host-b";
                final String sedCmdB = "s/jaxws-manual-pojo-1/FOO\\/jaxws-manual-pojo-1/g";

                ModelNode opB = createOpNode("subsystem=webservices/", WRITE_ATTRIBUTE_OPERATION);
                opB.get(NAME).set("wsdl-path-rewrite-rule");
                opB.get(VALUE).set(sedCmdB);
                applyUpdate(client, opB, true); //update again, but we'll need to reload, as there's an active deployment

                //check the wsdl host is still the one we updated to before
                checkWsdl(wsdlURL, expectedContext);

                //and check that still applies even if we undeploy and redeploy the endpoint
                container.undeploy(DEP_1 + ".war");
                container.deploy(deployment1(), DEP_1 + ".war");
                checkWsdl(wsdlURL, expectedContext);
            }
        } finally {
            try {
                container.undeploy(DEP_1 + ".war");
            } catch (Throwable t) {
                //ignore
            }
            ModelNode op = createOpNode("subsystem=webservices/", UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("wsdl-path-rewrite-rule");
            applyUpdate(client, op, checkUpdateWithDeployedEndpoint);

        }
    }


    @After
    public void stopContainer() throws IOException {
        if (container.isStarted()) {
            container.stop();
        }
        if (managementClient != null) {
            managementClient.close();
        }
    }

    private String getAttribute(final String attribute, final ModelControllerClient client) throws Exception {
        return getAttribute(attribute, client, true);
    }

    private String getAttribute(final String attribute, final ModelControllerClient client, final boolean checkDefined) throws Exception {
        ModelNode op = createOpNode("subsystem=webservices/", READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attribute);
        final ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (checkDefined) {
                Assert.assertTrue(result.hasDefined(RESULT));
            }
            return result.get(RESULT).asString();
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
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

    private static ModelNode applyUpdate(final ModelControllerClient client, final ModelNode update, final boolean expectReloadRequired) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (expectReloadRequired) {
                Assert.assertTrue("Response headers should be defined", result.hasDefined(RESPONSE_HEADERS));
                ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
                Assert.assertTrue("Reload required expected", responseHeaders.hasDefined(OPERATION_REQUIRES_RELOAD));
                Assert.assertEquals("true", responseHeaders.get(OPERATION_REQUIRES_RELOAD).asString());
            } else {
                Assert.assertFalse("Response headers shouldn't be defined", result.hasDefined(RESPONSE_HEADERS));
            }
            return result;
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    private void checkWsdl(URL wsdlURL, String hostOrPort) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(200, connection.getResponseCode());
            connection.getInputStream();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("address location")) {
                    Assert.assertTrue(line.contains(hostOrPort));
                    return;
                }
            }
            fail("Could not check soap:address!");
        } finally {
            connection.disconnect();
        }
    }

    private void checkWSDLUriScheme(final ModelControllerClient managementClient, String deploymentName, String expectedScheme) throws Exception {
        final ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName);
        address.add(SUBSYSTEM, "webservices");
        address.add("endpoint", "*"); // get all endpoints

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(RECURSIVE).set(true);

        ModelNode result = managementClient.execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        for (final ModelNode endpointResult : result.get("result").asList()) {
            final ModelNode endpoint = endpointResult.get("result");
            final URL wsdlURL = new URL(endpoint.get("wsdl-url").asString());
            HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
            try {
                connection.connect();
                Assert.assertEquals(200, connection.getResponseCode());
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("address location")) {
                        if ("https".equals(expectedScheme)) {
                            Assert.assertTrue(line, line.contains("https"));
                            return;
                        } else {
                            Assert.assertTrue(line, line.contains("http") && !line.contains("https"));
                            return;
                        }
                    }
                }
                fail(" Could not check soap:address!");
            } finally {
                connection.disconnect();
            }
        }
    }
}
