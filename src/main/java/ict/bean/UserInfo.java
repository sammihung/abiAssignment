package ict.bean;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private String email, userId, username, password, role;

    public UserInfo() {
    }

    public UserInfo(String username, String password, String email, String userId, String role) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.userId = userId;
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
