/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * Modifications © 2017-2018 Hashmap, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hashmapinc.server.transport.mqtt;

import com.hashmapinc.server.common.transport.SessionMsgProcessor;
import com.hashmapinc.server.common.transport.auth.DeviceAuthService;
import com.hashmapinc.server.common.transport.quota.QuotaService;
import com.hashmapinc.server.dao.asset.AssetService;
import com.hashmapinc.server.dao.attributes.AttributesService;
import com.hashmapinc.server.dao.device.DeviceService;
import com.hashmapinc.server.dao.event.EventService;
import com.hashmapinc.server.dao.mail.MailService;
import com.hashmapinc.server.dao.relation.RelationService;
import com.hashmapinc.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Service("MqttTransportService")
@ConditionalOnProperty(prefix = "mqtt", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class MqttTransportService {

    @Autowired(required = false)
    private ApplicationContext appContext;

    @Autowired(required = false)
    private SessionMsgProcessor processor;

    @Autowired(required = false)
    private DeviceService deviceService;

    @Autowired(required = false)
    private DeviceAuthService authService;

    @Autowired(required = false)
    private RelationService relationService;

    @Autowired(required = false)
    private EventService eventService;

    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Autowired(required = false)
    private QuotaService quotaService;

    @Autowired(required = false)
    private AttributesService attributesService;

    @Autowired(required = false)
    private AssetService assetService;

    @Autowired(required = false)
    private MailService mailService;

    @Value("${mqtt.bind_address}")
    private String host;
    @Value("${mqtt.bind_port}")
    private Integer port;
    @Value("${mqtt.adaptor}")
    private String adaptorName;

    @Value("${mqtt.netty.leak_detector_level}")
    private String leakDetectorLevel;
    @Value("${mqtt.netty.boss_group_thread_count}")
    private Integer bossGroupThreadCount;
    @Value("${mqtt.netty.worker_group_thread_count}")
    private Integer workerGroupThreadCount;


    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void init() throws InterruptedException {
        log.info("Setting resource leak detector level to {}", leakDetectorLevel);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetectorLevel.toUpperCase()));

        log.info("Starting MQTT transport...");
        log.info("Lookup MQTT transport adaptor {}", adaptorName);
        MqttTransportAdaptor adaptor = (MqttTransportAdaptor) appContext.getBean(adaptorName);

        log.info("Starting MQTT transport server");
        bossGroup = new NioEventLoopGroup(bossGroupThreadCount);
        workerGroup = new NioEventLoopGroup(workerGroupThreadCount);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MqttTransportServerInitializer(processor, deviceService, authService, relationService,
                        adaptor, sslHandlerProvider, quotaService,attributesService, assetService, eventService, mailService));

        serverChannel = b.bind(host, port).sync().channel();
        log.info("Mqtt transport started!");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping MQTT transport!");
        try {
            serverChannel.close().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        log.info("MQTT transport stopped!");
    }
}
