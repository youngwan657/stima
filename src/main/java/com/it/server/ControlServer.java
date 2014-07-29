package com.it.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.it.domain.AllMember;
import com.it.domain.Member;
import com.it.domain.Status;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class ControlServer extends Server {
  private static final Logger logger = LoggerFactory.getLogger(ControlServer.class);

  public ControlServer(Member member) {
    super(member);
  }

  @Override
  public int getPort() {
    return myInfo.getControlPort();
  }

  @Override
  public void run() {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
          .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel socketChannel) throws Exception {
              socketChannel.pipeline().addLast(new ObjectEncoder(),
                  new ObjectDecoder(ClassResolvers.cacheDisabled(null)), new ServerHandlerAdapter());
            }
          }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

      ChannelFuture channelFuture = bootstrap.bind(getPort()).sync();

      AllMember.getInstance().getMemberInfos().putControlChannelFuture(myInfo, channelFuture);
      myInfo.setStatus(Status.STANDBY);
      isStartup = true;
      logger.info("controlServer started (port: {})", getPort());

      awaitDisconnection(channelFuture);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }
}
