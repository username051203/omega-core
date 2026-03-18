package com.omega.protocol.model;

public class Profiles {
    public Profile user   = new Profile();
    public Profile alicia = new Profile();

    public Profiles() {
        user.name         = "YOU";
        user.tagline      = "The Operator";
        user.photoDefault = "https://iili.io/q7kMZ6N.md.jpg";
        user.photoWin     = "https://iili.io/q7venWX.md.jpg";
        user.photoLoss    = "https://iili.io/q7v3cDQ.md.jpg";

        alicia.name         = "ALEXANDRIUS";
        alicia.tagline      = "The Rival";
        alicia.photoDefault = "https://iili.io/qR0vySS.md.jpg";
        alicia.photoWin     = "https://iili.io/qR0kSY7.md.jpg";
        alicia.photoLoss    = "https://iili.io/qR08IOF.md.jpg";
    }
}
