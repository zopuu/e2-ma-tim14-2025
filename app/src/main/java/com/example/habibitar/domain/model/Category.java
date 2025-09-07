package com.example.habibitar.domain.model;

public class Category {
    public String id;
    public String name;
    public String colorCode;
    private String ownerId;

    public Category() {}

    public Category(String id, String ownerId, String name, String colorCode)
    {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.colorCode = colorCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

}
