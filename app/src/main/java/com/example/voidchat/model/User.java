
package com.example.voidchat.model;
public class User {
    private String uid;
    private String email;
    private String displayName;
    private String profileImageUrl;

    // Constructor vacío requerido por Firebase para la deserialización de datos
    public User() {}
    public User(String uid, String email, String displayName, String profileImageUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
    }
    // Getters para acceder a las propiedades del usuario
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // Setters para modificar las propiedades del usuario
    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
