package com.pfe.gestionpfe.imports;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImportRow {

    private Map<String, String> values = new LinkedHashMap<>();
    private boolean valid = true;
    private String errorMessage = "";

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void put(String key, String value) {
        values.put(key, value == null ? "" : value.trim());
    }

    public String get(String key) {
        return values.getOrDefault(key, "");
    }
}