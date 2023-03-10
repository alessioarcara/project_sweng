package com.aadm.cardexchange.server.services;

import com.aadm.cardexchange.server.gsonserializer.GsonSerializer;
import com.aadm.cardexchange.server.mapdb.MapDB;
import com.aadm.cardexchange.server.mapdb.MapDBConstants;
import com.aadm.cardexchange.server.mapdb.MapDBImpl;
import com.aadm.cardexchange.shared.DefaultDecksConstants;
import com.aadm.cardexchange.shared.ExchangeService;
import com.aadm.cardexchange.shared.exceptions.AuthException;
import com.aadm.cardexchange.shared.exceptions.InputException;
import com.aadm.cardexchange.shared.exceptions.ProposalNotFoundException;
import com.aadm.cardexchange.shared.models.*;
import com.aadm.cardexchange.shared.payloads.ProposalPayload;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.mapdb.Serializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ExchangeServiceImpl extends RemoteServiceServlet implements ExchangeService, DefaultDecksConstants, MapDBConstants {
    private static final long serialVersionUID = 5868088467963819042L;
    private final MapDB db;
    private final Gson gson = new Gson();

    private final Type type = new TypeToken<Map<String, Deck>>() {
    }.getType();

    public ExchangeServiceImpl() {
        db = new MapDBImpl();
    }

    public ExchangeServiceImpl(MapDB mockDB) {
        db = mockDB;
    }

    private boolean checkEmailExistence(String email) {
        Map<String, User> userMap = db.getPersistentMap(
                getServletContext(), USER_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson));
        return userMap.get(email) != null;
    }

    private boolean checkPhysicalCardsConsistency(List<PhysicalCard> physicalCards) {
        return physicalCards != null && !physicalCards.isEmpty();
    }


    @Override
    public boolean addProposal(String token, String receiverUserEmail, List<PhysicalCard> senderPhysicalCards, List<PhysicalCard> receiverPhysicalCards) throws AuthException, InputException {
        String email = AuthServiceImpl.checkTokenValidity(token,
                db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        if (receiverUserEmail == null || receiverUserEmail.isEmpty() || !checkEmailExistence(receiverUserEmail))
            throw new InputException("Invalid receiver email");
        if (email.equals(receiverUserEmail))
            throw new InputException("You cannot propose an exchange with yourself.");
        if (!checkPhysicalCardsConsistency(senderPhysicalCards))
            throw new InputException("Invalid sender physical cards");
        if (!checkPhysicalCardsConsistency(receiverPhysicalCards))
            throw new InputException("Invalid receiver physical cards");

        Proposal newProposal = new Proposal(email, receiverUserEmail, senderPhysicalCards, receiverPhysicalCards);
        return db.writeOperation(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson),
                (Map<Integer, Proposal> proposalMap) -> proposalMap.putIfAbsent(newProposal.getId(), newProposal) == null);
    }

    private List<PhysicalCardWithName> joinPhysicalCardsWithCatalogCards(List<PhysicalCard> pCards) {
        List<PhysicalCardWithName> pCardsWithName = new LinkedList<>();
        for (PhysicalCard pCard : pCards) {
            pCardsWithName.add(new PhysicalCardWithName(pCard, CardServiceImpl.getNameCard(
                    pCard.getCardId(),
                    db.getCachedMap(
                            getServletContext(),
                            CardServiceImpl.getCardMap(pCard.getGameType()),
                            Serializer.INTEGER,
                            new GsonSerializer<>(gson)
                    )
            )));
        }
        return pCardsWithName;
    }

    @Override
    public ProposalPayload getProposal(String token, int proposalId) throws AuthException, InputException, ProposalNotFoundException {
        String email = AuthServiceImpl.checkTokenValidity(token, db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        if (proposalId < 0)
            throw new InputException("Invalid proposal Id");
        Map<Integer, Proposal> proposalMap = db.getPersistentMap(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson));
        Proposal proposal = proposalMap.get(proposalId);
        if (proposal == null)
            throw new ProposalNotFoundException("Not existing proposal");
        if (!email.equals(proposal.getSenderUserEmail()) && !email.equals(proposal.getReceiverUserEmail()))
            throw new AuthException("You can only view proposals linked to your account as sender or receiver");

        return new ProposalPayload(proposal.getSenderUserEmail(), proposal.getReceiverUserEmail(),
                joinPhysicalCardsWithCatalogCards(proposal.getSenderPhysicalCards()),
                joinPhysicalCardsWithCatalogCards(proposal.getReceiverPhysicalCards())
        );
    }

    private void deleteReferredProposals(Map<Integer, Proposal> proposalMap, List<PhysicalCard> receiver_cards, List<PhysicalCard> sender_cards) {
        List<PhysicalCard> allCards = Stream.concat(receiver_cards.stream(), sender_cards.stream()).collect(Collectors.toList());
        Map<String, PhysicalCard> lookUp = allCards.stream().collect(Collectors.toMap(PhysicalCard::getId, Function.identity()));

        proposalMap.values().removeIf(proposal ->
                proposal.getSenderPhysicalCards().stream().anyMatch(pCard -> lookUp.containsKey(pCard.getId())) ||
                        proposal.getReceiverPhysicalCards().stream().anyMatch(pCard -> lookUp.containsKey(pCard.getId()))
        );
    }

    @Override
    public boolean acceptProposal(String token, int proposalId) throws AuthException, ProposalNotFoundException {
        String email = AuthServiceImpl.checkTokenValidity(token,
                db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        Map<Integer, Proposal> proposalMap = db.getPersistentMap(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson));
        Proposal proposal = proposalMap.get(proposalId);
        if (proposal == null)
            throw new ProposalNotFoundException("Not existing proposal");
        if (!email.equals(proposal.getReceiverUserEmail()))
            throw new AuthException("You can only accept proposals made to you.");

        return db.writeOperation(getServletContext(), DECK_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson, type),
                (Map<String, Map<String, Deck>> deckMap) -> {
                    deckMap.put(proposal.getReceiverUserEmail(), DeckServiceImpl.updateUserDecks(deckMap.get(proposal.getReceiverUserEmail()), proposal.getSenderPhysicalCards(), proposal.getReceiverPhysicalCards()));
                    deckMap.put(proposal.getSenderUserEmail(), DeckServiceImpl.updateUserDecks(deckMap.get(proposal.getSenderUserEmail()), proposal.getReceiverPhysicalCards(), proposal.getSenderPhysicalCards()));
                    deleteReferredProposals(proposalMap, proposal.getSenderPhysicalCards(), proposal.getReceiverPhysicalCards());
                    return true;
                }) != null;
    }

    @Override
    public boolean refuseOrWithdrawProposal(String token, int proposalId) throws AuthException, InputException, NullPointerException, ProposalNotFoundException {
        String email = AuthServiceImpl.checkTokenValidity(token, db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        if (proposalId < 0)
            throw new InputException("Invalid proposal Id");
        Map<Integer, Proposal> proposalMap = db.getPersistentMap(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson));
        Proposal proposal = proposalMap.get(proposalId);
        if (proposal == null)
            throw new ProposalNotFoundException("Not existing proposal");
        if (!email.equals(proposal.getSenderUserEmail()) && !email.equals(proposal.getReceiverUserEmail()))
            throw new AuthException("You can only interact with proposals of which you are the sender or the receiver");
        return db.writeOperation(getServletContext(), () -> proposalMap.remove(proposalId, proposal));
    }

    public List<Proposal> getProposalListReceived(String token) throws AuthException {
        String email = AuthServiceImpl.checkTokenValidity(token,
                db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        Map<Integer, Proposal> proposalMap = db.getPersistentMap(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson));
        List<Proposal> proposalList = new ArrayList<>();
        for (Proposal item : proposalMap.values())
            if (email.equals(item.getReceiverUserEmail()))
                proposalList.add(item);
        return proposalList;
    }

    public List<Proposal> getProposalListSent(String token) throws AuthException {
        String email = AuthServiceImpl.checkTokenValidity(token,
                db.getPersistentMap(getServletContext(), LOGIN_MAP_NAME, Serializer.STRING, new GsonSerializer<>(gson)));
        Map<Integer, Proposal> proposalMap = db.getPersistentMap(getServletContext(), PROPOSAL_MAP_NAME, Serializer.INTEGER, new GsonSerializer<>(gson));
        List<Proposal> proposalList = new ArrayList<>();
        for (Proposal item : proposalMap.values())
            if (email.equals(item.getSenderUserEmail()))
                proposalList.add(item);
        return proposalList;
    }
}
