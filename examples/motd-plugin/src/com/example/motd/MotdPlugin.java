package com.example.motd;

import proxyti.api.EventHandler;
import proxyti.api.Plugin;
import proxyti.api.ProxyTi;
import proxyti.api.events.HandshakeEvent;
import proxyti.api.events.StatusRequestEvent;

/**
 * Example ProxyTi plugin. It decorates the MOTD, reports the live player count
 * and logs every host that players connect through.
 */
public final class MotdPlugin implements Plugin {
    private static final String COLOR = "\u00a7";
    private ProxyTi proxy;

    @Override
    public void onLoad(ProxyTi proxy) {
        this.proxy = proxy;
        proxy.events().register(this);
        proxy.logger().info("Loaded (ProxyTi %s)", proxy.version());
    }

    @Override
    public void onEnable() {
        proxy.logger().info("Enabled - answering %d configured backend(s)", proxy.servers().size());
    }

    @Override
    public void onDisable() {
        proxy.logger().info("Disabled");
    }

    @EventHandler(priority = 10)
    public void onStatus(StatusRequestEvent event) {
        event.setDescription(event.description() + " " + COLOR + "7[" + COLOR + "bProxyTi" + COLOR + "7]");
        event.setOnlinePlayers(proxy.onlinePlayers());
    }

    @EventHandler
    public void onHandshake(HandshakeEvent event) {
        proxy.logger().debug("%s -> %s", event.requestedHost(), event.targetServer());
    }
}