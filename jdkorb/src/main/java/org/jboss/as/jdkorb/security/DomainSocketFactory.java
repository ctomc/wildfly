package org.jboss.as.jdkorb.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.as.jdkorb.JdkORBLogger;
import org.jboss.as.jdkorb.JdkORBMessages;
import org.jboss.as.jdkorb.JdkORBSubsystemConstants;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;

import com.sun.corba.se.impl.orbutil.ORBConstants;
import com.sun.corba.se.pept.transport.Acceptor;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.ORBSocketFactory;

public class DomainSocketFactory implements ORBSocketFactory {

    // FIXME Security Domain should be obtained from orb configuration
    private static String securityDomain = null;

    public static void setSecurityDomain(final String securityDomain) {
        DomainSocketFactory.securityDomain = securityDomain;
    }

    private ORB orb;

    private SSLContext sslContext = null;

    private JSSESecurityDomain jsseSecurityDomain = null;

    private boolean request_mutual_auth = false;

    private boolean require_mutual_auth = false;

    @Override
    public void setORB(ORB orb) {
        this.orb = orb;

        try {
            InitialContext context = new InitialContext();
            jsseSecurityDomain = (JSSESecurityDomain) context.lookup(SecurityConstants.JAAS_CONTEXT_ROOT + securityDomain
                    + "/jsse");
            JdkORBLogger.ROOT_LOGGER.debugJSSEDomainRetrieval(securityDomain);
        } catch (NamingException ne) {
            JdkORBLogger.ROOT_LOGGER.failedToObtainJSSEDomain(securityDomain);
        }
        if (jsseSecurityDomain == null)
            throw new RuntimeException(JdkORBMessages.MESSAGES.failedToLookupJSSEDomain());
    }

    @Override
    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        ServerSocketChannel serverSocketChannel = null;
        ServerSocket serverSocket = null;

        if (type.equals(JdkORBSubsystemConstants.SSL_SOCKET_TYPE)) {
            serverSocket = createSSLServerSocket(inetSocketAddress.getPort(), 1000,
                    InetAddress.getByName(inetSocketAddress.getHostName()));
        } else if (orb.getORBData().acceptorSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
        } else {
            serverSocket = new ServerSocket();
        }
        if (!type.equals(JdkORBSubsystemConstants.SSL_SOCKET_TYPE)) {
            serverSocket.bind(inetSocketAddress);
        }
        return serverSocket;
    }

    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        SocketChannel socketChannel = null;
        Socket socket = null;

        if (type.contains(JdkORBSubsystemConstants.SSL_SOCKET_TYPE)) {
            socket = createSSLSocket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } else if (orb.getORBData().connectionSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            socketChannel = SocketChannel.open(inetSocketAddress);
            socket = socketChannel.socket();
        } else {
            socket = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        }

        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);
        return socket;
    }

    public void setAcceptedSocketOptions(Acceptor acceptor, ServerSocket serverSocket, Socket socket) throws SocketException {
        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);
    }

    public Socket createSSLSocket(String host, int port) throws IOException {
        this.initSSLContext();
        InetAddress address = InetAddress.getByName(host);

        SSLSocketFactory socketFactory = this.sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(address, port);
        if (this.jsseSecurityDomain.getProtocols() != null)
            socket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        if (this.jsseSecurityDomain.getCipherSuites() != null)
            socket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());
        socket.setNeedClientAuth(this.jsseSecurityDomain.isClientAuth());
        return socket;
    }

    public ServerSocket createSSLServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        this.initSSLContext();
        SSLServerSocketFactory serverSocketFactory = this.sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port, backlog, inetAddress);
        if (this.jsseSecurityDomain.getProtocols() != null)
            serverSocket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        if (this.jsseSecurityDomain.getCipherSuites() != null)
            serverSocket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());

        if (this.jsseSecurityDomain.isClientAuth() || this.require_mutual_auth)
            serverSocket.setNeedClientAuth(true);
        else
            serverSocket.setWantClientAuth(this.request_mutual_auth);

        return serverSocket;
    }

    private void initSSLContext() throws IOException {
        if (this.sslContext != null)
            return;
        this.sslContext = Util.forDomain(this.jsseSecurityDomain);
    }
}

// End of file.
