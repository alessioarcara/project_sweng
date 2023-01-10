package com.aadm.cardexchange.shared;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

public class CardTest implements CardTestConstants {
    private Card card;

    @BeforeEach
    public void initialize() {
        card = new CardImpl(cardName, cardDesc, cardType);
    }

    @Test
    public void testGetName() {
        Assertions.assertEquals(cardName, card.getName());
    }

    @Test
    public void testGetDescription() {
        Assertions.assertEquals(cardDesc, card.getDescription());
    }

    @Test
    public void testGetType() {
        Assertions.assertEquals(cardType, card.getType());
    }

    @Test
    public void testCardSerializable() {
        Assertions.assertTrue(card instanceof Serializable);
    }
}