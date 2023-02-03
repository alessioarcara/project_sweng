package com.aadm.cardexchange.client.views;

import com.aadm.cardexchange.client.widgets.DeckWidget;
import com.aadm.cardexchange.shared.models.PhysicalCardDecorator;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;


public class NewExchangeViewImpl extends Composite implements NewExchangeView {
    private static final NewExchangeViewImpl.NewExchangeViewImplUIBinder uiBinder = GWT.create(NewExchangeViewImpl.NewExchangeViewImplUIBinder.class);
    Presenter presenter;
    String token;
    String selectedCardId;
    @UiField
    HeadingElement receiverEmail;
    @UiField(provided=true)
    DeckWidget senderDeck;
    @UiField(provided=true)
    DeckWidget receiverDeck;
    @UiField
    Button cancelButton;
    @UiField
    Button acceptButton;

    public NewExchangeViewImpl() {
        this.senderDeck = new DeckWidget(null, "");
        this.receiverDeck = new DeckWidget(null, "");
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setData(String receiverUserEmail, String selectedCardId, String token) {
        this.token = token;
        receiverEmail.setInnerHTML(receiverUserEmail);
        this.selectedCardId = selectedCardId;
    }

    public void setSenderDeck(List<PhysicalCardDecorator> physicalCardDecorators) {
        senderDeck.setData(physicalCardDecorators);
    }

    public void setReceiverDeck(List<PhysicalCardDecorator> physicalCardDecorators) {
        receiverDeck.setData(physicalCardDecorators, selectedCardId);
    }

    @Override
    public void setPresenter(NewExchangeView.Presenter presenter) {
        this.presenter = presenter;
    }

    interface NewExchangeViewImplUIBinder extends UiBinder<Widget, NewExchangeViewImpl> {
    }
}