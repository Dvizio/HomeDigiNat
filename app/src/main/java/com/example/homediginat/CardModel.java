package com.example.homediginat;

public class CardModel {
    private String name;
    private int image;
    private String card2File;

    public CardModel(String name, int image, String card2File) {
        this.name = name;
        this.image = image;
        this.card2File = card2File;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }
    public String getCard2File() {
        return card2File;
    }

    public void setCard2File(String card2File) {
        this.card2File = card2File;
    }
}