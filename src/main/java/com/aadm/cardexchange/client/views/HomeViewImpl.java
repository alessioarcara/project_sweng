package com.aadm.cardexchange.client.views;

import com.aadm.cardexchange.client.handlers.ImperativeHandleCard;
import com.aadm.cardexchange.client.places.CardPlace;
import com.aadm.cardexchange.client.widgets.CardWidget;
import com.aadm.cardexchange.client.widgets.GameFiltersWidget;
import com.aadm.cardexchange.shared.models.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

import java.util.*;


public class HomeViewImpl extends Composite implements HomeView, ImperativeHandleCard {
    private static final HomeViewImplUIBinder uiBinder = GWT.create(HomeViewImplUIBinder.class);
    @UiField
    RadioButton magicRadio;
    @UiField
    RadioButton pokemonRadio;
    @UiField
    RadioButton yugiohRadio;
    @UiField
    HTMLPanel cardsPanel;
    @UiField
    GameFiltersWidget filters;
    @UiField
    PushButton applyFiltersButton;
    @UiField
    PushButton cleanFiltersButton;
    private Presenter presenter;
    private boolean isGameChanged;

    public HomeViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        magicRadio.addValueChangeHandler(e -> onGameChanged(Game.MAGIC));
        pokemonRadio.addValueChangeHandler(e -> onGameChanged(Game.POKEMON));
        yugiohRadio.addValueChangeHandler(e -> onGameChanged(Game.YUGIOH));
        applyFiltersButton.addClickHandler(e -> handleFiltersApply());
        cleanFiltersButton.addClickHandler(e -> handleFiltersClean());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setData(List<Card> data) {
        cardsPanel.clear();
        Set<String> uniqueSpecialAttributes = new HashSet<>();
        Set<String> uniqueTypes = new HashSet<>();
        data.forEach(card -> {
            uniqueTypes.add(card.getType());
            if (card instanceof MagicCard) {
                uniqueSpecialAttributes.add(((MagicCard) card).getRarity());
            } else if (card instanceof PokemonCard) {
                uniqueSpecialAttributes.add(((PokemonCard) card).getRarity());
            } else if (card instanceof YuGiOhCard) {
                uniqueSpecialAttributes.add(((YuGiOhCard) card).getRace());
            }
            cardsPanel.add(new CardWidget(this, card));
        });
        if (!isGameChanged) {
            setFilters(uniqueSpecialAttributes, uniqueTypes);
            isGameChanged = true;
        }
    }

    private void setFilters(Set<String> uniqueSpecialAttributes, Set<String> uniqueTypes) {
        filters.specialAttributeOptions.clear();
        filters.typeOptions.clear();
        filters.specialAttributeOptions.addItem("all");
        filters.typeOptions.addItem("all");
        uniqueSpecialAttributes.forEach(specialAttribute -> filters.specialAttributeOptions.addItem(specialAttribute));
        uniqueTypes.forEach(type -> filters.typeOptions.addItem(type));
    }

    private void handleFiltersClean() {
        filters.specialAttributeOptions.setItemSelected(0, true);
        filters.typeOptions.setItemSelected(0, true);
        filters.textOptions.setItemSelected(0, true);
        filters.textInput.setText("");
        filters.checkBoxes.forEach(checkBox -> checkBox.setValue(false));
        setData(presenter.filterGameCards(
                filters.specialAttributeOptions.getSelectedValue(),
                filters.typeOptions.getSelectedValue(),
                filters.textOptions.getSelectedValue(),
                filters.textInput.getText(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
    }

    private void handleFiltersApply() {
        List<String> booleanInputNames = new ArrayList<>();
        List<Boolean> booleanInputValues = new ArrayList<>();
        for (CheckBox checkBox : filters.checkBoxes) {
            booleanInputNames.add(checkBox.getText());
            booleanInputValues.add(checkBox.getValue());
        }
        setData(presenter.filterGameCards(
                filters.specialAttributeOptions.getSelectedValue(),
                filters.typeOptions.getSelectedValue(),
                filters.textOptions.getSelectedValue(),
                filters.textInput.getText(),
                booleanInputNames,
                booleanInputValues
        ));
    }

    @Override
    public void onOpenDetailsClick(Game game, int id) {
        presenter.goTo(new CardPlace(game, id));
    }

    private void onGameChanged(Game game) {
        presenter.fetchGameCards(game);
        filters.handleGameChange(game);
        isGameChanged = false;
    }

    interface HomeViewImplUIBinder extends UiBinder<Widget, HomeViewImpl> {
    }
}
