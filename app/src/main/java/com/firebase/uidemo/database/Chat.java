package com.firebase.uidemo.database;

public class Chat {
    private String name;
    private String text;
    private String uid;

    public Chat() {
    }

    public Chat(String name, String uid, String message) {
        this.name = name;
        text = message;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public String getText() {
        return text;
    }
}