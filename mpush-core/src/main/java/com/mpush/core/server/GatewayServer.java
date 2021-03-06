/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   ohun@live.cn (夜色)
 */

package com.mpush.core.server;

import com.mpush.api.protocol.Command;
import com.mpush.api.service.Listener;
import com.mpush.common.MessageDispatcher;
import com.mpush.core.handler.GatewayPushHandler;
import com.mpush.netty.server.NettyServer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;

import java.util.concurrent.Executors;

import static com.mpush.tools.config.CC.mp.net.traffic_shaping.gateway_server.*;

/**
 * Created by ohun on 2015/12/30.
 *
 * @author ohun@live.cn
 */
public final class GatewayServer extends NettyServer {

    private ServerChannelHandler channelHandler;
    private ServerConnectionManager connectionManager;
    private GlobalChannelTrafficShapingHandler trafficShapingHandler;

    public GatewayServer(int port) {
        super(port);
    }

    @Override
    public void init() {
        super.init();
        MessageDispatcher receiver = new MessageDispatcher();
        receiver.register(Command.GATEWAY_PUSH, new GatewayPushHandler());
        connectionManager = new ServerConnectionManager();
        channelHandler = new ServerChannelHandler(false, connectionManager, receiver);
        if (enabled) {
            trafficShapingHandler = new GlobalChannelTrafficShapingHandler(
                    Executors.newSingleThreadScheduledExecutor()
                    , write_global_limit, read_global_limit,
                    write_channel_limit, read_channel_limit,
                    check_interval);
        }
    }

    @Override
    public void stop(Listener listener) {
        if (trafficShapingHandler != null) {
            trafficShapingHandler.release();
        }
        super.stop(listener);
        if (connectionManager != null) {
            connectionManager.destroy();
        }
    }

    @Override
    protected void initPipeline(ChannelPipeline pipeline) {
        super.initPipeline(pipeline);
        if (trafficShapingHandler != null) {
            pipeline.addLast(trafficShapingHandler);
        }
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }
}
