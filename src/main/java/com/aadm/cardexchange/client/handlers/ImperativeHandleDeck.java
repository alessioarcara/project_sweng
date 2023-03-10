package com.aadm.cardexchange.client.handlers;

import com.aadm.cardexchange.shared.models.PhysicalCard;
import com.aadm.cardexchange.shared.models.PhysicalCardWithName;

import java.util.List;
import java.util.function.BiConsumer;

public interface ImperativeHandleDeck {
    void onShowDeck(String deckName, BiConsumer<List<PhysicalCardWithName>, String> setDeckData);

    void onRemovePhysicalCard(String deckName, PhysicalCard pCard);
}
