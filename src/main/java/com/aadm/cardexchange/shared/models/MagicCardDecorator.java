package com.aadm.cardexchange.shared.models;


import java.util.Objects;

public class MagicCardDecorator extends CardDecorator {
    private static final long serialVersionUID = 2368581922806822154L;
    private String artist;
    private String rarity;
    private boolean hasFoil;
    private boolean isAlternative;
    private boolean isFullArt;
    private boolean isPromo;
    private boolean isReprint;


    public MagicCardDecorator(CardImpl card, String artist, String rarity, boolean hasFoil, boolean isAlternative, boolean isFullArt, boolean isPromo, boolean isReprint) {
        super(card);
        this.artist = artist;
        this.rarity = rarity;
        this.hasFoil = hasFoil;
        this.isAlternative = isAlternative;
        this.isFullArt = isFullArt;
        this.isPromo = isPromo;
        this.isReprint = isReprint;
    }

    public MagicCardDecorator() {
    }

    public String getArtist() {
        return artist;
    }

    public String getRarity() {
        return rarity;
    }

    public boolean getHasFoil() {
        return hasFoil;
    }

    public boolean getIsAlternative() {
        return isAlternative;
    }

    public boolean getIsFullArt() {
        return isFullArt;
    }

    public boolean getIsPromo() {
        return isPromo;
    }

    public boolean getIsReprint() {
        return isReprint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MagicCardDecorator)) return false;
        if (!super.equals(o)) return false;
        MagicCardDecorator that = (MagicCardDecorator) o;
        return getHasFoil() == that.getHasFoil() && isAlternative == that.isAlternative && isFullArt == that.isFullArt && isPromo == that.isPromo && isReprint == that.isReprint && getArtist().equals(that.getArtist()) && getRarity().equals(that.getRarity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getArtist(), getRarity(), getHasFoil(), isAlternative, isFullArt, isPromo, isReprint);
    }
}
