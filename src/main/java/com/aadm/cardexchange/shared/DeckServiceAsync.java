package com.aadm.cardexchange.shared;

import com.aadm.cardexchange.shared.models.Status;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DeckServiceAsync {
    void addDeck(String email, String deckName, AsyncCallback<Boolean> callback);

    void addPhysicalCardToDeck(String token, String deckName, int cardId, Status status, String description, AsyncCallback<Boolean> callback);
}