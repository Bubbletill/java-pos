package store.bubbletill.pos.views;

import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import store.bubbletill.commons.BubbleView;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSHomeController;

public class OpeningFloatView implements BubbleView {

    private POSApplication app;
    private POSHomeController controller;

     private Pane declareOpeningFloat;
     private Pane dofPrompt;
     private Pane dofDeclare;
     private TextField dof50;
     private TextField dof20;
     private TextField dof10;
     private TextField dof5;
     private TextField dof1;
     private TextField dof50p;
     private TextField dof20p;
     private TextField dof10p;
     private TextField dof5p;
     private TextField dof2p;
     private TextField dof1p;
    
    public OpeningFloatView(POSApplication app, POSHomeController controller, Pane declareOpeningFloat, Pane dofPrompt, Pane dofDeclare, TextField dof50, TextField dof20,
                            TextField dof10, TextField dof5, TextField dof1, TextField dof50p, TextField dof20p,
                            TextField dof10p, TextField dof5p, TextField dof2p, TextField dof1p) {

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
    }

    @Override
    public void show() {
        
    }

    @Override
    public void hide() {

    }

    private void onOpeningFloatNoButtonPress() {
        declareOpeningFloat.setVisible(false);
        mainHome.setVisible(true);
        controller.showError(null);
        app.cashInDraw = 0;
    }


    private void onOpeningFloatYesButtonPress() {
        controller.showError(null);
        if (app.managerLoginRequest("Opening Float")) {
            dofPrompt.setVisible(false);
            dofDeclare.setVisible(true);
        } else {
            controller.showError("Insufficient permission.");
        }
    }

    private void onDofSubmitPress() {
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

        declareOpeningFloat.setVisible(false);
        //showError(null);
        controller.showError("Cash in draw: " + app.cashInDraw);
        mainHome.setVisible(true);
        app.floatKnown = true;

    }
}
