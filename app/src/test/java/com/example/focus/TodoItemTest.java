package com.example.focus;

import com.example.focus.model.TodoItem;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TodoItemTest {

    @Test
    public void testTodoItemCreationAndJsonSerialization() throws Exception {
        TodoItem item = new TodoItem("Finish autonomous project");
        Assert.assertFalse(item.isCompleted());
        Assert.assertEquals("Finish autonomous project", item.getText());

        item.setCompleted(true);
        Assert.assertTrue(item.isCompleted());

        JSONObject json = item.toJsonObject();
        TodoItem deserialized = TodoItem.fromJsonObject(json);

        Assert.assertEquals(item.getId(), deserialized.getId());
        Assert.assertEquals(item.getText(), deserialized.getText());
        Assert.assertEquals(item.isCompleted(), deserialized.isCompleted());
    }

    @Test
    public void testEisenhowerAndKaizenProductivitySerialization() throws Exception {
        TodoItem item = new TodoItem("Master Agentic Architecture");
        item.setEisenhowerQuadrant(TodoItem.QUADRANT_Q2_SCHEDULE);
        item.setKaizen(true);
        item.setPomodorosEstimated(4);
        item.incrementPomodoro();

        JSONObject json = item.toJsonObject();
        TodoItem deserialized = TodoItem.fromJsonObject(json);

        Assert.assertEquals(TodoItem.QUADRANT_Q2_SCHEDULE, deserialized.getEisenhowerQuadrant());
        Assert.assertTrue(deserialized.isKaizen());
        Assert.assertEquals(4, deserialized.getPomodorosEstimated());
        Assert.assertEquals(1, deserialized.getPomodorosCompleted());
    }
}
