package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconShellSession;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ShellStartExchange;
import io.xpipe.core.store.ShellStore;
import lombok.SneakyThrows;

public class ShellStartExchangeImpl extends ShellStartExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new IllegalArgumentException("Unknown connection"));
        if (!(e.getStore() instanceof ShellStore s)) {
            throw new BeaconClientException("Not a shell connection");
        }

        var existing = AppBeaconServer.get().getCache().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(e))
                .findFirst();
        if (existing.isPresent()) {
            return Response.builder().build();
        }

        var control = s.control().start();
        AppBeaconServer.get().getCache().getShellSessions().add(new BeaconShellSession(e, control));
        return Response.builder().build();
    }
}
