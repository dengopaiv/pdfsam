/*
 * This file is part of the PDF Split And Merge source code
 * Created on 11/apr/2014
 * Copyright 2017 by Sober Lemur S.r.l. (info@soberlemur.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.gui.components.notification;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.unicons.UniconsLine;
import org.pdfsam.ui.components.support.Style;

import java.util.StringJoiner;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.i18n.I18nContext.i18n;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

/**
 * Container for a basic notification with title, text and icon.
 * 
 * @author Andrea Vacondio
 *
 */
class Notification extends VBox {

    private final FadeTransition fade = new FadeTransition(Duration.millis(500), this);
    private Runnable onFocusLeft = () -> {
    };

    Notification(String title, Node content) {
        requireNotNullArg(content, "Notification content cannot be blank");
        getStyleClass().add("notification");
        getStyleClass().addAll(Style.CONTAINER.css());
        setId(UUID.randomUUID().toString());
        setAccessibleRole(AccessibleRole.TEXT);
        //the whole notification, so that screen readers read it all when it gets the focus
        setAccessibleText(accessibleText(title, content));
        setAccessibleRoleDescription(i18n().tr("Notification"));
        setFocusTraversable(true);
        focusWithinProperty().addListener((o, oldVal, focused) -> {
            if (focused) {
                fade.stop();
                setOpacity(1);
            } else {
                onFocusLeft.run();
            }
        });
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                eventStudio().broadcast(new RemoveNotificationRequest(getId()));
                e.consume();
            }
        });
        var closeButton = new Button("", FontIcon.of(UniconsLine.TIMES));
        closeButton.getStyleClass().addAll("close-button");
        closeButton.setAccessibleText(i18n().tr("Close notification"));
        closeButton.setTooltip(new Tooltip(i18n().tr("Close notification")));
        closeButton.setOnAction(e -> eventStudio().broadcast(new RemoveNotificationRequest(getId())));
        Label titleLabel = new Label(title);
        titleLabel.setPrefWidth(Integer.MAX_VALUE);
        titleLabel.getStyleClass().add("notification-title");
        StackPane top = new StackPane(titleLabel, closeButton);
        top.setAlignment(Pos.TOP_RIGHT);
        getChildren().addAll(top, content);
        setOpacity(0);
        setOnMouseEntered(e -> {
            fade.stop();
            setOpacity(1);
        });
        setOnMouseClicked(e -> {
            setOnMouseEntered(null);
            setOnMouseExited(null);
            fade.stop();
            eventStudio().broadcast(new RemoveNotificationRequest(getId()));
        });
        fade.setFromValue(1);
        fade.setToValue(0);
    }

    private static String accessibleText(String title, Node content) {
        var text = new StringJoiner(". ");
        if (isNotBlank(title)) {
            text.add(title);
        }
        collectText(content, text);
        return text.toString();
    }

    private static void collectText(Node node, StringJoiner text) {
        if (node instanceof Labeled labeled && !(node instanceof ButtonBase)) {
            if (isNotBlank(labeled.getText())) {
                text.add(labeled.getText());
            }
        } else if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(c -> collectText(c, text));
        }
    }

    /**
     * @param onFocusLeft action to perform when the focus leaves the notification
     */
    void onFocusLeft(Runnable onFocusLeft) {
        this.onFocusLeft = requireNonNullElse(onFocusLeft, () -> {
        });
    }

    /**
     * Fades the notification away after the given delay, unless it has the focus. A notification someone is reading stays.
     */
    void autoFadeAway(Duration delay) {
        if (!isFocusWithin()) {
            fadeAway(delay);
        }
    }

    void onFade(EventHandler<ActionEvent> onFaded) {
        fade.setOnFinished(onFaded);
    }

    void fadeAway(Duration delay) {
        fade.stop();
        fade.setDelay(delay);
        fade.jumpTo(Duration.ZERO);
        fade.play();
    }

    void fadeAway() {
        fadeAway(Duration.ZERO);
    }

}
