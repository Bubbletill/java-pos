package store.bubbletill.pos.views;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import store.bubbletill.commons.BubbleView;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSHomeController;

public class OpeningFloatView implements BubbleView {

    private final POSApplication app;
    private final POSHomeController controller;

     private final Pane declareOpeningFloat;
     private final Pane dofPrompt;
     private final Pane dofDeclare;
     private final TextField dof50;
     private final TextField dof20;
     private final TextField dof10;
     private final TextField dof5;
     private final TextField dof1;
     private final TextField dof50p;
     private final TextField dof20p;
     private final TextField dof10p;
     private final TextField dof5p;
     private final TextField dof2p;
     private final TextField dof1p;
    
    public OpeningFloatView(POSApplication app, POSHomeController controller, Pane declareOpeningFloat, Pane dofPrompt,
                            Pane dofDeclare, TextField dof50, TextField dof20, TextField dof10, TextField dof5,
                            TextField dof1, TextField dof50p, TextField dof20p, TextField dof10p, TextField dof5p,
                            TextField dof2p, TextField dof1p, Button openingFloatYesButton, Button openingFloatNoButton,
                            Button openingFloatSubmitButton) {

        this.app = app;
        this.controller = controller;
        this.declareOpeningFloat = declareOpeningFloat;
        this.dofPrompt = dofPrompt;
        this.dofDeclare = dofDeclare;
        this.dof50 = dof50;
        this.dof20 = dof20;
        this.dof10 = dof10;
        this.dof5 = dof5;
        this.dof1 = dof1;
        this.dof50p = dof50p;
        this.dof20p = dof20p;
        this.dof10p = dof10p;
        this.dof5p = dof5p;
        this.dof2p = dof2p;
        this.dof1p = dof1p;

        openingFloatYesButton.setOnAction(e -> { onYes(); });
        openingFloatNoButton.setOnAction(e -> { onNo(); });
        openingFloatSubmitButton.setOnAction(e -> { onSubmit(); });
    }

    @Override
    public void show() {
        declareOpeningFloat.setVisible(true);
        dofPrompt.setVisible(true);
        dofDeclare.setVisible(false);
    }

    @Override
    public void hide() {
        declareOpeningFloat.setVisible(false);
        dofPrompt.setVisible(false);
        dofDeclare.setVisible(false);
    }

    private void onNo() {
        hide();
        controller.homeTenderView.show();
        controller.showError(null);
        app.cashInDraw = 0;
    }


    private void onYes() {
        controller.showError(null);
        if (app.managerLoginRequest("Opening Float")) {
            dofPrompt.setVisible(false);
            dofDeclare.setVisible(true);
        } else {
            controller.showError("Insufficient permission.");
        }
    }

    private void onSubmit() {
        try {
            app.cashInDraw = 0;
            app.cashInDraw += Integer.parseInt(dof50.getText()) * 50;
            app.cashInDraw += Integer.parseInt(dof20.getText()) * 20;
            app.cashInDraw += Integer.parseInt(dof10.getText()) * 10;
            app.cashInDraw += Integer.parseInt(dof5.getText()) * 5;
            app.cashInDraw += Integer.parseInt(dof1.getText());
            app.cashInDraw += Integer.parseInt(dof50p.getText()) * 0.5;
            app.cashInDraw += Integer.parseInt(dof20p.getText()) * 0.2;
            app.cashInDraw += Integer.parseInt(dof10p.getText()) * 0.1;
            app.cashInDraw += Integer.parseInt(dof5p.getText()) * 0.05;
            app.cashInDraw += Integer.parseInt(dof2p.getText()) * 0.02;
            app.cashInDraw += Integer.parseInt(dof1p.getText()) * 0.01;
        } catch (Exception e) {
            controller.showError("Please populate all fields with a valid number.");
            return;
        }

        //showError(null);
        controller.showError("Cash in draw: " + app.cashInDraw);
        controller.homeTenderView.show();
        app.floatKnown = true;
        hide();
    }
}
