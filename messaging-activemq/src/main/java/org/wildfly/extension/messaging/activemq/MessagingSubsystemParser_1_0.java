/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.ACTIVEMQ_ARTEMIS_MODULE_ID;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.ha.LiveOnlyDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.ReplicationSlaveDefinition;
import org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreColocatedDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreMasterDefinition;
import org.wildfly.extension.messaging.activemq.ha.SharedStoreSlaveDefinition;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;
import org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition;

/**
 * Parser and Marshaller for messaging-activemq's {@link #NAMESPACE}.
 *
 * <em>All resources and attributes must be listed explicitly and not through any collections.</em>
 * This ensures that if the resource definitions change in later version (e.g. a new attribute is added),
 * this will have no impact on parsing this specific version of the subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class MessagingSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static final String NAMESPACE = "urn:jboss:domain:messaging-activemq:1.0";

    protected static final MessagingSubsystemParser_1_0 INSTANCE = new MessagingSubsystemParser_1_0();

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MessagingSubsystemRootResourceDefinition.INSTANCE)
                .addChild(
                        builder(ServerDefinition.INSTANCE)
                                .addAttributes(
                                        // no attribute groups
                                        ServerDefinition.PERSISTENCE_ENABLED,
                                        ServerDefinition.PERSIST_ID_CACHE,
                                        ServerDefinition.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY,
                                        ServerDefinition.ID_CACHE_SIZE,
                                        ServerDefinition.PAGE_MAX_CONCURRENT_IO,
                                        ServerDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                        ServerDefinition.THREAD_POOL_MAX_SIZE,
                                        ServerDefinition.WILD_CARD_ROUTING_ENABLED,
                                        ServerDefinition.CONNECTION_TTL_OVERRIDE,
                                        ServerDefinition.ASYNC_CONNECTION_EXECUTION_ENABLED,
                                        // security
                                        ServerDefinition.SECURITY_DOMAIN,
                                        ServerDefinition.SECURITY_ENABLED,
                                        ServerDefinition.SECURITY_INVALIDATION_INTERVAL,
                                        ServerDefinition.OVERRIDE_IN_VM_SECURITY,
                                        // cluster
                                        ServerDefinition.CLUSTER_USER,
                                        ServerDefinition.CLUSTER_PASSWORD,
                                        // management
                                        ServerDefinition.MANAGEMENT_ADDRESS,
                                        ServerDefinition.MANAGEMENT_NOTIFICATION_ADDRESS,
                                        ServerDefinition.JMX_MANAGEMENT_ENABLED,
                                        ServerDefinition.JMX_DOMAIN,
                                        // journal
                                        ServerDefinition.JOURNAL_TYPE,
                                        ServerDefinition.JOURNAL_BUFFER_TIMEOUT,
                                        ServerDefinition.JOURNAL_BUFFER_SIZE,
                                        ServerDefinition.JOURNAL_SYNC_TRANSACTIONAL,
                                        ServerDefinition.JOURNAL_SYNC_NON_TRANSACTIONAL,
                                        ServerDefinition.LOG_JOURNAL_WRITE_RATE,
                                        ServerDefinition.JOURNAL_FILE_SIZE,
                                        ServerDefinition.JOURNAL_MIN_FILES,
                                        ServerDefinition.JOURNAL_POOL_FILES,
                                        ServerDefinition.JOURNAL_COMPACT_PERCENTAGE,
                                        ServerDefinition.JOURNAL_COMPACT_MIN_FILES,
                                        ServerDefinition.JOURNAL_MAX_IO,
                                        ServerDefinition.CREATE_BINDINGS_DIR,
                                        ServerDefinition.CREATE_JOURNAL_DIR,
                                        // statistics
                                        ServerDefinition.STATISTICS_ENABLED,
                                        ServerDefinition.MESSAGE_COUNTER_SAMPLE_PERIOD,
                                        ServerDefinition.MESSAGE_COUNTER_MAX_DAY_HISTORY,
                                        // transaction
                                        ServerDefinition.TRANSACTION_TIMEOUT,
                                        ServerDefinition.TRANSACTION_TIMEOUT_SCAN_PERIOD,
                                        // message expiry
                                        ServerDefinition.MESSAGE_EXPIRY_SCAN_PERIOD,
                                        ServerDefinition.MESSAGE_EXPIRY_THREAD_PRIORITY,
                                        // debug
                                        ServerDefinition.PERF_BLAST_PAGES,
                                        ServerDefinition.RUN_SYNC_SPEED_TEST,
                                        ServerDefinition.SERVER_DUMP_INTERVAL,
                                        ServerDefinition.MEMORY_MEASURE_INTERVAL,
                                        ServerDefinition.MEMORY_WARNING_THRESHOLD,
                                        CommonAttributes.INCOMING_INTERCEPTORS,
                                        CommonAttributes.OUTGOING_INTERCEPTORS)
                                .addChild(
                                        builder(LiveOnlyDefinition.INSTANCE)
                                                .addAttributes(
                                                        ScaleDownAttributes.SCALE_DOWN,
                                                        ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_GROUP_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP,
                                                        ScaleDownAttributes.SCALE_DOWN_CONNECTORS))
                                .addChild(
                                        builder(ReplicationMasterDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.CLUSTER_NAME,
                                                        HAAttributes.GROUP_NAME,
                                                        HAAttributes.CHECK_FOR_LIVE_SERVER,
                                                        HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT))
                                .addChild(
                                        builder(ReplicationSlaveDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.CLUSTER_NAME,
                                                        HAAttributes.GROUP_NAME,
                                                        HAAttributes.ALLOW_FAILBACK,
                                                        HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT,
                                                        HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE,
                                                        HAAttributes.RESTART_BACKUP,
                                                        ScaleDownAttributes.SCALE_DOWN,
                                                        ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_GROUP_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP,
                                                        ScaleDownAttributes.SCALE_DOWN_CONNECTORS))
                                .addChild(
                                        builder(ReplicationColocatedDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.REQUEST_BACKUP,
                                                        HAAttributes.BACKUP_REQUEST_RETRIES,
                                                        HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL,
                                                        HAAttributes.MAX_BACKUPS,
                                                        HAAttributes.BACKUP_PORT_OFFSET,
                                                        HAAttributes.EXCLUDED_CONNECTORS)
                                                .addChild(
                                                        builder(ReplicationMasterDefinition.CONFIGURATION_INSTANCE)
                                                                .addAttributes(
                                                                        HAAttributes.CLUSTER_NAME,
                                                                        HAAttributes.GROUP_NAME,
                                                                        HAAttributes.CHECK_FOR_LIVE_SERVER,
                                                                        HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT))
                                                .addChild(
                                                        builder(ReplicationSlaveDefinition.CONFIGURATION_INSTANCE)
                                                                .addAttributes(
                                                                        HAAttributes.CLUSTER_NAME,
                                                                        HAAttributes.GROUP_NAME,
                                                                        HAAttributes.ALLOW_FAILBACK,
                                                                        HAAttributes.INITIAL_REPLICATION_SYNC_TIMEOUT,
                                                                        HAAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE,
                                                                        HAAttributes.RESTART_BACKUP,
                                                                        ScaleDownAttributes.SCALE_DOWN,
                                                                        ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME,
                                                                        ScaleDownAttributes.SCALE_DOWN_GROUP_NAME,
                                                                        ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP,
                                                                        ScaleDownAttributes.SCALE_DOWN_CONNECTORS)))
                                .addChild(
                                        builder(SharedStoreMasterDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN))
                                .addChild(
                                        builder(SharedStoreSlaveDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.ALLOW_FAILBACK,
                                                        HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN,
                                                        HAAttributes.RESTART_BACKUP,
                                                        ScaleDownAttributes.SCALE_DOWN,
                                                        ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_GROUP_NAME,
                                                        ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP,
                                                        ScaleDownAttributes.SCALE_DOWN_CONNECTORS))
                                .addChild(
                                        builder(SharedStoreColocatedDefinition.INSTANCE)
                                                .addAttributes(
                                                        HAAttributes.REQUEST_BACKUP,
                                                        HAAttributes.BACKUP_REQUEST_RETRIES,
                                                        HAAttributes.BACKUP_REQUEST_RETRY_INTERVAL,
                                                        HAAttributes.MAX_BACKUPS,
                                                        HAAttributes.BACKUP_PORT_OFFSET)
                                                .addChild(
                                                        builder(SharedStoreMasterDefinition.CONFIGURATION_INSTANCE)
                                                                .addAttributes(
                                                                        HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN))
                                                .addChild(
                                                        builder(SharedStoreSlaveDefinition.CONFIGURATION_INSTANCE)
                                                                .addAttributes(
                                                                        HAAttributes.ALLOW_FAILBACK,
                                                                        HAAttributes.FAILOVER_ON_SERVER_SHUTDOWN,
                                                                        HAAttributes.RESTART_BACKUP,
                                                                        ScaleDownAttributes.SCALE_DOWN,
                                                                        ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME,
                                                                        ScaleDownAttributes.SCALE_DOWN_GROUP_NAME,
                                                                        ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP,
                                                                        ScaleDownAttributes.SCALE_DOWN_CONNECTORS)))
                                .addChild(
                                        builder(PathDefinition.BINDINGS_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.BINDINGS_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.JOURNAL_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.JOURNAL_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.LARGE_MESSAGES_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.LARGE_MESSAGES_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(PathDefinition.PAGING_INSTANCE)
                                                .addAttributes(
                                                        PathDefinition.PATHS.get(CommonAttributes.PAGING_DIRECTORY),
                                                        PathDefinition.RELATIVE_TO))
                                .addChild(
                                        builder(QueueDefinition.INSTANCE)
                                                .addAttributes(
                                                        QueueDefinition.ADDRESS,
                                                        CommonAttributes.DURABLE,
                                                        CommonAttributes.FILTER))
                                .addChild(
                                        builder(SecuritySettingDefinition.INSTANCE)
                                                .addChild(
                                                        builder(SecurityRoleDefinition.INSTANCE)
                                                                .addAttributes(
                                                                        SecurityRoleDefinition.SEND,
                                                                        SecurityRoleDefinition.CONSUME,
                                                                        SecurityRoleDefinition.CREATE_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.DELETE_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE,
                                                                        SecurityRoleDefinition.MANAGE)))
                                .addChild(
                                        builder(AddressSettingDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.DEAD_LETTER_ADDRESS,
                                                        CommonAttributes.EXPIRY_ADDRESS,
                                                        AddressSettingDefinition.EXPIRY_DELAY,
                                                        AddressSettingDefinition.REDELIVERY_DELAY,
                                                        AddressSettingDefinition.REDELIVERY_MULTIPLIER,
                                                        AddressSettingDefinition.MAX_DELIVERY_ATTEMPTS,
                                                        AddressSettingDefinition.MAX_REDELIVERY_DELAY,
                                                        AddressSettingDefinition.MAX_SIZE_BYTES,
                                                        AddressSettingDefinition.PAGE_SIZE_BYTES,
                                                        AddressSettingDefinition.PAGE_MAX_CACHE_SIZE,
                                                        AddressSettingDefinition.ADDRESS_FULL_MESSAGE_POLICY,
                                                        AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT,
                                                        AddressSettingDefinition.LAST_VALUE_QUEUE,
                                                        AddressSettingDefinition.REDISTRIBUTION_DELAY,
                                                        AddressSettingDefinition.SEND_TO_DLA_ON_NO_ROUTE,
                                                        AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD,
                                                        AddressSettingDefinition.SLOW_CONSUMER_POLICY,
                                                        AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD,
                                                        AddressSettingDefinition.AUTO_CREATE_JMS_QUEUES,
                                                        AddressSettingDefinition.AUTO_DELETE_JMS_QUEUES))
                                .addChild(
                                        builder(HTTPConnectorDefinition.INSTANCE)
                                                .addAttributes(
                                                        HTTPConnectorDefinition.SOCKET_BINDING,
                                                        HTTPConnectorDefinition.ENDPOINT,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(RemoteTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        RemoteTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(InVMTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        InVMTransportDefinition.SERVER_ID,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(GenericTransportDefinition.CONNECTOR_INSTANCE)
                                                .addAttributes(
                                                        GenericTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(HTTPAcceptorDefinition.INSTANCE)
                                                .addAttributes(
                                                        HTTPAcceptorDefinition.HTTP_LISTENER,
                                                        HTTPAcceptorDefinition.UPGRADE_LEGACY,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(RemoteTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        RemoteTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(InVMTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        InVMTransportDefinition.SERVER_ID,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(GenericTransportDefinition.ACCEPTOR_INSTANCE)
                                                .addAttributes(
                                                        GenericTransportDefinition.SOCKET_BINDING,
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS))
                                .addChild(
                                        builder(BroadcastGroupDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.JGROUPS_STACK,
                                                        CommonAttributes.JGROUPS_CHANNEL,
                                                        CommonAttributes.SOCKET_BINDING,
                                                        BroadcastGroupDefinition.BROADCAST_PERIOD,
                                                        BroadcastGroupDefinition.CONNECTOR_REFS))
                                .addChild(
                                        builder(DiscoveryGroupDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.JGROUPS_STACK,
                                                        CommonAttributes.JGROUPS_CHANNEL,
                                                        CommonAttributes.SOCKET_BINDING,
                                                        DiscoveryGroupDefinition.REFRESH_TIMEOUT,
                                                        DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT))
                                .addChild(
                                        builder(ClusterConnectionDefinition.INSTANCE)
                                                .addAttributes(
                                                        ClusterConnectionDefinition.ADDRESS,
                                                        ClusterConnectionDefinition.CONNECTOR_NAME,
                                                        ClusterConnectionDefinition.CHECK_PERIOD,
                                                        ClusterConnectionDefinition.CONNECTION_TTL,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CALL_TIMEOUT,
                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                        ClusterConnectionDefinition.RETRY_INTERVAL,
                                                        ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER,
                                                        ClusterConnectionDefinition.MAX_RETRY_INTERVAL,
                                                        ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS,
                                                        ClusterConnectionDefinition.RECONNECT_ATTEMPTS,
                                                        ClusterConnectionDefinition.USE_DUPLICATE_DETECTION,
                                                        ClusterConnectionDefinition.MESSAGE_LOAD_BALANCING_TYPE,
                                                        ClusterConnectionDefinition.MAX_HOPS,
                                                        CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
                                                        ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS,
                                                        ClusterConnectionDefinition.NOTIFICATION_INTERVAL,
                                                        ClusterConnectionDefinition.CONNECTOR_REFS,
                                                        ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY,
                                                        ClusterConnectionDefinition.DISCOVERY_GROUP_NAME))
                                .addChild(
                                        builder(GroupingHandlerDefinition.INSTANCE)
                                                .addAttributes(
                                                        GroupingHandlerDefinition.TYPE,
                                                        GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS,
                                                        GroupingHandlerDefinition.TIMEOUT,
                                                        GroupingHandlerDefinition.GROUP_TIMEOUT,
                                                        GroupingHandlerDefinition.REAPER_PERIOD))
                                .addChild(
                                        builder(DivertDefinition.INSTANCE)
                                                .addAttributes(
                                                        DivertDefinition.ROUTING_NAME,
                                                        DivertDefinition.ADDRESS,
                                                        DivertDefinition.FORWARDING_ADDRESS,
                                                        CommonAttributes.FILTER,
                                                        CommonAttributes.TRANSFORMER_CLASS_NAME,
                                                        DivertDefinition.EXCLUSIVE)
                                                // WFLY-6516 the transformer-class-name attribute has been replaced by the transformer-class attribute
                                                .setAdditionalOperationsGenerator(new ClassOperationGenerator(CommonAttributes.TRANSFORMER_CLASS, CommonAttributes.TRANSFORMER_CLASS_NAME)))
                                .addChild(
                                        builder(BridgeDefinition.INSTANCE)
                                                .addAttributes(
                                                        BridgeDefinition.QUEUE_NAME,
                                                        BridgeDefinition.FORWARDING_ADDRESS,
                                                        CommonAttributes.HA,
                                                        CommonAttributes.FILTER,
                                                        CommonAttributes.TRANSFORMER_CLASS_NAME,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CHECK_PERIOD,
                                                        CommonAttributes.CONNECTION_TTL,
                                                        CommonAttributes.RETRY_INTERVAL,
                                                        CommonAttributes.RETRY_INTERVAL_MULTIPLIER,
                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                        BridgeDefinition.INITIAL_CONNECT_ATTEMPTS,
                                                        BridgeDefinition.RECONNECT_ATTEMPTS,
                                                        BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE,
                                                        BridgeDefinition.USE_DUPLICATE_DETECTION,
                                                        CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
                                                        BridgeDefinition.USER,
                                                        BridgeDefinition.PASSWORD,
                                                        BridgeDefinition.CONNECTOR_REFS,
                                                        BridgeDefinition.DISCOVERY_GROUP_NAME)
                                                // WFLY-6519 the transformer-class-name attribute has been replaced by the transformer-class attribute
                                                .setAdditionalOperationsGenerator(new ClassOperationGenerator(CommonAttributes.TRANSFORMER_CLASS, CommonAttributes.TRANSFORMER_CLASS_NAME)))
                                .addChild(
                                        builder(ConnectorServiceDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.FACTORY_CLASS,
                                                        CommonAttributes.PARAMS)
                                                // WFLY-6269 the factory-class attribute has been replaced by the class attribute
                                                .setAdditionalOperationsGenerator(new ClassOperationGenerator(ConnectorServiceDefinition.CLASS, CommonAttributes.FACTORY_CLASS)))
                                .addChild(
                                        builder(JMSQueueDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.DESTINATION_ENTRIES,
                                                        CommonAttributes.SELECTOR,
                                                        CommonAttributes.DURABLE,
                                                        CommonAttributes.LEGACY_ENTRIES))
                                .addChild(
                                        builder(JMSTopicDefinition.INSTANCE)
                                                .addAttributes(
                                                        CommonAttributes.DESTINATION_ENTRIES,
                                                        CommonAttributes.LEGACY_ENTRIES))
                                .addChild(
                                        builder(ConnectionFactoryDefinition.INSTANCE)
                                                .addAttributes(
                                                        // common
                                                        ConnectionFactoryAttributes.Common.DISCOVERY_GROUP,
                                                        ConnectionFactoryAttributes.Common.CONNECTORS,
                                                        ConnectionFactoryAttributes.Common.ENTRIES,
                                                        CommonAttributes.HA,
                                                        ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD,
                                                        ConnectionFactoryAttributes.Common.CONNECTION_TTL,
                                                        CommonAttributes.CALL_TIMEOUT,
                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                        ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE,
                                                        ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE,
                                                        ConnectionFactoryAttributes.Common.PROTOCOL_MANAGER_FACTORY,
                                                        ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES,
                                                        ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CLIENT_ID,
                                                        ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE,
                                                        ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND,
                                                        ConnectionFactoryAttributes.Common.AUTO_GROUP,
                                                        ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE,
                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL,
                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER,
                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                        ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS,
                                                        ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION,
                                                        ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                                                        ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS,
                                                        ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                        ConnectionFactoryAttributes.Common.THREAD_POOL_MAX_SIZE,
                                                        ConnectionFactoryAttributes.Common.GROUP_ID,
                                                        // regular
                                                        ConnectionFactoryAttributes.Regular.FACTORY_TYPE))
                                .addChild(
                                        builder(LegacyConnectionFactoryDefinition.INSTANCE)
                                                .addAttributes(
                                                        LegacyConnectionFactoryDefinition.ENTRIES,
                                                        LegacyConnectionFactoryDefinition.DISCOVERY_GROUP,
                                                        LegacyConnectionFactoryDefinition.CONNECTORS,

                                                        LegacyConnectionFactoryDefinition.AUTO_GROUP,
                                                        LegacyConnectionFactoryDefinition.BLOCK_ON_ACKNOWLEDGE,
                                                        LegacyConnectionFactoryDefinition.BLOCK_ON_DURABLE_SEND,
                                                        LegacyConnectionFactoryDefinition.BLOCK_ON_NON_DURABLE_SEND,
                                                        LegacyConnectionFactoryDefinition.CACHE_LARGE_MESSAGE_CLIENT,
                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                        CommonAttributes.CALL_TIMEOUT,
                                                        LegacyConnectionFactoryDefinition.CLIENT_FAILURE_CHECK_PERIOD,
                                                        CommonAttributes.CLIENT_ID,
                                                        LegacyConnectionFactoryDefinition.COMPRESS_LARGE_MESSAGES,
                                                        LegacyConnectionFactoryDefinition.CONFIRMATION_WINDOW_SIZE,
                                                        LegacyConnectionFactoryDefinition.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                                                        LegacyConnectionFactoryDefinition.CONNECTION_TTL,
                                                        LegacyConnectionFactoryDefinition.CONSUMER_MAX_RATE,
                                                        LegacyConnectionFactoryDefinition.CONSUMER_WINDOW_SIZE,
                                                        LegacyConnectionFactoryDefinition.DUPS_OK_BATCH_SIZE,
                                                        LegacyConnectionFactoryDefinition.FACTORY_TYPE,
                                                        LegacyConnectionFactoryDefinition.FAILOVER_ON_INITIAL_CONNECTION,
                                                        LegacyConnectionFactoryDefinition.GROUP_ID,
                                                        LegacyConnectionFactoryDefinition.HA,
                                                        LegacyConnectionFactoryDefinition.INITIAL_CONNECT_ATTEMPTS,
                                                        LegacyConnectionFactoryDefinition.INITIAL_MESSAGE_PACKET_SIZE,
                                                        LegacyConnectionFactoryDefinition.MAX_RETRY_INTERVAL,
                                                        LegacyConnectionFactoryDefinition.MIN_LARGE_MESSAGE_SIZE,
                                                        LegacyConnectionFactoryDefinition.PRE_ACKNOWLEDGE,
                                                        LegacyConnectionFactoryDefinition.PRODUCER_MAX_RATE,
                                                        LegacyConnectionFactoryDefinition.PRODUCER_WINDOW_SIZE,
                                                        LegacyConnectionFactoryDefinition.RECONNECT_ATTEMPTS,
                                                        LegacyConnectionFactoryDefinition.RETRY_INTERVAL,
                                                        LegacyConnectionFactoryDefinition.RETRY_INTERVAL_MULTIPLIER,
                                                        LegacyConnectionFactoryDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                        LegacyConnectionFactoryDefinition.THREAD_POOL_MAX_SIZE,
                                                        LegacyConnectionFactoryDefinition.TRANSACTION_BATCH_SIZE,
                                                        LegacyConnectionFactoryDefinition.USE_GLOBAL_POOLS))
                                .addChild(
                                        builder(PooledConnectionFactoryDefinition.INSTANCE)
                                                .addAttributes(
                                                        // common
                                                        ConnectionFactoryAttributes.Common.DISCOVERY_GROUP,
                                                        ConnectionFactoryAttributes.Common.CONNECTORS,
                                                        ConnectionFactoryAttributes.Common.ENTRIES,
                                                        CommonAttributes.HA,
                                                        ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD,
                                                        ConnectionFactoryAttributes.Common.CONNECTION_TTL,
                                                        CommonAttributes.CALL_TIMEOUT,
                                                        CommonAttributes.CALL_FAILOVER_TIMEOUT,
                                                        ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE,
                                                        ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE,
                                                        ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE,
                                                        ConnectionFactoryAttributes.Common.PROTOCOL_MANAGER_FACTORY,
                                                        ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES,
                                                        ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT,
                                                        CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                                                        CommonAttributes.CLIENT_ID,
                                                        ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE,
                                                        ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND,
                                                        ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND,
                                                        ConnectionFactoryAttributes.Common.AUTO_GROUP,
                                                        ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE,
                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL,
                                                        ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER,
                                                        CommonAttributes.MAX_RETRY_INTERVAL,
                                                        ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS,
                                                        ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION,
                                                        ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                                                        ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS,
                                                        ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE,
                                                        ConnectionFactoryAttributes.Common.THREAD_POOL_MAX_SIZE,
                                                        ConnectionFactoryAttributes.Common.GROUP_ID,
                                                        // pooled
                                                        ConnectionFactoryAttributes.Pooled.USE_JNDI,
                                                        ConnectionFactoryAttributes.Pooled.JNDI_PARAMS,
                                                        ConnectionFactoryAttributes.Pooled.USE_LOCAL_TX,
                                                        ConnectionFactoryAttributes.Pooled.SETUP_ATTEMPTS,
                                                        ConnectionFactoryAttributes.Pooled.SETUP_INTERVAL,
                                                        ConnectionFactoryAttributes.Pooled.TRANSACTION,
                                                        ConnectionFactoryAttributes.Pooled.USER,
                                                        ConnectionFactoryAttributes.Pooled.PASSWORD,
                                                        ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE,
                                                        ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE,
                                                        ConnectionFactoryAttributes.Pooled.MANAGED_CONNECTION_POOL,
                                                        ConnectionFactoryAttributes.Pooled.ENLISTMENT_TRACE,
                                                        ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY,
                                                        ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                                                        ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS)))
                .addChild(
                        builder(JMSBridgeDefinition.INSTANCE)
                                .addAttributes(
                                        JMSBridgeDefinition.MODULE,
                                        JMSBridgeDefinition.QUALITY_OF_SERVICE,
                                        JMSBridgeDefinition.FAILURE_RETRY_INTERVAL,
                                        JMSBridgeDefinition.MAX_RETRIES,
                                        JMSBridgeDefinition.MAX_BATCH_SIZE,
                                        JMSBridgeDefinition.MAX_BATCH_TIME,
                                        CommonAttributes.SELECTOR,
                                        JMSBridgeDefinition.SUBSCRIPTION_NAME,
                                        CommonAttributes.CLIENT_ID,
                                        JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER,
                                        JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY,
                                        JMSBridgeDefinition.SOURCE_DESTINATION,
                                        JMSBridgeDefinition.SOURCE_USER,
                                        JMSBridgeDefinition.SOURCE_PASSWORD,
                                        JMSBridgeDefinition.TARGET_CONNECTION_FACTORY,
                                        JMSBridgeDefinition.TARGET_DESTINATION,
                                        JMSBridgeDefinition.TARGET_USER,
                                        JMSBridgeDefinition.TARGET_PASSWORD,
                                        JMSBridgeDefinition.SOURCE_CONTEXT,
                                        JMSBridgeDefinition.TARGET_CONTEXT))
                .build();
    }

    private MessagingSubsystemParser_1_0() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(MessagingSubsystemRootResourceDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());
        xmlDescription.persist(writer, model, NAMESPACE);
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }

    /**
     * Generator that removes a legacy attribute representing a class (with only the name of the class and no module)
     * and replace it by a new attribute where the class is represented by its name and the org.apache.activemq.artemis module.
     */
    private static final class ClassOperationGenerator implements PersistentResourceXMLDescription.AdditionalOperationsGenerator {

        private final AttributeDefinition newAttribute;
        private final AttributeDefinition legacyAttribute;

        private ClassOperationGenerator(AttributeDefinition newClassAttribute, AttributeDefinition legacyAttribute) {
            this.newAttribute = newClassAttribute;
            this.legacyAttribute = legacyAttribute;
        }

        @Override
        public void additionalOperations(PathAddress address, ModelNode addOperation, List<ModelNode> operations) {
            if (!addOperation.hasDefined(legacyAttribute.getName())) {
                return;
            }
            ModelNode className = addOperation.remove(legacyAttribute.getName());
            ModelNode classModel = new ModelNode();
            classModel.get(CommonAttributes.NAME).set(className);
            // hard-code the module definition to org.apache.activemq.artemis as the
            // class was necessarily loaded from this module by Artemis itself
            classModel.get(CommonAttributes.MODULE).set(ACTIVEMQ_ARTEMIS_MODULE_ID);
            addOperation.get(newAttribute.getName()).set(classModel);

        }
    }
}
