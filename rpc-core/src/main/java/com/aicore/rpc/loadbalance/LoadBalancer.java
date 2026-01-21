package com.aicore.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {
    InetSocketAddress select(List<InetSocketAddress> serviceAddresses);
}