package org.ballerinalang.model.values;

/**
 * Captures the position of a character
 */
public class Position {

    private int lineNumber = -1;
    private int column = -1;

    public Position() {

    }

    public Position(int lineNumber, int column) {
        this.lineNumber = lineNumber;
        this.column = column;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
