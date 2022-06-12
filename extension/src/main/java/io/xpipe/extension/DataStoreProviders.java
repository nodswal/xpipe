package io.xpipe.extension;

import io.xpipe.core.config.Dialog;
import io.xpipe.extension.event.ErrorEvent;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static Set<DataStoreProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(p -> (DataStoreProvider) p.get()).collect(Collectors.toSet());
            ALL.forEach(p -> {
                try {
                    p.init();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
            });
        }
    }

    public static Optional<DataStoreProvider> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }

    public static Optional<Dialog> byURL(URL url) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().map(d -> d.dialogForURL(url)).filter(Objects::nonNull).findAny();
    }

    public static Set<DataStoreProvider> getAll() {
        return ALL;
    }
}