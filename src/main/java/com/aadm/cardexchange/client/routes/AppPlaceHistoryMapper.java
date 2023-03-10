package com.aadm.cardexchange.client.routes;

import com.aadm.cardexchange.client.auth.AuthSubject;
import com.aadm.cardexchange.client.places.*;
import com.aadm.cardexchange.shared.models.Game;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;

public class AppPlaceHistoryMapper implements PlaceHistoryMapper, RouteConstants {
    private static final String DELIMITER = "/";
    private final Place defaultPlace = new HomePlace();
    private final AuthSubject authSubject;

    public AppPlaceHistoryMapper(AuthSubject authSubject) {
        this.authSubject = authSubject;
    }

    @Override
    public Place getPlace(String token) {
        if (token.isEmpty()) {
            return defaultPlace;
        } else if (token.equals(authLink) && !authSubject.isLoggedIn()) {
            return new AuthPlace();
        } else if (token.equals(decksLink) && authSubject.isLoggedIn()) {
            return new DecksPlace();
        } else {
            try {
                String[] parts = token.split(DELIMITER);
                if (parts[0].equals(cardsLink)) {
                    Game game = Game.valueOf(parts[1].toUpperCase());
                    int cardId = Integer.parseInt(parts[2]);
                    return new CardPlace(game, cardId);
                } else if (parts[0].equals(newExchangeLink) && authSubject.isLoggedIn()) {
                    String receiverUserEmail = parts[1];
                    String selectedCardId = parts[2];
                    return new NewExchangePlace(selectedCardId, receiverUserEmail);
                } else if (parts[0].equals(exchangesLink) && authSubject.isLoggedIn()) {
                    return new ExchangesPlace(parts[1] != null ? Integer.parseInt(parts[1]) : null);
                }
            } catch (Exception e) {
                return defaultPlace;
            }
        }
        return defaultPlace;
    }

    @Override
    public String getToken(Place place) {
        if (place instanceof HomePlace) {
            return homeLink;
        } else if (place instanceof AuthPlace) {
            return authLink;
        } else if (place instanceof DecksPlace) {
            return decksLink;
        } else if (place instanceof ExchangesPlace && ((ExchangesPlace) place).getProposalId() == null) {
            return exchangesLink;
        } else if (place instanceof ExchangesPlace) {
            ExchangesPlace exchangePlace = (ExchangesPlace) place;
            return exchangesLink + DELIMITER + exchangePlace.getProposalId();
        } else if (place instanceof NewExchangePlace) {
            NewExchangePlace newExchangePlace = (NewExchangePlace) place;
            return newExchangeLink + DELIMITER + newExchangePlace.getReceiverUserEmail() + DELIMITER + newExchangePlace.getSelectedCardId();
        } else if (place instanceof CardPlace) {
            CardPlace cardPlace = (CardPlace) place;
            return cardsLink + DELIMITER + cardPlace.getGame().name().toLowerCase() + DELIMITER + cardPlace.getCardId();
        } else {
            return "";
        }
    }
}