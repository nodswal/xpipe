package io.xpipe.app.browser.fs;

import io.xpipe.app.browser.BrowserFilterComp;
import io.xpipe.app.browser.BrowserNavBar;
import io.xpipe.app.browser.BrowserOverviewComp;
import io.xpipe.app.browser.BrowserStatusBarComp;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.file.BrowserContextMenu;
import io.xpipe.app.browser.file.BrowserFileListComp;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.Shortcuts;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;
    private final boolean showStatusBar;

    public OpenFileSystemComp(OpenFileSystemModel model, boolean showStatusBar) {
        this.model = model;
        this.showStatusBar = showStatusBar;
    }

    @Override
    protected Region createSimple() {
        var alertOverlay = new ModalOverlayComp(Comp.of(() -> createContent()), model.getOverlay());
        return alertOverlay.createRegion();
    }

    private Region createContent() {
        var overview = new Button(null, new FontIcon("mdi2m-monitor"));
        overview.setOnAction(e -> model.cdAsync(null));
        overview.disableProperty().bind(model.getInOverview());
        overview.setAccessibleText("System overview");

        var backBtn = BrowserAction.byId("back", model, List.of()).toButton(model, List.of());
        var forthBtn = BrowserAction.byId("forward", model, List.of()).toButton(model, List.of());
        var refreshBtn = BrowserAction.byId("refresh", model, List.of()).toButton(model, List.of());
        var terminalBtn = BrowserAction.byId("openTerminal", model, List.of()).toButton(model, List.of());

        var menuButton = new MenuButton(null, new FontIcon("mdral-folder_open"));
        new ContextMenuAugment<>(
                        event -> event.getButton() == MouseButton.PRIMARY,
                        null,
                        () -> new BrowserContextMenu(model, null))
                .augment(new SimpleCompStructure<>(menuButton));
        menuButton.disableProperty().bind(model.getInOverview());
        menuButton.setAccessibleText("Directory options");

        var filter = new BrowserFilterComp(model, model.getFilter()).createStructure();
        Shortcuts.addShortcut(filter.toggleButton(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        var topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.getStyleClass().add("top-bar");
        topBar.getChildren()
                .setAll(
                        overview,
                        backBtn,
                        forthBtn,
                        new Spacer(10),
                        new BrowserNavBar(model).hgrow().createRegion(),
                        new Spacer(5),
                        filter.get(),
                        refreshBtn,
                        terminalBtn,
                        menuButton);

        var content = createFileListContent();
        var root = new VBox(topBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        return root;
    }

    private Region createFileListContent() {
        var directoryView = new BrowserFileListComp(model.getFileList())
                .apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
        var fileListElements = new ArrayList<Comp<?>>();
        fileListElements.add(directoryView);
        if (showStatusBar) {
            var statusBar = new BrowserStatusBarComp(model);
            fileListElements.add(statusBar);
        }
        var fileList = new VerticalComp(fileListElements);

        var home = new BrowserOverviewComp(model);
        var stack = new MultiContentComp(Map.of(
                home,
                model.getCurrentPath().isNull(),
                fileList,
                model.getCurrentPath().isNull().not()));
        return stack.createRegion();
    }
}
