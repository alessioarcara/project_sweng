package com.aadm.cardexchange.server;

import com.aadm.cardexchange.server.mapdb.MapDB;
import com.aadm.cardexchange.server.services.DeckServiceImpl;
import com.aadm.cardexchange.shared.exceptions.*;
import com.aadm.cardexchange.shared.models.*;
import com.aadm.cardexchange.shared.payloads.ModifiedDeckPayload;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapdb.Serializer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;

import static com.aadm.cardexchange.server.DummyData.createMagicDummyMap;
import static org.easymock.EasyMock.*;

public class DeckServiceTest {
    private IMocksControl ctrl;
    private MapDB mockDB;
    ServletConfig mockConfig;
    ServletContext mockCtx;
    private DeckServiceImpl deckService;

    @BeforeEach
    public void initialize() throws ServletException {
        ctrl = createStrictControl();
        mockDB = ctrl.createMock(MapDB.class);
        deckService = new DeckServiceImpl(mockDB);
        mockConfig = ctrl.createMock(ServletConfig.class);
        mockCtx = ctrl.createMock(ServletContext.class);
        deckService.init(mockConfig);
    }

    @Test
    public void testIfDeckAlreadyExist() {
        Map<String, Map<String, Deck>> deckMap = new HashMap<>();
        Map<String, Deck> mockDecks = new HashMap<>() {{
            put("Owned", new Deck("Owned"));
            put("Wished", new Deck("Wished"));
        }};
        deckMap.put("test@test.it", mockDecks);
        ctrl.replay();
        Assertions.assertFalse(DeckServiceImpl.createDefaultDecks("test@test.it", deckMap));
        ctrl.verify();
    }

    @Test
    public void testDefaultDeckCreation() {
        Map<String, Map<String, Deck>> deckMap = new HashMap<>();
        ctrl.replay();
        Assertions.assertTrue(DeckServiceImpl.createDefaultDecks("testbis@test.it", deckMap));
        ctrl.verify();
        // check if default decks "Owned", "Wished" are present
        Assertions.assertAll(() -> {
            Assertions.assertNotNull(deckMap.get("testbis@test.it").get("Owned"));
            Assertions.assertNotNull(deckMap.get("testbis@test.it").get("Wished"));
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testAddPhysicalCardToDeckForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () ->
                deckService.addPhysicalCardToDeck(input, Game.MAGIC, "Owned", 111, Status.Damaged, "This is a valid description.")
        );
        ctrl.verify();
    }

    private void setupForValidToken() {
        Map<String, LoginInfo> mockLoginMap = new HashMap<>() {{
            put("validToken", new LoginInfo("test@test.it", System.currentTimeMillis() - 10000));
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockLoginMap);
    }

    private void setupForInvalidToken() {
        Map<String, LoginInfo> mockLoginMap = new HashMap<>() {{
            put("validToken1", new LoginInfo("test@test1.it", System.currentTimeMillis() - 10000));
            put("validToken2", new LoginInfo("test@test2.it", System.currentTimeMillis() - 20000));
            put("validToken3", new LoginInfo("test@test3.it", System.currentTimeMillis() - 30000));
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockLoginMap);
    }

    @Test
    public void testAddPhysicalCardToDeckForInvalidGame() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> {
            deckService.addPhysicalCardToDeck("validToken", null, "Owned", 111, Status.Excellent, "This is a valid description.");
        });
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testAddPhysicalCardToDeckForInvalidDeckName(String input) {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () ->
                deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, input, 111, Status.Excellent, "This is a valid description.")
        );
        ctrl.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    public void testAddPhysicalCardToDeckForInvalidCardId(int input) {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () ->
                deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", input, Status.Excellent, "This is a valid description.")
        );
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardToDeckForInvalidStatus() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () ->
                deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", 111, null, "This is a valid description.")
        );
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not valid descr", "1234567890123456789", "1 3 5 7 9 1 3 5 7 9 "})
    public void testAddPhysicalCardToDeckForInvalidDescription(String input) {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () ->
                deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", 111, Status.Excellent, input)
        );
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardToDeckForNotExistingDeckMap() {
        setupForValidToken();
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", null);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertThrows(RuntimeException.class, () ->
                deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", 111, Status.Excellent, "This is a valid description.")
        );
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardToDeckForNotExistingDeck() throws BaseException {
        setupForValidToken();
        Map<String, Deck> deckMap = new HashMap<>();
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", deckMap);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertFalse(deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", 111, Status.Excellent, "This is a valid description."));
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardToDeckForCardAddition() throws BaseException {
        setupForValidToken();
        Map<String, Deck> deckMap = new HashMap<>() {{
            put("Owned", new Deck("Owned", true));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", deckMap);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertTrue(deckService.addPhysicalCardToDeck("validToken", Game.MAGIC, "Owned", 111, Status.Excellent, "This is a valid description."));
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testAddDeckForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.addDeck(input, "testDeckName"));
        ctrl.verify();
    }

    @Test
    public void testAddDeckForAlreadyExistingDeck() throws AuthException {
        setupForValidToken();
        String userEmail = "test@test.it";
        String deckName = "testDeckName";
        Map<String, Deck> decks = new HashMap<>();
        decks.put(deckName, new Deck(deckName, false));
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put(userEmail, decks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertFalse(deckService.addDeck("validToken", deckName));
        ctrl.verify();
    }

    @Test
    public void testAddDeckForNotAlreadyExistingDeck() throws AuthException {
        setupForValidToken();
        String customDeckName = "testDeckName";
        Map<String, Deck> decks = new HashMap<>() {{
            put("Owned", new Deck("Owned", true));
            put("Wished", new Deck("Wished", true));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", decks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertTrue(deckService.addDeck("validToken", customDeckName));
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testGetUserDeckNamesForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.getUserDeckNames(input));
        ctrl.verify();
    }

    @Test
    public void testGetUserDeckNamesForNotExistingDeckMap() throws AuthException {
        setupForValidToken();
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", null);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertTrue(deckService.getUserDeckNames("validToken").isEmpty());
        ctrl.verify();
    }

    @Test
    public void testGetUserDeckNamesForReturnCorrectDeckNames() throws AuthException {
        List<String> mockDeckNames = Arrays.asList("Owned", "Wished", "Test");
        setupForValidToken();
        Map<String, Deck> decks = new HashMap<>() {{
            put("Owned", new Deck("Owned", true));
            put("Wished", new Deck("Wished", true));
            put("Test", new Deck("Test", false));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", decks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        List<String> deckNames = deckService.getUserDeckNames("validToken");
        ctrl.verify();
        Assertions.assertAll(() -> {
            Assertions.assertEquals(mockDeckNames.size(), deckNames.size());
            Assertions.assertIterableEquals(mockDeckNames, deckNames);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testGetDeckByNameForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.getMyDeck(input, "Owned"));
        ctrl.verify();
    }

    @Test
    public void testGetDeckByNameForNotExistingDeck() {
        setupForValidToken();
        Map<String, Deck> deckMap = new HashMap<>();
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", deckMap);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertThrows(NullPointerException.class, () -> deckService.getMyDeck("validToken", "Owned"));
        ctrl.verify();
    }

    private void setupForValidCardsAndUserDecks() {
        // init Mocks
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");
        PhysicalCard mockPCard2 = new PhysicalCard(Game.MAGIC, 2222, Status.Fair, "This is a valid bis description.");
        Card mockCard1 = DummyData.createPokemonDummyCard();
        Card mockCard2 = DummyData.createYuGiOhDummyCard();
        Map<Integer, Card> cardMap = new HashMap<>() {{
            put(1111, mockCard1);
            put(2222, mockCard2);
        }};
        Map<String, Deck> userDecks = new HashMap<>() {{
            put("Owned", new Deck("Owned", true));
        }};
        userDecks.get("Owned").addPhysicalCard(mockPCard1);
        userDecks.get("Owned").addPhysicalCard(mockPCard2);
        Map<String, Map<String, Deck>> deckMap = new HashMap<>() {{
            put("test@test.it", userDecks);
        }};

        // what I expect
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(deckMap);
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getCachedMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(cardMap);
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getCachedMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(cardMap);
    }

    private void setupDefaultDecks(String deckName, String mail1, String mail2) {
        PhysicalCard mockPCard1 = new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is the card that I want.");
        PhysicalCard mockPCard2 = new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is the card that I want.");
        PhysicalCard mockPCard3 = new PhysicalCard(Game.randomGame(), 2222, Status.randomStatus(), "This is the card that I want.");
        PhysicalCard mockPCard4 = new PhysicalCard(Game.randomGame(), 3333, Status.randomStatus(), "This is the card that I want.");
        Deck mockDeck1 = new Deck(deckName, true);
        Deck mockDeck2 = new Deck(deckName, true);

        mockDeck1.addPhysicalCard(mockPCard1);
        mockDeck1.addPhysicalCard(mockPCard4);
        mockDeck2.addPhysicalCard(mockPCard3);
        mockDeck2.addPhysicalCard(mockPCard2);
        Map<String, Map<String, Deck>> deckMap = new LinkedHashMap<>() {{
            put(mail1, new HashMap<>() {{
                put(deckName, mockDeck1);
            }});
            put(mail2, new HashMap<>() {{
                put(deckName, mockDeck2);
            }});
        }};

        // expects
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class))).andReturn(deckMap);
    }

    @Test
    public void testGetDeckByNameForValidEntry() throws AuthException {
        setupForValidToken();
        setupForValidCardsAndUserDecks();
        ctrl.replay();
        List<PhysicalCardWithName> decoratedCards = deckService.getMyDeck("validToken", "Owned");
        ctrl.verify();
        Assertions.assertAll(() -> {
            Assertions.assertEquals(2, decoratedCards.size());
            Assertions.assertEquals("Charizard", decoratedCards.get(0).getName());
            Assertions.assertEquals("Exodia the Forbidden One", decoratedCards.get(1).getName());
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"test", "test@", "test@test", "test@test."})
    public void testGetUserOwnedDeckForInvalidEmail(String input) {
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.getUserOwnedDeck(input));
        ctrl.verify();
    }

    @Test
    public void testGetUserOwnedDeckForValidEmail() throws AuthException {
        setupForValidCardsAndUserDecks();
        ctrl.replay();
        List<PhysicalCardWithName> decoratedCards = deckService.getUserOwnedDeck("test@test.it");
        ctrl.verify();
        Assertions.assertAll(() -> {
            Assertions.assertEquals(2, decoratedCards.size());
            Assertions.assertEquals("Charizard", decoratedCards.get(0).getName());
            Assertions.assertEquals("Exodia the Forbidden One", decoratedCards.get(1).getName());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    public void testGetOwnedPhysicalCardsByCardIdForInvalidId(int input) {
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.getOwnedPhysicalCardsByCardId(input));
        ctrl.verify();
    }

    @Test
    public void testGetOwnedPhysicalCardsByCardIdForValidId() throws InputException {
        // init Mocks
        String emailTest1 = "test1@test.it";
        String emailTest2 = "test2@test.it";
        setupDefaultDecks("Owned", emailTest1, emailTest2);
        ctrl.replay();
        List<PhysicalCardWithEmail> pCardsWithEmail = deckService.getOwnedPhysicalCardsByCardId(1111);
        ctrl.verify();
        Assertions.assertAll(() -> {
            Assertions.assertEquals(2, pCardsWithEmail.size());
            Assertions.assertEquals(emailTest1, pCardsWithEmail.get(0).getEmail());
            Assertions.assertEquals(emailTest2, pCardsWithEmail.get(1).getEmail());
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRemovePhysicalCardFromDeckForInvalidDeckName(String input) {
        setupForValidToken();
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () ->
                deckService.removePhysicalCardFromDeck("validToken", input, mockPCard1)
        );
        ctrl.verify();
    }

    @Test
    public void testRemovePhysicalCardFromDeckForNotExistingDeckMap() {
        setupForValidToken();
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");

        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", null);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertThrows(RuntimeException.class, () ->
                deckService.removePhysicalCardFromDeck("validToken", "Owned", mockPCard1)
        );
        ctrl.verify();
    }

    @Test
    public void testRemovePhysicalCardFromDeckForNotExistingDeck() throws AuthException, InputException, DeckNotFoundException {
        setupForValidToken();
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");
        Map<String, Deck> deckMap = new HashMap<>() {{
            put("Wished", new Deck("Wished", true));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", deckMap);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertThrows(DeckNotFoundException.class, () -> deckService.removePhysicalCardFromDeck("validToken", "Owned", mockPCard1));
        ctrl.verify();
    }

    @Test
    public void testRemovePhysicalCardFromDeckForNotExistingPhysicalCard() throws AuthException, InputException, DeckNotFoundException {
        setupForValidToken();
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");
        PhysicalCard mockPCard2 = new PhysicalCard(Game.MAGIC, 1221, Status.Fair, "This is a valid bis description.");
        Map<String, Deck> deckMap = new HashMap<>() {{
            put("Owned", new Deck("Owned", true));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", deckMap);
        }};
        deckMap.get("Owned").addPhysicalCard(mockPCard2);

        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        //.remove method of Set.java return false is object to remove is not present
        Assertions.assertEquals(Collections.emptyList(), deckService.removePhysicalCardFromDeck("validToken", "Owned", mockPCard1));
        ctrl.verify();
    }

    @Test
    public void testRemovePhysicalCardFromDeckSuccess() throws AuthException, InputException, DeckNotFoundException {
        setupForValidToken();
        final int numOfCards = 4;

        // init mocks
        PhysicalCard targetPCard = new PhysicalCard(Game.MAGIC, 1111, Status.Excellent, "This is a valid description.");

        List<Deck> decks = Arrays.asList(
                new Deck("Owned", true),
                new Deck("Wished", true),
                new Deck("custom1"),
                new Deck("custom2"),
                new Deck("custom3")
        );

        Map<String, Deck> mockUserDeckMap = new HashMap<>();

        // load physical cards on existing decks
        for (Deck deck : decks) {
            for (PhysicalCard pCard : DummyData.createPhysicalCardDummyList(numOfCards)) {
                deck.addPhysicalCard(pCard);
            }
            if (!deck.getName().equals("Wished") && !deck.getName().equals("custom2")) {
                deck.addPhysicalCard(targetPCard);
            }
            mockUserDeckMap.put(deck.getName(), deck);
        }

        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put("test@test.it", mockUserDeckMap);
        }};

        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);

        for (int i = 0; i < numOfCards * 3; i++) {
            expect(mockConfig.getServletContext()).andReturn(mockCtx);
            expect(mockDB.getCachedMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                    .andReturn(new HashMap<>());
        }

        ctrl.replay();
        Assertions.assertEquals(3, deckService.removePhysicalCardFromDeck("validToken", "Owned", targetPCard).size());
        ctrl.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    public void testGetWishedPhysicalCardsByCardIdForInvalidId(int input) {
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.getWishedPhysicalCardsByCardId(input));
        ctrl.verify();
    }

    @Test
    public void testGetWishedPhysicalCardsByCardIdForValidId() throws InputException {
        // init mocks
        String emailTest1 = "test1@test.it";
        String emailTest2 = "test2@test.it";
        setupDefaultDecks("Wished", emailTest1, emailTest2);

        ctrl.replay();
        List<PhysicalCardWithEmail> pCardsWithEmail = deckService.getWishedPhysicalCardsByCardId(1111);
        ctrl.verify();

        Assertions.assertAll(() -> {
            Assertions.assertEquals(emailTest1, pCardsWithEmail.get(0).getEmail());
            Assertions.assertEquals(emailTest2, pCardsWithEmail.get(1).getEmail());
            Assertions.assertEquals(2, pCardsWithEmail.size());
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testRemoveCustomDeckForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.removeCustomDeck(input, "testDeckName"));
        ctrl.verify();
    }

    @Test
    public void testRemoveCustomDeckForNotExistingDeck() throws AuthException {
        setupForValidToken();
        String email = "test@test.it";
        String deckName = "testDeckName";
        Map<String, Deck> userDecks = new HashMap<>();
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put(email, userDecks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertFalse(deckService.removeCustomDeck("validToken", deckName));
        ctrl.verify();
    }

    @Test
    public void testRemoveCustomDeckForDefaultDeck() throws AuthException {
        setupForValidToken();
        String email = "test@test.it";
        String deckName = "testDeckName";
        Map<String, Deck> userDecks = new HashMap<>() {{
            put(deckName, new Deck(deckName, true));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put(email, userDecks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertFalse(deckService.removeCustomDeck("validToken", deckName));
        ctrl.verify();
    }

    @Test
    public void testRemoveCustomDeckForRemovableCustomDeck() throws AuthException {
        setupForValidToken();
        String email = "test@test.it";
        String deckName = "testDeckName";
        Map<String, Deck> userDecks = new HashMap<>() {{
            put(deckName, new Deck(deckName));
        }};
        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
            put(email, userDecks);
        }};
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(mockDeckMap);
        ctrl.replay();
        Assertions.assertTrue(deckService.removeCustomDeck("validToken", deckName));
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testAddPhysicalCardsToCustomDeckForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.addPhysicalCardsToCustomDeck(input, "test",
                Arrays.asList(
                        new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 2222, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 3333, Status.randomStatus(), "This is a valid description.")
                )));
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Owned", "Wished"})
    public void testAddPhysicalCardsToCustomDeckForInvalidDeckName(String input) {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.addPhysicalCardsToCustomDeck("validToken", input,
                Arrays.asList(
                        new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 2222, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 3333, Status.randomStatus(), "This is a valid description.")
                )));
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardsToCustomDeckForEmptyList() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.addPhysicalCardsToCustomDeck("validToken",
                "test", Collections.emptyList()));
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardsToCustomDeckForCustomDeckNotFound() {
        setupForValidToken();
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(new HashMap<>() {{
                    put("test@test.it", new HashMap<>() {{
                        put("Owned", new Deck("Owned", true));
                        put("Wished", new Deck("Wished", true));
                    }});
                }});

        ctrl.replay();
        Assertions.assertThrows(DeckNotFoundException.class, () -> deckService.addPhysicalCardsToCustomDeck("validToken", "test",
                Arrays.asList(
                        new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 2222, Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.randomGame(), 3333, Status.randomStatus(), "This is a valid description.")
                )));
        ctrl.verify();
    }

    @Test
    public void testAddPhysicalCardsToCustomDeckForValidParameters() throws InputException, AuthException, DeckNotFoundException {
        // init mocks
        Map<Integer, MagicCard> cardMap = createMagicDummyMap();
        List<Integer> indexCardId = new ArrayList<>(cardMap.keySet());
        PhysicalCard mockPCard1 = new PhysicalCard(Game.MAGIC, indexCardId.get(0), Status.randomStatus(), "This is a valid description.");
        PhysicalCard mockPCard2 = new PhysicalCard(Game.MAGIC, indexCardId.get(1), Status.randomStatus(), "This is a valid description.");
        Deck testDeck = new Deck("test", false);
        testDeck.addPhysicalCard(mockPCard1);
        testDeck.addPhysicalCard(mockPCard2);

        // expects
        setupForValidToken();
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(new HashMap<>() {{
                    put("test@test.it", new HashMap<>() {{
                        put("Owned", new Deck("Owned", true));
                        put("Wished", new Deck("Wished", true));
                        put("test", testDeck);
                    }});
                }});

        for (int i = 0; i < 5; i++) {
            expect(mockConfig.getServletContext()).andReturn(mockCtx);
            expect(mockDB.getCachedMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                    .andReturn(cardMap);
        }

        ctrl.replay();
        List<PhysicalCardWithName> pCardsWithNames = deckService.addPhysicalCardsToCustomDeck("validToken", "test",
                Arrays.asList(
                        new PhysicalCard(Game.MAGIC, indexCardId.get(2), Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.MAGIC, indexCardId.get(3), Status.randomStatus(), "This is a valid description."),
                        new PhysicalCard(Game.MAGIC, indexCardId.get(4), Status.randomStatus(), "This is a valid description."),
                        // duplicate cards
                        mockPCard1,
                        mockPCard2
                ));

        ctrl.verify();
        Assertions.assertAll(() -> {
            Assertions.assertEquals(5, pCardsWithNames.size());
            Assertions.assertEquals("Lightning Bolt", pCardsWithNames.get(0).getName());
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testEditPhysicalCardForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.editPhysicalCard(input,
                "Owned", new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description.")));
        ctrl.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"custom"})
    public void testEditPhysicalCardForInvalidDeckName(String input) {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.editPhysicalCard("validToken",
                input, new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description.")));
        ctrl.verify();
    }

    @Test
    public void testEditPhysicalCardForInvalidPhysicalCard() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> deckService.editPhysicalCard("validToken", "Owned", null));
        ctrl.verify();
    }

    @Test
    public void testEditPhysicalCardForExistingProposal() {
        // init mocks
        PhysicalCard mockPCard = new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description.");
        List<PhysicalCard> testList = DummyData.createPhysicalCardDummyList(5);
        testList.add(mockPCard);
        Proposal mockProposal1 = new Proposal("test2@test.it", "test2@test.it",
                testList, DummyData.createPhysicalCardDummyList(5));
        Proposal mockProposal2 = new Proposal("test3@test.it", "test4@test.it",
                DummyData.createPhysicalCardDummyList(5), DummyData.createPhysicalCardDummyList(5));

        // expects
        setupForValidToken();
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(new HashMap<>() {{
                    put(0, mockProposal1);
                    put(1, mockProposal2);
                }});
        ctrl.replay();
        Assertions.assertThrows(ExistingProposal.class, () -> deckService.editPhysicalCard("validToken",
                "Owned", mockPCard));
        ctrl.verify();
    }

    @Test
    public void testEditPhysicalCardForValidParameters() throws ExistingProposal, InputException, AuthException {
        // init mocks
        PhysicalCard mockPCard = new PhysicalCard(Game.randomGame(), 1111, Status.randomStatus(), "This is a valid description.");
        PhysicalCard editedPCard = mockPCard.copyWithModifiedStatusAndDescription(Status.randomStatus(), "This is a modified description");
        List<Deck> decks = Arrays.asList(
                new Deck("Owned", true),
                new Deck("Wished", true),
                new Deck("test1", false),
                new Deck("test2", false),
                new Deck("test3", false)
        );

        // modify random number of decks
        Random r = new Random(42);
        int numOfCards = 4;
        int numOfDecksModified = 0;

        for (Deck deck : decks) {
            // load deck with dummy physical cards (#numOfCards)
            for (PhysicalCard pCard : DummyData.createPhysicalCardDummyList(numOfCards)) {
                deck.addPhysicalCard(pCard);
            }
            // insert target physical card to edit in random number of decks (+1)
            if (!deck.getName().equals("Wished") && r.nextBoolean()) {
                deck.addPhysicalCard(mockPCard);
                numOfDecksModified++;
            }
        }

        // expects
        setupForValidToken();
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(new HashMap<>());
        expect(mockConfig.getServletContext()).andReturn(mockCtx);
        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                .andReturn(new HashMap<>() {{
                    put("test@test.it", new LinkedHashMap<>() {{
                        for (Deck deck : decks)
                            put(deck.getName(), deck);
                    }});
                }});

        for (int i = 0; i < numOfDecksModified * (numOfCards + 1); i++) {
            expect(mockConfig.getServletContext()).andReturn(mockCtx);
            expect(mockDB.getCachedMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
                    .andReturn(new HashMap());
        }

        ctrl.replay();
        List<ModifiedDeckPayload> modifiedDecks = deckService.editPhysicalCard("validToken", "Owned", editedPCard);
        ctrl.verify();
        // number of decks modified should be equal of number of decks returned
        Assertions.assertEquals(numOfDecksModified, modifiedDecks.size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalidToken"})
    public void testGetListPhysicalCardWithEmailDealingForInvalidToken(String input) {
        setupForInvalidToken();
        ctrl.replay();
        Assertions.assertThrows(AuthException.class, () -> deckService.getListPhysicalCardWithEmailDealing(input, Game.randomGame(), 1));
        ctrl.verify();
    }

    @Test
    public void testGetListPhysicalCardWithEmailDealingForNullGame() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> {
            deckService.getListPhysicalCardWithEmailDealing("validToken", null, 1);
            ctrl.verify();
        });
    }

    @Test
    public void testGetListPhysicalCardWithEmailDealingForNegativeCardId() {
        setupForValidToken();
        ctrl.replay();
        Assertions.assertThrows(InputException.class, () -> {
            deckService.getListPhysicalCardWithEmailDealing("validToken", Game.randomGame(), -1);
            ctrl.verify();
        });
    }

    //    private Map<String, Deck> generateValidDeckofMagicPhysicalCardMap(Map<Integer, MagicCard> magicCardMap, String deckName) {
//        Map<String, Deck> deckMap = new HashMap<>() {{
//            put("Owned", new Deck("Owned", true));
//            put("Wished", new Deck("Wished", true))
//            put(deckName, new Deck(deckName, true));
//        }};
//        String validDescription = "This is a super valid description";
//        ArrayList<Integer> allIdCards = new ArrayList<>(magicCardMap.size());
//        for (Map.Entry<Integer, MagicCard> entry : magicCardMap.entrySet()) {
//            allIdCards.add(entry.getValue().getId());
//        }
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Excellent, validDescription));    // index 0
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.VeryDamaged, validDescription));  // index 1
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));          // index 2 !!THIS!!
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(1), Status.Excellent, validDescription));    // index 3 !!THIS!!
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(2), Status.Good, validDescription));         // index 4
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(3), Status.VeryDamaged, validDescription));  // index 5
//        return deckMap;
//    }
//
//    private Map<String, Deck> generateValidExtendedDeckofMagicPhysicalCardMap(Map<Integer, MagicCard> magicCardMap, String deckName) {
//        //Extended deck, with repetitions, is use for specific tests
//        //normal deck population
//        Map<String, Deck> deckMap = generateValidDeckofMagicPhysicalCardMap(magicCardMap, deckName);
//        ArrayList<Integer> allIdCards = new ArrayList<>(magicCardMap.size());
//        for (Map.Entry<Integer, MagicCard> entry : magicCardMap.entrySet()) {
//            allIdCards.add(entry.getValue().getId());
//        }
//        String validDescription = "This is a super valid description";
//        //add repetitions
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Excellent, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Fair, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Damaged, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.VeryDamaged, validDescription));
//        deckMap.get("Owned").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.VeryDamaged, validDescription));
//        return deckMap;
//    }
//
//
//    private Map<String, Deck> generateValidWishedDeckofMagicPhysicalCardMap(Map<Integer, MagicCard> magicCardMap) {
//        //
//        String deckName = "Wished";
//        Map<String, Deck> deckMap = new HashMap<>() {{
//            put(deckName, new Deck(deckName, true));
//        }};
//        String validDescription = "This is a super valid description";
//        ArrayList<Integer> allIdCards = new ArrayList<>(magicCardMap.size());
//        for (Map.Entry<Integer, MagicCard> entry : magicCardMap.entrySet()) {
//            allIdCards.add(entry.getValue().getId());
//        }
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(1), Status.Good, validDescription));
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(2), Status.Excellent, validDescription));
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(2), Status.Excellent, validDescription)); // repeated
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(4), Status.Excellent, validDescription));
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(4), Status.Good, validDescription));
//        deckMap.get(deckName).addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(4), Status.VeryDamaged, validDescription));
//
//        return deckMap;
//    }
//
//    private Map<String, Deck> generateValidExtendedWishedDeckofMagicPhysicalCardMap(Map<Integer, MagicCard> magicCardMap) {
//        //Extended wished deck, with repetitions, is use for specific tests
//        //normal wished deck population
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(magicCardMap);
//        ArrayList<Integer> allIdCards = new ArrayList<>(magicCardMap.size());
//        for (Map.Entry<Integer, MagicCard> entry : magicCardMap.entrySet()) {
//            allIdCards.add(entry.getValue().getId());
//        }
//        //add repetitions
//        String validDescription = "This is a super valid description";
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Excellent, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Good, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Fair, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.Damaged, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.VeryDamaged, validDescription));
//        deckWishedMap.get("Wished").addPhysicalCard(new PhysicalCard(Game.MAGIC, allIdCards.get(0), Status.VeryDamaged, validDescription));
//
//        return deckWishedMap;
//    }
//
//    private int countNullCardPawn(List<PhysicalCardWithEmailDealing> actual) {
//        int countNullPawnCard = 0;
//        for (PhysicalCardWithEmailDealing item : actual) {
//            if (item.getIdPhysicalCardPawn() == null) {
//                countNullPawnCard++;
//            }
//        }
//        return countNullPawnCard;
//    }

//    @Test
//    public void testGetListPhysicalCardWithEmailDealingWithoutOwnedDeck() {
//        setupForValidToken();
//        Map<String, Deck> deckMap = createMock(createMagicDummyMap(), "OtherDeck");
//        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
//            put("test@test.it", deckMap);
//        }};
//
//        expect(mockConfig.getServletContext()).andReturn(mockCtx);
//        expect(mockDB.getPersistentMap(isA(ServletContext.class), anyString(), isA(Serializer.class), isA(Serializer.class)))
//                .andReturn(mockDeckMap);
//        ctrl.replay();
//        Assertions.assertThrows(DeckNotFoundException.class, () ->
//                deckService.getListPhysicalCardWithEmailDealing("validToken", Game.randomGame(), 3)
//        );
//        ctrl.verify();
//    }

//    @Test
//    public void testGetListPhysicalCardWithEmailDealingWithEmptyOwnedDeck() throws BaseException {
//        setupForValidToken();
//        //Generate empty Owned deck
//        Map<String, Deck> deckMap = new HashMap<>() {{
//            put("Owned", new Deck("Owned", true));
//        }};
//        Map<String, Map<String, Deck>> mockDeckMap = new HashMap<>() {{
//            put("test@test.it", deckMap);
//        }};
//
//        //Generate Wished consistent deck
//        Map<Integer, MagicCard> MagicCardMap = createMagicDummyMap();
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(MagicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//
//        List<PhysicalCardWithEmailDealing> actual = deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, arrWished[4].getCardId());
//
//        //Count null PawnCard from method call
//        final int resultCountNullPawnCard;
//        resultCountNullPawnCard = countNullCardPawn(actual);
//
//        Assertions.assertAll(() -> {
//            Assertions.assertEquals(3, actual.size());
//            Assertions.assertEquals(actual.size(), resultCountNullPawnCard);
//        });
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingWithMultiplePCardMatchingInOwnedDeck() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> magicCardMap = createMagicDummyMap();
//
//        //Create extended Owned deck map, with multiple and repeated cards
//        Map<String, Deck> deckOwnedMap = generateValidExtendedDeckofMagicPhysicalCardMap(magicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//
//        //Generate normal Wished consistent deck
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(magicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard whishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(whishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//        List<PhysicalCardWithEmailDealing> actual = deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, arrWished[0].getCardId());
//
//        //Count null PawnCard from method call
//        final int resultCountNullPawnCard;
//        resultCountNullPawnCard = countNullCardPawn(actual);
//
//        Assertions.assertAll(() -> {
//            Assertions.assertEquals(1, actual.size());
//            Assertions.assertEquals(0, resultCountNullPawnCard);
//            Assertions.assertEquals(expectedPawnListId[0], actual.get(0).getIdPhysicalCardPawn());
//        });
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingWithMultiplePCardMatchingInOwnedDeckAndMultipleSameWishedCard() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> magicCardMap = createMagicDummyMap();
//
//        //Create extended Owned deck map, with multiple and repeated cards
//        Map<String, Deck> deckOwnedMap = generateValidExtendedDeckofMagicPhysicalCardMap(magicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//        //Generate wished extended consistent deck, with multiple and repeated cards
//        Map<String, Deck> deckWishedMap = generateValidExtendedWishedDeckofMagicPhysicalCardMap(magicCardMap);
//
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null, null, null, null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard wishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(wishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//        List<PhysicalCardWithEmailDealing> actual = deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, arrWished[0].getCardId());
//
//        //Count null PawnCard from method call
//        final int resultCountNullPawnCard;
//        resultCountNullPawnCard = countNullCardPawn(actual);
//
//        Assertions.assertAll(() -> {
//            Assertions.assertEquals(8, actual.size());
//            Assertions.assertEquals(0, resultCountNullPawnCard);
//            Assertions.assertEquals(expectedPawnListId[0], actual.get(0).getIdPhysicalCardPawn());
//        });
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingMatchingIdCard() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> MagicCardMap = createMagicDummyMap();
//
//        //Generate normal Owned consistent deck
//        Map<String, Deck> deckOwnedMap = generateValidDeckofMagicPhysicalCardMap(MagicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//
//        //Generate normal Wished consistent deck
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(MagicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard wishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(wishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Create array of Catalogue Card id from WishedCards
//        Integer[] wishedCardId = new Integer[]{arrWished[0].getCardId(), arrWished[1].getCardId(), arrWished[2].getCardId(), arrWished[3].getCardId()};
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//
//        Assertions.assertEquals(expectedPawnListId[0], deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, wishedCardId[0]).get(0).getIdPhysicalCardPawn());
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingMatchingIdCard2() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> MagicCardMap = createMagicDummyMap();
//
//        //Create normal Owned deck map
//        Map<String, Deck> deckOwnedMap = generateValidDeckofMagicPhysicalCardMap(MagicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//
//        //Generate normal Wished consistent deck
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(MagicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard wishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(wishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Create array of Catalogue Card id from WishedCards
//        Integer[] wishedCardId = new Integer[]{arrWished[0].getCardId(), arrWished[1].getCardId(), arrWished[2].getCardId(), arrWished[3].getCardId()};
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//
//        Assertions.assertEquals(expectedPawnListId[1], deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, wishedCardId[1]).get(0).getIdPhysicalCardPawn());
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingMatchingIdCard3_resultNullPawn() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> MagicCardMap = createMagicDummyMap();
//
//        //Generate normal Owned consistent deck
//        Map<String, Deck> deckOwnedMap = generateValidDeckofMagicPhysicalCardMap(MagicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//
//        //Generate normal Wished consistent deck
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(MagicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard wishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(wishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Create array of Catalogue Card id from WishedCards
//        Integer[] wishedCardId = new Integer[]{arrWished[0].getCardId(), arrWished[1].getCardId(), arrWished[2].getCardId(), arrWished[3].getCardId()};
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//
//        Assertions.assertEquals(expectedPawnListId[2], deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, wishedCardId[2]).get(0).getIdPhysicalCardPawn());
//
//        ctrl.verify();
//    }
//
//    @Test
//    public void testGetListPhysicalCardWithEmailDealingMatchingIdCard3_resultNullPawn2() throws BaseException {
//        setupForValidToken();
//        //Generate magicCard catalogue
//        Map<Integer, MagicCard> MagicCardMap = createMagicDummyMap();
//
//        //Generate normal Owned consistent deck
//        Map<String, Deck> deckOwnedMap = generateValidDeckofMagicPhysicalCardMap(MagicCardMap, "Owned");
//        Map<String, Map<String, Deck>> mockOwnedDeckMap = new HashMap<>() {{
//            put("test@test.it", deckOwnedMap);
//        }};
//
//        //Generate normal Wished consistent deck
//        Map<String, Deck> deckWishedMap = generateValidWishedDeckofMagicPhysicalCardMap(MagicCardMap);
//        Map<String, Map<String, Deck>> mockWishedDeckMap = new HashMap<>() {{
//            put("wisher@test.it", deckWishedMap);
//        }};
//
//        //Create array from OwnedDeck to build expectedPawnListId & create array from WishedDeck to find real catalogue cardId
//        PhysicalCard[] arrOwned = new PhysicalCard[deckOwnedMap.get("Owned").getPhysicalCards().size()];
//        PhysicalCard[] arrWished = new PhysicalCard[deckWishedMap.get("Wished").getPhysicalCards().size()];
//
//        deckOwnedMap.get("Owned").getPhysicalCards().toArray(arrOwned);
//        deckWishedMap.get("Wished").getPhysicalCards().toArray(arrWished);
//
//        String[] expectedPawnListId = new String[]{arrOwned[2].getId(), arrOwned[3].getId(), null, null, null, null, null};
//
//        //Build the PhysicalCardWithEmailDealing expected
//        List<PhysicalCardWithEmailDealing> expectedResponse = new ArrayList<>();
//        int counter = 0;
//        for (PhysicalCard wishedPcard : deckWishedMap.get("Wished").getPhysicalCards()) {
//
//            expectedResponse.add(new PhysicalCardWithEmailDealing(new PhysicalCardWithEmail(wishedPcard, "wisher@test.it"), expectedPawnListId[counter]));
//            counter++;
//        }
//
//        //Create array of Catalogue Card id from WishedCards
//        Integer[] wishedCardId = new Integer[]{arrWished[0].getCardId(), arrWished[1].getCardId(), arrWished[2].getCardId(), arrWished[3].getCardId()};
//
//
//        //Setup Mocks
//        setupMockCall(new Map[]{mockOwnedDeckMap, mockWishedDeckMap});
//        ctrl.replay();
//
//        Assertions.assertEquals(expectedPawnListId[3], deckService.getListPhysicalCardWithEmailDealing("validToken", Game.MAGIC, wishedCardId[3]).get(0).getIdPhysicalCardPawn());
//
//        ctrl.verify();
//    }
}
