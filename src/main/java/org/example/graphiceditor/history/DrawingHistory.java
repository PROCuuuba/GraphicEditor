package org.example.graphiceditor.history;

import org.example.graphiceditor.model.Drawing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class DrawingHistory {
    private static final int LIMIT = 100;
    private final Drawing drawing;
    private final Deque<Change> undo = new ArrayDeque<>();
    private final Deque<Change> redo = new ArrayDeque<>();
    private Drawing.Snapshot before;
    private String label;

    public DrawingHistory(Drawing drawing) { this.drawing = Objects.requireNonNull(drawing, "drawing"); }

    public void begin(String label) {
        if (before != null) commit();
        before = drawing.snapshot();
        this.label = Objects.requireNonNull(label, "label");
    }

    public boolean commit() {
        if (before == null) return false;
        Drawing.Snapshot after = drawing.snapshot();
        Change change = new Change(label, before, after);
        before = null;
        if (change.before().equals(after)) return false;
        undo.addLast(change);
        while (undo.size() > LIMIT) undo.removeFirst();
        redo.clear();
        return true;
    }

    public void cancel() {
        if (before != null) drawing.restore(before);
        before = null;
    }

    public boolean undo() {
        commit();
        if (undo.isEmpty()) return false;
        Change change = undo.removeLast();
        drawing.restore(change.before());
        redo.addLast(change);
        return true;
    }

    public boolean redo() {
        commit();
        if (redo.isEmpty()) return false;
        Change change = redo.removeLast();
        drawing.restore(change.after());
        undo.addLast(change);
        return true;
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public void clear() { before = null; undo.clear(); redo.clear(); }

    private record Change(String label, Drawing.Snapshot before, Drawing.Snapshot after) { }
}
