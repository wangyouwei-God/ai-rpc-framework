package com.aicore.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer {
    private final Random random = new Random();

    @Override
    public InetSocketAddress select(List<InetSocketAddress> serviceAddresses) {
        if (serviceAddresses == null || serviceAddresses.isEmpty()) {
            return null;
        }
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}