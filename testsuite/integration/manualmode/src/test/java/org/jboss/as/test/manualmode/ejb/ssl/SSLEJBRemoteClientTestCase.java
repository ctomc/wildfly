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

package org.jboss.as.test.manualmode.ejb.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.as.test.manualmode.ejb.ssl.beans.StatefulBean;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatefulBeanRemote;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBean;
import org.jboss.as.test.manualmode.ejb.ssl.beans.StatelessBeanRemote;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * Testing ssl connection of remote ejb client.
 *
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class SSLEJBRemoteClientTestCase {
    private static final Logger log = Logger.getLogger(SSLEJBRemoteClientTestCase.class);
    private static final String MODULE_NAME_STATELESS = "ssl-remote-ejb-client-test";
    private static final String MODULE_NAME_STATEFUL = "ssl-remote-ejb-client-test-stateful";
    private static boolean serverConfigDone = false;
    private static ContextSelector<EJBClientContext> previousClientContextSelector;

    @Inject
    protected static ServerController container;

    public static Archive<?> deployStateless() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_STATELESS + ".jar");
        jar.addClasses(StatelessBeanRemote.class, StatelessBean.class);
        return jar;
    }

    public static Archive<?> deployStateful() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_STATEFUL + ".jar");
        jar.addClasses(StatefulBeanRemote.class, StatefulBean.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("*** BEFORE CLASS ***");
            log.debug("*** javax.net.ssl.trustStore=" + System.getProperty("javax.net.ssl.trustStore"));
            log.debug("*** javax.net.ssl.trustStorePassword=" + System.getProperty("javax.net.ssl.trustStorePassword"));
            log.debug("*** javax.net.ssl.keyStore=" + System.getProperty("javax.net.ssl.keyStore"));
            log.debug("*** javax.net.ssl.keyStorePassword=" + System.getProperty("javax.net.ssl.keyStorePassword"));
        }
        // probably not required, we get these properties from maven
        /*System.setProperty("javax.net.ssl.trustStore", client_truststore_path);
        System.setProperty("javax.net.ssl.trustStorePassword", SSLRealmSetupTool.CLIENT_KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStore", client_keystore_path);
        System.setProperty("javax.net.ssl.keyStorePassword", SSLRealmSetupTool.CLIENT_KEYSTORE_PASSWORD);*/
        System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", "true");
    }

    private static ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        log.debug("*** reading EJBClientContextSelector properties");
        // setup the selector
        final String clientPropertiesFile = "org/jboss/as/test/manualmode/ejb/ssl/jboss-ejb-client.properties";
        final InputStream inputStream = SSLEJBRemoteClientTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        log.debug("*** creating EJBClientContextSelector");
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        log.debug("*** applying EJBClientContextSelector");
        return EJBClientContext.setSelector(selector);
    }


    @Before
    public void prepareServerOnce() throws Exception {
        if (!serverConfigDone) {
            // prepare server config and then restart
            log.debug("*** preparing server configuration");

            log.debug("*** starting server");
            container.startInAdminMode();
            log.debug("*** will configure server now");
            new SSLRealmSetupTool().setup(container.getClient());
            log.debug("*** restarting server");
            container.reload();
            if (log.isTraceEnabled()) {
                SSLRealmSetupTool.readSSLRealmConfig(container.getClient());
            }
            previousClientContextSelector = setupEJBClientContextSelector();
            serverConfigDone = true;
        } else {
            log.debug("*** Server already prepared, skipping config procedure");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (previousClientContextSelector != null) {
            EJBClientContext.setSelector(previousClientContextSelector);
        }
        new SSLRealmSetupTool().tearDown(container.getClient());
    }

    private Properties getEjbClientContextProperties() {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put("jboss.naming.client.ejb.context", true);
        return env;
    }

    @Test
    public void testStatelessBean() throws Exception {
        log.debug("**** deploying deployment with stateless beans");
        container.deploy(deployStateless(), MODULE_NAME_STATELESS + ".jar");
        log.debug("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.debug("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATELESS + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());

            log.debug("**** About to perform synchronous call on stateless bean");
            String response = bean.sayHello();
            log.debug("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of EJB was not successful", StatelessBeanRemote.ANSWER, response);


        } catch (Exception e) {
            log.error("code made a boo boo", e);
            throw e;
        } finally {
            ctx.close();
            container.undeploy(MODULE_NAME_STATELESS + ".jar");
        }
    }

    @Test
    public void testStatelessBeanAsync() throws Exception {
        log.debug("**** deploying deployment with stateless beans");
        container.deploy(deployStateless(), MODULE_NAME_STATELESS + ".jar");
        log.debug("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.debug("**** looking up StatelessBean through JNDI");
            StatelessBeanRemote bean = (StatelessBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATELESS + "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getCanonicalName());
            log.debug("**** About to perform asynchronous call on stateless bean");
            Future<String> futureResponse = bean.sayHelloAsync();
            String response = futureResponse.get();
            log.debug("**** The answer is: " + response);
            Assert.assertEquals("Remote asynchronous invocation of EJB was not successful", StatelessBeanRemote.ANSWER, response);
        } finally {
            ctx.close();
            container.undeploy(MODULE_NAME_STATELESS + ".jar");
        }
    }

    @Test
    public void testStatefulBean() throws Exception {
        log.debug("**** deploying deployment with stateful beans");
        container.deploy(deployStateful(), MODULE_NAME_STATEFUL + ".jar");
        log.debug("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.debug("**** looking up StatefulBean through JNDI");
            StatefulBeanRemote bean = (StatefulBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATEFUL + "/" + StatefulBean.class.getSimpleName() + "!" + StatefulBeanRemote.class.getCanonicalName() + "?stateful");
            log.debug("**** About to perform synchronous call on stateful bean");
            String response = bean.sayHello();
            log.debug("**** The answer is: " + response);
            Assert.assertEquals("Remote invocation of EJB was not successful", StatefulBeanRemote.ANSWER, response);
        } finally {
            ctx.close();
            container.undeploy(MODULE_NAME_STATEFUL + ".jar");
        }
    }

    @Test
    public void testStatefulBeanAsync() throws Exception {
        log.debug("**** deploying deployment with stateful beans");
        container.deploy(deployStateful(), MODULE_NAME_STATEFUL + ".jar");
        log.debug("**** creating InitialContext");
        InitialContext ctx = new InitialContext(getEjbClientContextProperties());
        try {
            log.debug("**** looking up StatefulBean through JNDI");
            StatefulBeanRemote bean = (StatefulBeanRemote)
                    ctx.lookup("ejb:/" + MODULE_NAME_STATEFUL + "/" + StatefulBean.class.getSimpleName() + "!" + StatefulBeanRemote.class.getCanonicalName() + "?stateful");
            log.debug("**** About to perform asynchronous call on stateful bean");
            Future<String> futureResponse = bean.sayHelloAsync();
            String response = futureResponse.get();
            log.debug("**** The answer is: " + response);
            Assert.assertEquals("Remote asynchronous invocation of EJB was not successful", StatefulBeanRemote.ANSWER, response);
        } finally {
            ctx.close();
            container.undeploy(MODULE_NAME_STATEFUL + ".jar");
        }
    }


}