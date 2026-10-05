# ProxyTi

ProxyTi is a small, dependency-free Java 21 Minecraft Java Edition routing
proxy. Its default mode is deliberately transparent:

1. It reads the initial Minecraft handshake.
2. It chooses a backend using the requested hostname.
3. It sends a handshake to that backend.
4. It copies every remaining byte in both directions.

After the handshake, Minecraft encryption and compression remain end-to-end
between the client and the backend. Therefore the backend does **not** need a
plugin, mod, Velocity registration, forwarding secret, or proxy-specific
configuration. This supports untouched vanilla online-mode servers.

## Important limitation

This zero-registration mode is a routing/reverse proxy, not a protocol-terminating
Velocity replacement. It can select a backend when the player connects, but it
cannot inspect chat or switch servers during an existing encrypted connection.
Seamless `/server` switching requires the proxy to terminate the client login,
which necessarily means either offline-mode backends or a supported forwarding
configuration. ProxyTi's transparent mode intentionally avoids that requirement.

## Build and run

Requirements: Java 21 or newer.

```powershell
.\build.ps1
.\run.ps1
```

The first argument to `run.ps1` can be an alternate config file:

```powershell
.\run.ps1 .\config.properties
```

## Configuration

```properties
bind=0.0.0.0:25565
default-server=lobby
server.lobby=127.0.0.1:25566
server.survival=127.0.0.1:25567
forced-host.lobby.example.com=lobby
forced-host.survival.example.com=survival
```

Point both DNS names at the proxy's public IP. Players connecting to
`lobby.example.com` or `survival.example.com` will be routed to the matching
backend. The backend server ports should not be publicly exposed if the goal is
to force all connections through the proxy.

`intercept-status=true` makes ProxyTi answer server-list pings itself using the
configured MOTD and active login count. With the default `false`, status is
passed through to the selected backend.

## Security

Because the backend performs the normal Minecraft authentication in transparent
mode, this proxy does not weaken online-mode authentication. Restrict backend
firewall access to the proxy host where possible.
