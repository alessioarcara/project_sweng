package com.aadm.cardexchange.client.presenters;

import com.aadm.cardexchange.client.auth.AuthSubject;
import com.aadm.cardexchange.client.auth.Observer;
import com.aadm.cardexchange.client.places.CardPlace;
import com.aadm.cardexchange.client.utils.BaseAsyncCallback;
import com.aadm.cardexchange.client.views.CardView;
import com.aadm.cardexchange.shared.CardServiceAsync;
import com.aadm.cardexchange.shared.DeckServiceAsync;
import com.aadm.cardexchange.shared.exceptions.AuthException;
import com.aadm.cardexchange.shared.exceptions.InputException;
import com.aadm.cardexchange.shared.models.Card;
import com.aadm.cardexchange.shared.models.PhysicalCardWithEmail;
import com.aadm.cardexchange.shared.models.PhysicalCardWithEmailDealing;
import com.aadm.cardexchange.shared.models.Status;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import java.util.List;
import java.util.stream.Collectors;

public class CardActivity extends AbstractActivity implements CardView.Presenter, Observer {
    private final CardPlace place;
    private final CardView view;
    private final CardServiceAsync cardService;
    private final DeckServiceAsync deckService;
    private final AuthSubject authSubject;
    private final PlaceController placeController;

    public CardActivity(CardPlace place, CardView view, CardServiceAsync cardService, DeckServiceAsync deckService, AuthSubject authSubject, PlaceController placeController) {
        this.place = place;
        this.view = view;
        this.cardService = cardService;
        this.deckService = deckService;
        this.authSubject = authSubject;
        this.placeController = placeController;
        authSubject.attach(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchCard();
        update();
    }

    @Override
    public void update() {
        view.createUserWidgets(authSubject.isLoggedIn());
        fetchOwnedPhysicalCards();
        fetchWishedPhysicalCards();
    }

    public void fetchCard() {
        cardService.getGameCard(place.getGame(), place.getCardId(), new BaseAsyncCallback<Card>() {
            @Override
            public void onSuccess(Card result) {
                view.setData(result);
            }
        });
    }

    private List<PhysicalCardWithEmail> filterOwnPhysicalCards(List<? extends PhysicalCardWithEmail> pCardsWithEmail) {
        return pCardsWithEmail.stream().filter(pCardWithEmail -> !pCardWithEmail.getEmail().equals(authSubject.getEmail())).collect(Collectors.toList());
    }

    private void fetchOwnedPhysicalCards() {
        deckService.getOwnedPhysicalCardsByCardId(place.getCardId(), new BaseAsyncCallback<List<PhysicalCardWithEmail>>() {
            @Override
            public void onSuccess(List<PhysicalCardWithEmail> result) {
                view.setOwnedByUserList(filterOwnPhysicalCards(result));
            }
        });
    }

    private void fetchWishedPhysicalCards() {
        if (authSubject.isLoggedIn()) {
            deckService.getListPhysicalCardWithEmailDealing(authSubject.getToken(), place.getGame(), place.getCardId(), new BaseAsyncCallback<List<PhysicalCardWithEmailDealing>>() {
                @Override
                public void onSuccess(List<PhysicalCardWithEmailDealing> result) {
                    view.setWishedByUserList(filterOwnPhysicalCards(result));
                }
            });
        } else {
            deckService.getWishedPhysicalCardsByCardId(place.getCardId(), new BaseAsyncCallback<List<PhysicalCardWithEmail>>() {
                @Override
                public void onSuccess(List<PhysicalCardWithEmail> result) {
                    view.setWishedByUserList(filterOwnPhysicalCards(result));
                }
            });
        }
    }

    public void addCardToDeck(String deckName, String status, String description) {
        deckService.addPhysicalCardToDeck(
                authSubject.getToken(),
                place.getGame(),
                deckName,
                place.getCardId(),
                Status.getStatus(Integer.parseInt(status)),
                description,
                new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        if (caught instanceof AuthException) {
                            view.displayAlert(((AuthException) caught).getErrorMessage());
                        } else if (caught instanceof InputException) {
                            view.displayAlert(((InputException) caught).getErrorMessage());
                        } else {
                            view.displayAlert("Internal server error");
                        }
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            view.displayAlert("Success! Card added to " + view.getDeckSelected() + " deck");
                            view.hideModal();
                        } else {
                            view.displayAlert("Deck not found");
                        }
                    }
                }
        );
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
