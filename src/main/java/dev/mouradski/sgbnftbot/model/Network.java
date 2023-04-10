package dev.mouradski.sgbnftbot.model;

public enum Network {

    FLARE(14),
    SONGBIRD(19);

    final Integer value;

    Network(Integer value) {
        this.value = value;
    }

    @SuppressWarnings("unused")
    public Integer getValue() {
        return this.value;
    }
}
