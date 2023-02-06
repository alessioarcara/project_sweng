package com.aadm.cardexchange.client.views;

import com.aadm.cardexchange.client.handlers.ImperativeHandleDeck;
import com.aadm.cardexchange.client.widgets.DeckWidget;
import com.aadm.cardexchange.shared.models.Deck;
import com.aadm.cardexchange.shared.models.PhysicalCard;
import com.aadm.cardexchange.shared.models.PhysicalCardWithName;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DecksViewImpl extends Composite implements DecksView, ImperativeHandleDeck {
    private static final DecksViewImpl.DecksViewImplUIBinder uiBinder = GWT.create(DecksViewImpl.DecksViewImplUIBinder.class);
    private static final String DEFAULT_CUSTOM_DECK_TEXT = "Write here name for your custom deck";
    Presenter presenter;
    @UiField
    HTMLPanel decksContainer;
    @UiField
    HeadingElement newDeckName;

    public DecksViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setData(List<String> data) {
        for (String deckName : data) {
            decksContainer.add(createDeck(deckName));
        }
    }

    @Override
    public void onShowDeck(String deckName, BiConsumer<List<PhysicalCardWithName>, String> setDeckData) {
        presenter.fetchUserDeck(deckName, setDeckData);
    }

    @Override
    public void onRemovePhysicalCard(String deckName, PhysicalCard pCard, Consumer<List<Deck>> isRemoved) {
        presenter.removePhysicalCardFromDeck(deckName, pCard, isRemoved);
    }

    @Override
    public void resetData() {
        decksContainer.clear();
    }

    @Override
    public void displayAlert(String message) {
        Window.alert(message);
    }

    @Override
    public void displayAddedCustomDeck(String deckName) {
        decksContainer.add(createDeck(deckName));
        newDeckName.setInnerText(DEFAULT_CUSTOM_DECK_TEXT);
    }

    // factory
    private DeckWidget createDeck(String deckName) {
        if (deckName.equals("Owned") || deckName.equals("Wished")) {
            return new DeckWidget(this, null, null, deckName);
        } else {
            Button removeButton = new Button("x", (ClickHandler) e -> {
                if (Window.confirm("Are you sure you want to remove this deck?"))
                    presenter.deleteCustomDeck();
            });
            removeButton.setStyleName("deckButton");
            return new DeckWidget(this, removeButton, null, deckName);
        }
    }

    @UiHandler(value = "newDeckButton")
    public void onClickCustomDeckAdd(ClickEvent e) {
        presenter.createCustomDeck(newDeckName.getInnerText());
    }

    @Override
    public void setPresenter(DecksView.Presenter presenter) {
        this.presenter = presenter;
    }

    interface DecksViewImplUIBinder extends UiBinder<Widget, DecksViewImpl> {
    }
}
