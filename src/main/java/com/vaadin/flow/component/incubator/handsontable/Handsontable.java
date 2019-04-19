package com.vaadin.flow.component.incubator.handsontable;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;

@JavaScript("frontend://handsontable/dist/handsontable.full.js")
@StyleSheet("frontend://handsontable/dist/handsontable.full.css")
@JavaScript("frontend://handsontable/dist/languages/all.js")
@JavaScript("frontend://handsontableConnector.js")
@StyleSheet("frontend://handsontable-extra.css")
public class Handsontable extends Div {
    private Map<UUID, Consumer<JsonArray>> jsonArrayConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<List<String[]>>> listOfStringArrayConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<List<Cell>>> cellListConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<Settings>> settingsConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<String>> stringConsumers = new HashMap<>(1);

    /**
     * creates an empty Handsontable
     */
    public Handsontable() {
        String language = UI.getCurrent().getLocale().toString().replaceAll("_", "-");
        String initFunction = "createHandsontable($0, $1);";
        UI.getCurrent().getPage().executeJavaScript(initFunction, this, language);
    }

    /**
     * creates a Handsontable and initialize it with the given data.
     *
     * @param data
     */
    public Handsontable(JsonArray data) {
        String language = UI.getCurrent().getLocale().toString().replaceAll("_", "-");
        String initFunction = "createHandsontable($0, $1, $2);";
        UI.getCurrent().getPage().executeJavaScript(initFunction, this, language, data.toString());
    }

    /**
     * @param data
     */
    public void setData(JsonArray data) {
        getElement().callFunction("$handsontable.setData", data.toString());
    }

    /**
     * retrieves data with the same structure that passed by
     * <code>setData</code>.
     *
     * @param callback When the data is ready, <code>callback</code> is called
     *                 and the received data is passed to it.
     */
    public void retrieveData(Consumer<JsonArray> callback) {
        UUID uuid = UUID.randomUUID();
        jsonArrayConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveData", uuid.toString());
    }

    /**
     * retrieves data with in a form of a list of array of String.
     *
     * @param callback When the data is ready, <code>callback</code> is called
     *                 and the received data is passed to it.
     */
    public void retrieveDataAsArray(Consumer<List<String[]>> callback) {
        UUID uuid = UUID.randomUUID();
        listOfStringArrayConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveDataAsArray", uuid.toString());
    }

    /**
     * sets meta-data for the given cells.
     *
     * @param cells
     */
    public void setCellsMeta(List<Cell> cells) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String stringValue = mapper.writeValueAsString(cells);
            getElement().callFunction("$handsontable.setCellsMeta", stringValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * retrieves meta-data of all cells.
     *
     * @param callback When the data is ready, <code>callback</code> is called
     *                 and the received data is passed to it.
     */
    public void retrieveCellsMeta(Consumer<List<Cell>> callback) {
        UUID uuid = UUID.randomUUID();
        cellListConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveCellsMeta", uuid.toString());
    }

    /**
     * @param settings
     */
    public void setSettings(Settings settings) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, settings);
            String stringValue = writer.toString();
            writer.close();
            getElement().callFunction("$handsontable.setSettings", stringValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param callback When the settings is ready, <code>callback</code> is
     *                 called and the received settings is passed to it.
     */
    public void retrieveSettings(Consumer<Settings> callback) {
        UUID uuid = UUID.randomUUID();
        settingsConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveSettings", uuid.toString());
    }

    @ClientCallable
    private void receiveSettings(String uuidStr, String settingsStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            Consumer<Settings> consumer = settingsConsumers.remove(uuid);
            Objects.requireNonNull(consumer, "settingsConsumer with the given UUID was not found!");
            ObjectMapper mapper = new ObjectMapper();
            Settings settings = mapper.readValue(settingsStr, Settings.class);
            consumer.accept(settings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String[]> convertToListOfStringArray(JsonArray jsonArray) {
        List<String[]> list = new ArrayList<>(jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            String[] array = new String[jsonArray.getJsonArray(i).size()];
            for (int j = 0; j < array.length; j++)
                array[j] = jsonArray.getJsonArray(i).get(j).toString();
            list.add(array);
        }

        return list;
    }

    @ClientCallable
    private void receiveListOfStringArray(String uuidStr, String data) {
        UUID uuid = UUID.fromString(uuidStr);
        Consumer<List<String[]>> consumer = listOfStringArrayConsumers.remove(uuid);
        Objects.requireNonNull(consumer, "listOfStringArrayConsumer with the given UUID was not found!");

        JsonReader reader = Json.createReader(new StringReader(data));
        JsonArray jsonArray = reader.readArray();

        List<String[]> list = convertToListOfStringArray(jsonArray);
        reader.close();
        consumer.accept(list);
    }

    @ClientCallable
    private void receiveJsonArray(String uuidStr, String data) {
        UUID uuid = UUID.fromString(uuidStr);
        Consumer<JsonArray> consumer = jsonArrayConsumers.remove(uuid);
        Objects.requireNonNull(consumer, "jsonArrayConsumer with the given UUID was not found!");

        JsonReader reader = Json.createReader(new StringReader(data));
        JsonArray jsonArray = reader.readArray();
        reader.close();
        consumer.accept(jsonArray);
    }

    @ClientCallable
    private void receiveCellsMeta(String uuidStr, String cellsMeta) {
        UUID uuid = UUID.fromString(uuidStr);
        Consumer<List<Cell>> consumer = cellListConsumers.remove(uuid);
        Objects.requireNonNull(consumer, "cellListConsumer with the given UUID was not found!");

        JsonReader reader = Json.createReader(new StringReader(cellsMeta));
        JsonArray jsonArray = reader.readArray();

        List<Cell> list = convertToListOfCellsArray(jsonArray);
        reader.close();
        consumer.accept(list);
    }

    private List<Cell> convertToListOfCellsArray(JsonArray jsonArray) {
        List<Cell> list = new ArrayList<>(jsonArray.size());
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (JsonValue jsonValue : jsonArray) {
                Cell cell = mapper.readValue(jsonValue.toString(), Cell.class);
                list.add(cell);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    /**
     * sets headers of the table. It is used when the table has multiple header
     * rows or merged cells in the header.
     *
     * @param nestedHeaders
     * @see <a href="https://handsontable.com/docs/7.0.2/demo-nested-headers.html">
     * Nested headers document on Handsontable website</a>
     */
    public void setNestedHeaders(JsonArray nestedHeaders) {
        getElement().callFunction("$handsontable.setNestedHeaders", nestedHeaders.toString());
    }

    /**
     * inserts empty columns at the given index.
     *
     * @param index  starting index where new columns should be inserted
     * @param amount the number of columns
     */
    public void insertCol(int index, int amount) {
        getElement().callFunction("$handsontable.alter", "insert_col", index, amount);
    }

    /**
     * inserts empty rows at the given index.
     *
     * @param index  starting index where new rows should be inserted
     * @param amount the number of rows
     */
    public void insertRow(int index, int amount) {
        getElement().callFunction("$handsontable.alter", "insert_row", index, amount);
    }

    /**
     * removes columns at the given index.
     *
     * @param index  starting index where columns should be removed
     * @param amount the number of columns to be removed
     */
    public void removeCol(int index, int amount) {
        getElement().callFunction("$handsontable.alter", "remove_col", index, amount);
    }

    /**
     * removes rows at the given index.
     *
     * @param index  starting index where rows should be removed
     * @param amount the number of rows to be removed
     */
    public void removeRow(int index, int amount) {
        getElement().callFunction("$handsontable.alter", "remove_row", index, amount);
    }

    /**
     *
     * @param row
     * @param col
     * @param value
     */
    public void setDataAtCell(int row, int col, String value) {
        getElement().callFunction("$handsontable.setDataAtCell", row, col, value);
    }

    /**
     *
     * @param row
     * @param col
     * @param callback
     */
    public void retrieveDataAtCell(int row, int col, Consumer<String> callback) {
        UUID uuid = UUID.randomUUID();
        stringConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveDataAtCell", row, col, uuid.toString());
    }

    @ClientCallable
    private void receiveString(String uuidStr, String data) {
        UUID uuid = UUID.fromString(uuidStr);
        Consumer<String> consumer = stringConsumers.remove(uuid);
        Objects.requireNonNull(consumer, "stringConsumer with the given UUID was not found!");
        consumer.accept(data);
    }

    /**
     *
     * @param classNames
     */
    public void setHeaderClassNames(String[] classNames) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String stringValue = mapper.writeValueAsString(classNames);
            getElement().callFunction("$handsontable.setHeaderClassNames", stringValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
