package org.jboss.as.test.manualmode.ws;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public class AbstractWSTest {
    protected ModelControllerClient client;

    public void deploy(final Archive<?> archive, final String runtimeName) throws ServerDeploymentHelper.ServerDeploymentException {
        final ServerDeploymentHelper helper = new ServerDeploymentHelper(client);
        helper.deploy(runtimeName, archive.as(ZipExporter.class).exportAsInputStream());
    }

    public void undeploy(final String runtimeName) throws ServerDeploymentHelper.ServerDeploymentException {
        final ServerDeploymentHelper helper = new ServerDeploymentHelper(client);
        helper.undeploy(runtimeName);
    }
}
