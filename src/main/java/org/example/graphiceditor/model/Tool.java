package org.example.graphiceditor.model;

public enum Tool {
    POINT("Точка"), CIRCLE("Круг"), SQUARE("Квадрат");

    private final String displayName;

    Tool(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
