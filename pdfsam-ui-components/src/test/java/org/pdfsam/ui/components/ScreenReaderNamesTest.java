/*
 * This file is part of the PDF Split And Merge source code
 * Created on 11/nov/2026
 * Copyright 2026 by Sober Lemur S.r.l. (info@soberlemur.com).
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
package org.pdfsam.ui.components;

import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pdfsam.core.support.validation.Validators;
import org.pdfsam.model.io.FileType;
import org.pdfsam.model.io.OpenType;
import org.pdfsam.test.JavaFxThreadExtension;
import org.pdfsam.ui.components.commons.RadioButtonDrivenTextFieldsPane;
import org.pdfsam.ui.components.commons.ValidableTextField;
import org.pdfsam.ui.components.io.BrowsableFileField;
import org.pdfsam.ui.components.support.FXValidationSupport.ValidationState;
import org.pdfsam.ui.components.support.Views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screen readers on Windows read the accessible text of text fields and combo boxes as their value, and take their name from the label
 * pointing at them. These tests check that input fields are named through labels and leave the accessible text alone.
 */
@ExtendWith({ JavaFxThreadExtension.class })
public class ScreenReaderNamesTest {

    private static String nameOf(TextField field) {
        var labeledBy = field.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
        return assertInstanceOf(Label.class, labeledBy).getText();
    }

    @Test
    public void hiddenLabelNamesTheTarget() {
        var field = new TextField();
        var label = Views.hiddenLabelFor(field, "Chuck");
        assertSame(label, field.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY));
        assertEquals("Chuck", label.getText());
        assertFalse(label.isVisible());
        assertFalse(label.isManaged());
    }

    @Test
    public void browsableFieldNamedAfterItsPrompt() {
        var victim = new BrowsableFileField(FileType.PDF, OpenType.OPEN);
        var field = victim.getTextField();
        field.setText("C:\\chuck.pdf");
        assertNull(field.getAccessibleText());
        assertEquals(field.getPromptText(), nameOf(field));
        field.setPromptText("Select or drag and drop the PDF");
        assertEquals("Select or drag and drop the PDF", nameOf(field));
    }

    @Test
    public void invalidBrowsableFieldSaysSoInItsName() {
        var victim = new BrowsableFileField(FileType.PDF, OpenType.OPEN);
        var field = victim.getTextField();
        field.setValidator(Validators.nonBlank());
        field.validate();
        assertEquals(ValidationState.INVALID, field.getValidationState());
        assertNotEquals(field.getPromptText(), nameOf(field));
        assertTrue(nameOf(field).contains(field.getPromptText()));
        assertNull(field.getAccessibleText());
        field.setText("chuck");
        field.validate();
        assertEquals(field.getPromptText(), nameOf(field));
    }

    @Test
    public void validationErrorGoesInTheHelpAndIsRemoved() {
        var victim = new ValidableTextField();
        victim.setAccessibleHelp("Enter a number");
        victim.setErrorMessage("Invalid number");
        victim.setValidator(Validators.nonBlank());
        victim.validate();
        assertEquals(ValidationState.INVALID, victim.getValidationState());
        assertNull(victim.getAccessibleText());
        assertEquals("Invalid number. Enter a number", victim.getAccessibleHelp());
        victim.setText("3");
        victim.validate();
        assertEquals("Enter a number", victim.getAccessibleHelp());
        assertNull(victim.getAccessibleText());
    }

    @Test
    public void radioDrivenFieldNamedAfterItsRadio() {
        var victim = new RadioButtonDrivenTextFieldsPane(new ToggleGroup());
        var field = new TextField();
        victim.addRow(new RadioButton("Split after"), field, null);
        assertEquals("Split after", nameOf(field));
    }

    @Test
    public void radioDrivenFieldKeepsItsOwnLabel() {
        var victim = new RadioButtonDrivenTextFieldsPane(new ToggleGroup());
        var field = new TextField();
        var label = new Label("Pages");
        label.setLabelFor(field);
        victim.addRow(new RadioButton("Split after"), field, null);
        assertEquals("Pages", nameOf(field));
    }
}
