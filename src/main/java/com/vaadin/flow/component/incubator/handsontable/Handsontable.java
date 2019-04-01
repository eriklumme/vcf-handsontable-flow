package com.vaadin.flow.component.incubator.handsontable;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;

@JavaScript("frontend://handsontable/dist/handsontable.full.js")
@StyleSheet("frontend://handsontable/dist/handsontable.full.css")
@JavaScript("frontend://handsontableConnector.js")
@StyleSheet("frontend://handsontable-extra.css")
public class Handsontable extends Div {
    private Map<UUID, Consumer<JsonArray>> jsonArrayConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<List<String[]>>> listOfStringArrayConsumers = new HashMap<>(1);
    private Map<UUID, Consumer<List<Settings.Cell>>> cellListConsumers = new HashMap<>(1);

    public Handsontable() {
        String initFunction = "createHandsontbale($0);";
        UI.getCurrent().getPage().executeJavaScript(initFunction, this);
    }

    public Handsontable(JsonArray data) {
        String initFunction = "createHandsontbale($0, $1);";
        UI.getCurrent().getPage().executeJavaScript(initFunction, this, data.toString());
    }

    public void setData(JsonArray data) {
        getElement().callFunction("$handsontable.setData", data.toString());
    }

    public void retrieveData(Consumer<JsonArray> callback) {
        UUID uuid = UUID.randomUUID();
        jsonArrayConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveData", uuid.toString());
    }

    public void retrieveDataAsArray(Consumer<List<String[]>> callback) {
        UUID uuid = UUID.randomUUID();
        listOfStringArrayConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveDataAsArray", uuid.toString());
    }

    public void setCellsMeta(List<Settings.Cell> cellsSettings) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String stringValue = mapper.writeValueAsString(cellsSettings);
            getElement().callFunction("$handsontable.setCellsMeta", stringValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrieveCellsMeta(Consumer<List<Settings.Cell>> callback) {
        UUID uuid = UUID.randomUUID();
        cellListConsumers.put(uuid, callback);
        getElement().callFunction("$handsontable.retrieveCellsMeta", uuid.toString());
    }

    public void setSettings(Settings settings) {
        throw new UnsupportedOperationException("setSettings isn't been implemented yet!");
    }

    public void retrieveSettings(Consumer<Settings> callback) {
        throw new UnsupportedOperationException("setSettings isn't been implemented yet!");
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
        Consumer<List<Settings.Cell>> consumer = cellListConsumers.remove(uuid);
        Objects.requireNonNull(consumer, "cellListConsumer with the given UUID was not found!");

        JsonReader reader = Json.createReader(new StringReader(cellsMeta));
        JsonArray jsonArray = reader.readArray();

        List<Settings.Cell> list = convertToListOfCellsArray(jsonArray);
        reader.close();
        consumer.accept(list);
    }

    private List<Settings.Cell> convertToListOfCellsArray(JsonArray jsonArray) {
        List<Settings.Cell> list = new ArrayList<>(jsonArray.size());
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (JsonValue jsonValue : jsonArray) {
                Settings.Cell cell = mapper.readValue(jsonValue.toString(), Settings.Cell.class);
                list.add(cell);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}
