/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 23 ott 2017
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

import javafx.beans.binding.Bindings;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.pdfsam.i18n.I18nContext.i18n;

/**
 * Index column showing a row header with the row index
 * 
 * @author Andrea Vacondio
 *
 */
public class IndexColumn extends TableColumn<SelectionTableRowData, Object> {

    public IndexColumn() {
        super("#");
        setComparator(null);
        setSortable(false);
        setPrefWidth(15);
        setMaxWidth(200);
        setCellFactory(new Callback<>() {

            @Override
            public TableCell<SelectionTableRowData, Object> call(TableColumn<SelectionTableRowData, Object> param) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText("");
                            setAccessibleText(null);
                        } else {
                            var index = Integer.toString(getIndex() + 1);
                            setText(index);
                            setAccessibleText(accessibleText(index));
                        }
                    }

                    /**
                     * This is the cell focused when moving through the rows, so it describes the whole row and not just its index
                     */
                    private String accessibleText(String index) {
                        var text = new StringBuilder(i18n().tr("Row {0}", index));
                        var row = getTableRow();
                        if (nonNull(row) && nonNull(row.getItem())) {
                            var descriptor = row.getItem().descriptor();
                            text.append(": ").append(descriptor.getFileName());
                            var pages = descriptor.pages().getValue();
                            if (nonNull(pages) && pages > 0) {
                                text.append(", ").append(i18n().tr("{0} pages", Integer.toString(pages)));
                            }
                            var status = descriptor.loadingStatus().getValue();
                            if (nonNull(status) && isNotBlank(status.getDescription())) {
                                text.append(", ").append(status.getDescription());
                            }
                        }
                        return text.toString();
                    }
                };
            }
        });

        //a new value every time pages or loading status change, so the cell updates its accessible text
        setCellValueFactory(param -> {
            var descriptor = param.getValue().descriptor();
            return Bindings.createObjectBinding(Object::new, descriptor.pages(), descriptor.loadingStatus());
        });
    }
}
