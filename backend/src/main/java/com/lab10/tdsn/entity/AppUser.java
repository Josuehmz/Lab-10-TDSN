package com.lab10.tdsn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @Column(length = 256)
    private String id;

    @Column(length = 512)
    private String email;

    @Column(length = 256)
    private String name;

    @Column(length = 1024)
    private String pictureUrl;

    protected AppUser() {
    }

    public AppUser(String id, String email, String name, String pictureUrl) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void updateProfile(String email, String name, String pictureUrl) {
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
    }
}
