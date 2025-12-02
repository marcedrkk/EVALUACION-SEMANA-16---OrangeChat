// RUTA: com.example.voidchat.model/Message.java
package com.example.voidchat.model;
public class Message {

    private String userId;
    private String username;
    private String text;
    private long time;
    private boolean isSeparator = false;
    private String separatorLabel;

    // Constructor vacío requerido por Firebase
    public Message() {}

    // Constructor para mensajes normales
    public Message(String userId, String username, String text, long time) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.time = time;
    }

    // Método de fábrica estático para crear separadores de fecha de forma limpia
    public static Message createSeparator(String label) {
        Message separator = new Message();
        separator.isSeparator = true;
        separator.separatorLabel = label;
        return separator;
    }
    // Getters para acceder a las propiedades del mensaje
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getText() { return text; }
    public long getTime() { return time; }
    public boolean isSeparator() { return isSeparator; }
    public String getSeparatorLabel() { return separatorLabel; }
}
