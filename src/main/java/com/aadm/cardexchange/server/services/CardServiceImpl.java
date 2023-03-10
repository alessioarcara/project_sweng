package com.aadm.cardexchange.server.services;

import com.aadm.cardexchange.server.gsonserializer.GsonSerializer;
import com.aadm.cardexchange.server.mapdb.MapDB;
import com.aadm.cardexchange.server.mapdb.MapDBConstants;
import com.aadm.cardexchange.server.mapdb.MapDBImpl;
import com.aadm.cardexchange.shared.CardService;
import com.aadm.cardexchange.shared.exceptions.InputException;
import com.aadm.cardexchange.shared.models.Card;
import com.aadm.cardexchange.shared.models.Game;
import com.google.gson.Gson;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.mapdb.Serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CardServiceImpl extends RemoteServiceServlet implements CardService, MapDBConstants {
    private static final long serialVersionUID = -45618221088536472L;
    private final MapDB db;
    private final GsonSerializer<Card> serializer = new GsonSerializer<>(new Gson());

    public CardServiceImpl() {
        db = new MapDBImpl();
    }

    public CardServiceImpl(MapDB db) {
        this.db = db;
    }

    public static String getCardMap(Game game) {
        return game == Game.MAGIC ? MAGIC_MAP_NAME :
                game == Game.POKEMON ? POKEMON_MAP_NAME :
                        YUGIOH_MAP_NAME;
    }

    public static String getNameCard(int idCard, Map<Integer, Card> cardMap) {
        try {
            return cardMap.get(idCard).getName();
        } catch (NullPointerException e) {
            return "No Name Found";
        }
    }

    public static void checkGameValidity(Game game) throws InputException {
        if (game == null)
            throw new InputException("game cannot be null");
    }

    public static void checkCardIdValidity(int cardId) throws InputException {
        if (cardId <= 0) {
            throw new InputException("Invalid cardId provided. cardId must be a positive integer greater than 0");
        }
    }

    @Override
    public List<Card> getGameCards(Game game) throws InputException {
        checkGameValidity(game);
        Map<Integer, Card> map = db.getCachedMap(getServletContext(), getCardMap(game),
                Serializer.INTEGER, serializer);
        return new ArrayList<>(map.values());
    }

    @Override
    public Card getGameCard(Game game, int cardId) throws InputException {
        checkGameValidity(game);
        checkCardIdValidity(cardId);
        Map<Integer, Card> map = db.getCachedMap(getServletContext(), getCardMap(game),
                Serializer.INTEGER, serializer);
        return map.get(cardId);
    }
}
