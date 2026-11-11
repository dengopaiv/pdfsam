/*
 * This file is part of the PDF Split And Merge source code
 * Created on 25/mar/2014
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
package org.pdfsam.ui.components.selection.multiple;

import javafx.event.Event;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import org.pdfsam.ui.components.support.Views;

import java.util.Objects;
import java.util.Optional;

/**
 * Editable cell showing a tooltip.
 * <p></p>
 * Commit and focus behavior handling is still buggy and needs to be managed by us with a listener on the text field
 * and proper overrides. Waiting for PR: <a href="https://bugs.openjdk.org/browse/JDK-8089514">JDK-8089514</a>
 *
 * @author Andrea Vacondio
 */
class TooltippedTextFieldTableCell extends TooltippedTableCell<String> {

    private final TextField textField = new TextField();
    //names the editor for screen readers after the column it edits
    private final Label editorName = Views.hiddenLabelFor(textField, "");

    private final StringConverter<String> converter = new DefaultStringConverter();

    public TooltippedTextFieldTableCell(String tooltipMessage) {
        super(tooltipMessage);

        var editor = new HBox(editorName, textField);
        HBox.setHgrow(textField, Priority.ALWAYS);
        setGraphic(editor);
        setContentDisplay(ContentDisplay.TEXT_ONLY);

        textField.setOnAction(evt -> commitEdit(this.converter.fromString(textField.getText())));
        textField.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && isEditing()) {
                cancelEdit();
                e.consume();
            }
        });
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                // When the text field loses focus, the editing property flips to `false` and consequently
                // the backed-in commitEdit(...) method will simply bail.
                // To bypass this, we send a edit commit event.
                String newItem = converter.fromString(textField.getText());
                if (!isEditing() && !Objects.equals(getItem(), newItem)) {
                    TableView<SelectionTableRowData> table = getTableView();
                    TableColumn<SelectionTableRowData, String> column = getTableColumn();
                    TableColumn.CellEditEvent<SelectionTableRowData, String> event = new TableColumn.CellEditEvent<>(table,
                            new TablePosition<>(table, getIndex(), column), TableColumn.editCommitEvent(), newItem);
                    Event.fireEvent(column, event);
                } else {
                    commitEdit(newItem);
                }
            }
        });
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
        } else {
            setText(converter.toString(item));
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        Optional.ofNullable(getTableColumn()).map(TableColumn::getText).ifPresent(editorName::setText);
        textField.setText(converter.toString(getItem()));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        textField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        var editorFocused = textField.isFocused();
        super.cancelEdit();
        //discard what was typed so that losing the focus doesn't commit it
        textField.setText(converter.toString(getItem()));
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        returnFocusToTable(editorFocused);
    }

    /**
     * When editing ends with the keyboard (Enter or Escape) the editor is hidden, the focus goes back to the table so that keyboard users can
     * keep moving through the rows. When editing ends because the focus moved somewhere else, the focus is left there.
     */
    private void returnFocusToTable(boolean editorFocused) {
        if (editorFocused) {
            Optional.ofNullable(getTableView()).ifPresent(TableView::requestFocus);
        }
    }

    @Override
    public void commitEdit(String item) {
        var editorFocused = textField.isFocused();
        super.commitEdit(item);
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        returnFocusToTable(editorFocused);
    }
}
