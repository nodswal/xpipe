package io.xpipe.app.terminal;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.*;
import io.xpipe.core.process.*;
import io.xpipe.core.util.FailableFunction;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public interface ExternalTerminalType extends PrefsChoiceValue {

    //    ExternalTerminalType PUTTY = new WindowsType("app.putty","putty") {
    //
    //        @Override
    //        protected Optional<Path> determineInstallation() {
    //            try {
    //                var r = WindowsRegistry.local().readValue(WindowsRegistry.HKEY_LOCAL_MACHINE,
    //                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\Xshell.exe");
    //                return r.map(Path::of);
    //            }  catch (Exception e) {
    //                ErrorEvent.fromThrowable(e).omit().handle();
    //                return Optional.empty();
    //            }
    //        }
    //
    //        @Override
    //        public boolean supportsTabs() {
    //            return true;
    //        }
    //
    //        @Override
    //        public boolean isRecommended() {
    //            return false;
    //        }
    //
    //        @Override
    //        public boolean supportsColoredTitle() {
    //            return false;
    //        }
    //
    //        @Override
    //        protected void execute(Path file, LaunchConfiguration configuration) throws Exception {
    //            try (var sc = LocalShell.getShell()) {
    //                SshLocalBridge.init();
    //                var b = SshLocalBridge.get();
    //                var command = CommandBuilder.of().addFile(file.toString()).add("-ssh", "localhost",
    // "-l").addQuoted(b.getUser())
    //                        .add("-i").addFile(b.getIdentityKey().toString()).add("-P", "" +
    // b.getPort()).add("-hostkey").addFile(b.getPubHostKey().toString());
    //                sc.executeSimpleCommand(command);
    //            }
    //        }
    //    };

    static ExternalTerminalType determineNonSshBridgeFallback(ExternalTerminalType type) {
        if (type == XSHELL || type == MOBAXTERM || type == SECURECRT) {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD ? CMD : POWERSHELL;
        }

        if (type != TERMIUS) {
            return type;
        }

        switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                // This should not be termius as all others take precedence
                var def = determineDefault(null);
                // If there's no other terminal available, use a fallback which won't work
                return def != TERMIUS ? def : XTERM;
            }
            case OsType.MacOs macOs -> {
                return MACOS_TERMINAL;
            }
            case OsType.Windows windows -> {
                return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.CMD ? CMD : POWERSHELL;
            }
        }
    }

    ExternalTerminalType XSHELL = new XShellTerminalType();

    ExternalTerminalType SECURECRT = new SecureCrtTerminalType();

    ExternalTerminalType MOBAXTERM = new MobaXTermTerminalType();

    ExternalTerminalType TERMIUS = new TermiusTerminalType();

    ExternalTerminalType CMD = new CmdTerminalType();

    ExternalTerminalType POWERSHELL = new PowerShellTerminalType();

    ExternalTerminalType PWSH = new PwshTerminalType();

    ExternalTerminalType GNOME_TERMINAL = new GnomeTerminalType();

    ExternalTerminalType KONSOLE = new SimplePathType("app.konsole", "konsole", true) {

        @Override
        public String getWebsite() {
            return "https://konsole.kde.org/download.html";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return false;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            // Note for later: When debugging konsole launches, it will always open as a child process of
            // IntelliJ/XPipe even though we try to detach it.
            // This is not the case for production where it works as expected
            return CommandBuilder.of()
                    .addIf(configuration.isPreferTabs(), "--new-tab")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XFCE = new SimplePathType("app.xfce", "xfce4-terminal", true) {
        @Override
        public String getWebsite() {
            return "https://docs.xfce.org/apps/terminal/start";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(configuration.isPreferTabs(), "--tab")
                    .add("--title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--command")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType FOOT = new SimplePathType("app.foot", "foot", true) {
        @Override
        public String getWebsite() {
            return "https://codeberg.org/dnkl/foot";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("--title")
                    .addQuoted(configuration.getColoredTitle())
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType ELEMENTARY = new SimplePathType("app.elementaryTerminal", "io.elementary.terminal", true) {

        @Override
        public String getWebsite() {
            return "https://github.com/elementary/terminal";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(configuration.isPreferTabs(), "--new-tab")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILIX = new SimplePathType("app.tilix", "tilix", true) {
        @Override
        public String getWebsite() {
            return "https://gnunn1.github.io/tilix-web/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-t")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TERMINATOR = new SimplePathType("app.terminator", "terminator", true) {
        @Override
        public String getWebsite() {
            return "https://gnome-terminator.org/";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-e")
                    .addFile(configuration.getScriptFile())
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .addIf(configuration.isPreferTabs(), "--new-tab");
        }
    };
    ExternalTerminalType TERMINOLOGY = new SimplePathType("app.terminology", "terminology", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/borisfaure/terminology";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .addIf(!configuration.isPreferTabs(), "-s")
                    .add("-T")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-2")
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType GUAKE = new SimplePathType("app.guake", "guake", true) {

        @Override
        public int getProcessHierarchyOffset() {
            return 1;
        }

        @Override
        public String getWebsite() {
            return "https://github.com/Guake/guake";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-n", "~")
                    .add("-r")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType TILDA = new SimplePathType("app.tilda", "tilda", true) {
        @Override
        public String getWebsite() {
            return "https://github.com/lanoxx/tilda";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-c").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType XTERM = new SimplePathType("app.xterm", "xterm", true) {
        @Override
        public String getWebsite() {
            return "https://invisible-island.net/xterm/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of()
                    .add("-title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("-e")
                    .addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType DEEPIN_TERMINAL = new SimplePathType("app.deepinTerminal", "deepin-terminal", true) {

        @Override
        public int getProcessHierarchyOffset() {
            return 1;
        }

        @Override
        public String getWebsite() {
            return "https://www.deepin.org/en/original/deepin-terminal/";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-C").addFile(configuration.getScriptFile());
        }
    };
    ExternalTerminalType Q_TERMINAL = new SimplePathType("app.qTerminal", "qterminal", true) {

        @Override
        public int getProcessHierarchyOffset() {
            return ProcessControlProvider.get().getEffectiveLocalDialect() == ShellDialects.BASH ? 0 : 1;
        }

        @Override
        public String getWebsite() {
            return "https://github.com/lxqt/qterminal";
        }

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.NEW_WINDOW;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return CommandBuilder.of().add("-e").add(configuration.getDialectLaunchCommand());
        }
    };
    ExternalTerminalType MACOS_TERMINAL = new MacOsType("app.macosTerminal", "Terminal") {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        public int getProcessHierarchyOffset() {
            return 2;
        }

        @Override
        public boolean isRecommended() {
            return false;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("Terminal.app")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType ITERM2 = new MacOsType("app.iterm2", "iTerm") {

        @Override
        public TerminalOpenFormat getOpenFormat() {
            return TerminalOpenFormat.TABBED;
        }

        @Override
        public int getProcessHierarchyOffset() {
            return 3;
        }

        @Override
        public String getWebsite() {
            return "https://iterm2.com/";
        }

        @Override
        public boolean isRecommended() {
            return true;
        }

        @Override
        public boolean supportsColoredTitle() {
            return true;
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            LocalShell.getShell()
                    .executeSimpleCommand(CommandBuilder.of()
                            .add("open", "-a")
                            .addQuoted("iTerm.app")
                            .addFile(configuration.getScriptFile()));
        }
    };
    ExternalTerminalType WARP = new WarpTerminalType();
    ExternalTerminalType CUSTOM = new CustomTerminalType();
    List<ExternalTerminalType> WINDOWS_TERMINALS = List.of(
            WindowsTerminalType.WINDOWS_TERMINAL_CANARY,
            WindowsTerminalType.WINDOWS_TERMINAL_PREVIEW,
            WindowsTerminalType.WINDOWS_TERMINAL,
            AlacrittyTerminalType.ALACRITTY_WINDOWS,
            WezTerminalType.WEZTERM_WINDOWS,
            CMD,
            PWSH,
            POWERSHELL,
            MOBAXTERM,
            SECURECRT,
            TERMIUS,
            XSHELL,
            TabbyTerminalType.TABBY_WINDOWS);
    List<ExternalTerminalType> LINUX_TERMINALS = List.of(
            AlacrittyTerminalType.ALACRITTY_LINUX,
            WezTerminalType.WEZTERM_LINUX,
            KittyTerminalType.KITTY_LINUX,
            TERMINATOR,
            TERMINOLOGY,
            XFCE,
            ELEMENTARY,
            KONSOLE,
            GNOME_TERMINAL,
            TILIX,
            GUAKE,
            TILDA,
            XTERM,
            DEEPIN_TERMINAL,
            FOOT,
            Q_TERMINAL,
            TERMIUS);
    List<ExternalTerminalType> MACOS_TERMINALS = List.of(
            WARP,
            ITERM2,
            KittyTerminalType.KITTY_MACOS,
            TabbyTerminalType.TABBY_MAC_OS,
            AlacrittyTerminalType.ALACRITTY_MAC_OS,
            WezTerminalType.WEZTERM_MAC_OS,
            MACOS_TERMINAL,
            TERMIUS);

    List<ExternalTerminalType> ALL = getTypes(OsType.getLocal(), false, true);

    List<ExternalTerminalType> ALL_ON_ALL_PLATFORMS = getTypes(null, false, true);

    static List<ExternalTerminalType> getTypes(OsType osType, boolean remote, boolean custom) {
        var all = new ArrayList<ExternalTerminalType>();
        if (osType == null || osType.equals(OsType.WINDOWS)) {
            all.addAll(WINDOWS_TERMINALS);
        }
        if (osType == null || osType.equals(OsType.LINUX)) {
            all.addAll(LINUX_TERMINALS);
        }
        if (osType == null || osType.equals(OsType.MACOS)) {
            all.addAll(MACOS_TERMINALS);
        }
        if (remote) {
            all.removeIf(externalTerminalType -> externalTerminalType.remoteLaunchCommand(null) == null);
        }
        // Prefer recommended
        all.sort(Comparator.comparingInt(o -> (o.isRecommended() ? -1 : 0)));
        if (custom) {
            all.add(CUSTOM);
        }
        return all;
    }

    static ExternalTerminalType determineDefault(ExternalTerminalType existing) {
        // Check for incompatibility with fallback shell
        if (ExternalTerminalType.CMD.equals(existing)
                && !ProcessControlProvider.get().getEffectiveLocalDialect().equals(ShellDialects.CMD)) {
            return ExternalTerminalType.POWERSHELL;
        }

        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        return ALL.stream()
                .filter(externalTerminalType -> !externalTerminalType.equals(CUSTOM))
                .filter(terminalType -> terminalType.isAvailable())
                .findFirst()
                .orElse(null);
    }

    default TerminalInitFunction additionalInitCommands() {
        return TerminalInitFunction.none();
    }

    TerminalOpenFormat getOpenFormat();

    default String getWebsite() {
        return null;
    }

    boolean isRecommended();

    boolean supportsColoredTitle();

    default boolean shouldClear() {
        return true;
    }

    default void launch(TerminalLaunchConfiguration configuration) throws Exception {}

    default FailableFunction<TerminalLaunchConfiguration, String, Exception> remoteLaunchCommand(
            ShellDialect systemDialect) {
        return null;
    }

    abstract class WindowsType extends ExternalApplicationType.WindowsType implements ExternalTerminalType {

        public WindowsType(String id, String executable) {
            super(id, executable);
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var location = determineFromPath();
            if (location.isEmpty()) {
                location = determineInstallation();
                if (location.isEmpty()) {
                    throw new IOException("Unable to find installation of "
                            + toTranslatedString().getValue());
                }
            }

            execute(location.get(), configuration);
        }

        protected abstract void execute(Path file, TerminalLaunchConfiguration configuration) throws Exception;
    }

    abstract class MacOsType extends ExternalApplicationType.MacApplication
            implements ExternalTerminalType, TrackableTerminalType {

        public MacOsType(String id, String applicationName) {
            super(id, applicationName);
        }
    }

    @Getter
    abstract class PathCheckType extends ExternalApplicationType.PathApplication implements ExternalTerminalType {

        public PathCheckType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }
    }

    @Getter
    abstract class SimplePathType extends PathCheckType implements TrackableTerminalType {

        public SimplePathType(String id, String executable, boolean explicitAsync) {
            super(id, executable, explicitAsync);
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            var args = toCommand(configuration);
            launch(configuration.getColoredTitle(), args);
        }

        @Override
        public FailableFunction<TerminalLaunchConfiguration, String, Exception> remoteLaunchCommand(
                ShellDialect systemDialect) {
            return launchConfiguration -> {
                var args = toCommand(launchConfiguration);
                args.add(0, executable);
                if (explicitlyAsync) {
                    args = systemDialect.launchAsnyc(args);
                }
                return args.buildSimple();
            };
        }

        protected abstract CommandBuilder toCommand(TerminalLaunchConfiguration configuration) throws Exception;
    }
}
