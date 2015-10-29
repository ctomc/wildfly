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
package org.jboss.as.test.manualmode.cli;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;
import org.xnio.IoUtils;

@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class ReloadOpsTestCase extends AbstractCliTestBase {
    private static final Logger logger = Logger.getLogger(ReloadOpsTestCase.class);
    private static final long TIMEOUT = 10_000L;

    @Inject
    private static ServerController container;

    @BeforeClass
    public static void initServer() throws Exception {
        System.setProperty("server.config","standalone-full-ha.xml");
        container.start();
        initCLI();
    }

    @AfterClass
    public static void closeServer() throws Exception {
        closeCLI();
        container.stop();
    }

    @Test
    public void reloadServerTest() throws Exception {

        for (int i = 0; i < 100; i++) {

                CLI cli = CLI.newInstance();
                Thread.sleep(1000);
                logger.info("!!!!!!!!!!!!!!!!!!! Connect !!!!!!!!!!!!!!!!!!!!!!");
                cli.connect(TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), null, null);
                logger.info("!!!!!!!!!!!!!!!!!!! Reload !!!!!!!!!!!!!!!!!!!!!!");
                cli.cmd("reload");
                logger.info("!!!!!!!!!!!!!!!!!!! Disconnect !!!!!!!!!!!!!!!!!!!!!!");
                cli.disconnect();
                logger.info("!!!!!!!!!!!!!!!!!!! Done !!!!!!!!!!!!!!!!!!!!!!");

                waitForLiveServerToReload(15000);
            /*Thread.sleep(1000);
            reloadServer(15000);*/
        }
    }

    private void reloadServer(long timeout) throws Exception {
        executeReload();
        logger.info("!!!!!!!!!!!!!!!!!!! Reload !!!!!!!!!!!!!!!!!!!!!!");
        waitForLiveServerToReload(timeout);
        logger.info("!!!!!!!!!!!!!!!!!!! Done !!!!!!!!!!!!!!!!!!!!!!");
    }

    private static void executeReload() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("reload");
        } finally {
            ctx.terminateSession();
        }

    }

    private void waitForLiveServerToReload(long timeout) throws Exception {
        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < timeout) {
            ModelControllerClient liveClient = TestSuiteEnvironment.getModelControllerClient();
            try {
                logger.infof("Trying to execute op");
                ModelNode result = liveClient.execute(operation);
                logger.infof("server state %s", result.get(RESULT).asString());
                if ("running".equals(result.get(RESULT).asString())) {
                    return;
                }
            } catch (IOException e) {
                logger.infof(e, "error");
            } finally {
                IoUtils.safeClose(liveClient);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        fail("Live Server did not reload in the imparted time.");
    }

    protected void checkResponseHeadersForProcessState(CLIOpResult result) {
        assertNotNull("No response headers!", result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS));
        Map responseHeaders = (Map) result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS);
        Object processState = responseHeaders.get("process-state");
        assertNotNull("No process state in response-headers!", processState);
        assertTrue("Process state is of wrong type!", processState instanceof String);
        assertEquals("Wrong content of process-state header", "reload-required", (String) processState);

    }

    protected void assertNoProcessState(CLIOpResult result) {
        if (result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS) == null) {
            return;
        }
        Map responseHeaders = (Map) result.getFromResponse(ModelDescriptionConstants.RESPONSE_HEADERS);
        Object processState = responseHeaders.get("process-state");
        assertNull(processState);

    }
}