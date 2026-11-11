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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.pdfsam.core.context.BooleanPersistentProperty;

import static java.util.Objects.nonNull;
import static org.pdfsam.core.context.ApplicationContext.app;

/**
 * Container for the notifications
 * 
 * @author Andrea Vacondio
 *
 */
public class NotificationsContainer extends VBox {

    private static final Duration FADE_DELAY = Duration.seconds(5);
    private Node focusBeforeNotification;

    public NotificationsContainer() {
        getStyleClass().add("notifications");
        setMaxHeight(Region.USE_PREF_SIZE);
    }

    void addNotification(String title, Node message) {
        addNotification(title, message, false);
    }

    /**
     * @param takeFocus whether the notification should get the focus, so that screen readers read it. For notifications reporting a problem
     *                  with something the user just did, the same way an alert dialog would.
     */
    void addNotification(String title, Node message, boolean takeFocus) {
        Platform.runLater(() -> {
            Notification toAdd = doAddNotification(title, message);
            if (app().persistentSettings().get(BooleanPersistentProperty.STICKY_NOTIFICATIONS)) {
                fadeIn(toAdd, null);
            } else {
                toAdd.onFocusLeft(() -> toAdd.autoFadeAway(FADE_DELAY));
                fadeIn(toAdd, e -> toAdd.autoFadeAway(FADE_DELAY));
            }
            if (takeFocus) {
                focus(toAdd);
            }
        });
    }

    void addStickyNotification(String title, Node message) {
        Platform.runLater(() -> {
            Notification toAdd = doAddNotification(title, message);
            fadeIn(toAdd, null);
        });
    }

    private Notification doAddNotification(String title, Node message) {
        Notification toAdd = new Notification(title, message);
        toAdd.onFade(e -> {
            var hadFocus = toAdd.isFocusWithin();
            getChildren().remove(toAdd);
            if (hadFocus) {
                restoreFocus();
            }
        });
        getChildren().add(toAdd);
        return toAdd;
    }

    /**
     * Moves the focus to the most recent notification, if any. This is how keyboard and screen reader users reach notifications, which are
     * otherwise only shown for a few seconds.
     */
    public void focusLatest() {
        getChildren().stream().filter(Notification.class::isInstance).map(Notification.class::cast)
                .reduce((first, second) -> second).ifPresent(this::focus);
    }

    private void focus(Notification notification) {
        var scene = getScene();
        if (nonNull(scene)) {
            var owner = scene.getFocusOwner();
            if (nonNull(owner) && !isFocusWithin()) {
                focusBeforeNotification = owner;
            }
            notification.requestFocus();
        }
    }

    private void restoreFocus() {
        if (nonNull(focusBeforeNotification) && focusBeforeNotification.getScene() == getScene()) {
            focusBeforeNotification.requestFocus();
        }
        focusBeforeNotification = null;
    }

    private void fadeIn(Notification toAdd, EventHandler<ActionEvent> onFinished) {
        FadeTransition transition = new FadeTransition(Duration.millis(300), toAdd);
        transition.setFromValue(0);
        transition.setToValue(1);
        if (onFinished != null) {
            transition.setOnFinished(onFinished);
        }
        transition.play();
    }

    void removeNotification(String id) {
        Node toRemove = lookup(String.format("#%s", id));
        if (toRemove instanceof Notification notification) {
            notification.fadeAway();
        }
    }

}
