package com.it.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.it.client.Client;
import com.it.client.ClientHandlerAdapter;
import com.it.command.StartCommand;
import com.it.command.StopCommand;
import com.it.common.Config;
import com.it.common.Sender;
import com.it.model.AllMember;
import com.it.model.Clusters;
import com.it.model.Member;
import com.it.model.Status;
import com.it.server.Server;
import com.it.server.ServerHandlerAdapter;

public class ItRunner {
    private static final Logger logger = LoggerFactory
            .getLogger(ItRunner.class);

    public static ItRunner instance = new ItRunner();
    private ServerHandlerAdapter serverHandlerAdapter;
    private ClientHandlerAdapter clientHandlerAdapter;

    public static ItRunner getInstance() {
        return instance;
    }

    public ServerHandlerAdapter getServerHandlerAdapter() {
        return serverHandlerAdapter;
    }

    public ClientHandlerAdapter getClientHandlerAdapter() {
        return clientHandlerAdapter;
    }

    public void execute(ServerHandlerAdapter serverHandlerAdapter,
            ClientHandlerAdapter clientHandlerAdapter, String[] args) {
        this.serverHandlerAdapter = serverHandlerAdapter;
        this.clientHandlerAdapter = clientHandlerAdapter;

        try {
            Config.getInstance().init(args);
            Clusters clusters = AllMember.getInstance().getClusters();

            // server
            Server server = new Server(clusters.findMe());
            server.setServerHandler(serverHandlerAdapter);
            server.start();
            server.await();

            // clients
            for (String clusterName : clusters.getClusterNames()) {
                for (Member member : clusters.getMemberListIn(clusterName)
                        .getMembers()) {
                    if (!member.isMe()) {
                        Client client = new Client(member);
                        client.setClientHandler(clientHandlerAdapter);
                        client.start();
                        client.await();
                    }
                }
            }

            // change status to Running
            Member myInfo = server.getMyInfo();
            AllMember.getInstance()
                    .getMember(myInfo.getHost(), myInfo.getPort())
                    .setStatus(Status.RUNNING);

            logger.info(AllMember.getInstance().toString());

            Thread.sleep(Config.getInstance().getSpreadTime() * 1000);

            // broadcast StartCommand
            Sender.sendBroadcast(new StartCommand(Config.getInstance()
                    .getHost(), Config.getInstance().getPort()));
        } catch (Exception e) {
            shutdown();
        }
    }

    public void shutdown() {
        Sender.sendBroadcast(new StopCommand(Config.getInstance().getHost(),
                Config.getInstance().getPort()));

        Member me = AllMember.getInstance().getClusters().findMe();
        int waitTime = 100;
        int maxWaitTime = 5000;
        do {
            try {
                Thread.sleep(waitTime);
                maxWaitTime -= waitTime;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("await to shutdown.");
        } while(me.getReceivedTPS() > 0 && maxWaitTime > 0);
        System.exit(0);
    }
}