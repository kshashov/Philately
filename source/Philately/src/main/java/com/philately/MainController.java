package com.philately;

import com.philately.mark.MarkParamsCache;
import com.philately.mark.MarksFilter;
import com.philately.model.*;
import com.philately.model.Currency;
import com.philately.view.DoubleTextField;
import com.philately.view.IntegerTextField;
import com.philately.view.MarkListCell;
import com.philately.view.converters.ColorClassConverter;
import com.philately.view.converters.CountryClassConverter;
import com.philately.view.converters.CurrencyClassConverter;
import com.philately.view.converters.PaperClassConverter;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.hibernate.Criteria;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Restrictions;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * Created by kirill on 22.10.2015.
 */
public class MainController {

    @FXML
    private ChoiceBox countryFieldSearch;
    @FXML
    private IntegerTextField yearFieldSearch;
    @FXML
    private ChoiceBox cancellationFieldSearch;
    @FXML
    private TextField themeFieldSearch;
    @FXML
    private TextField seriesFieldSearch;
    @FXML
    private DoubleTextField priceFieldSearch;
    @FXML
    private IntegerTextField editionFieldSearch;
    @FXML
    private TextField separationFieldSearch;
    @FXML
    private ChoiceBox paperFieldSearch;
    @FXML
    private TextField sizeFieldSearch;
    @FXML
    private ChoiceBox colorFieldSearch;
    @FXML
    private ChoiceBox currencyFieldSearch;

    @FXML
    private ListView marksListView;

    @FXML
    private ImageView markImage;

    @FXML
    private ToolBar markToolbar;

    @FXML
    private VBox markVBoxPanel;

    private Mark selectedMark;


    // Reference to the main application.
    private MainApp mainApp;

    //after fxml has been loaded
    @FXML
    private void initialize() {

        setMark(null);

        Country defaultCountry = new Country();
        defaultCountry.setId(-1);
        defaultCountry.setTitle("-");
        countryFieldSearch.setItems(FXCollections.observableList(getArrayWithDefaultElement(defaultCountry, MarkParamsCache.getInstance().getCountries())));
        countryFieldSearch.getSelectionModel().selectFirst();
        countryFieldSearch.setConverter(new CountryClassConverter());

        Color defaultColor = new Color();
        defaultColor.setId(-1);
        defaultColor.setTitle("-");
        colorFieldSearch.setItems(FXCollections.observableList(getArrayWithDefaultElement(defaultColor, MarkParamsCache.getInstance().getColors())));
        colorFieldSearch.getSelectionModel().selectFirst();
        colorFieldSearch.setConverter(new ColorClassConverter());

        Paper defaultPaper = new Paper();
        defaultPaper.setId(-1);
        defaultPaper.setTitle("-");
        paperFieldSearch.setItems(FXCollections.observableList(FXCollections.observableList(getArrayWithDefaultElement(defaultPaper, MarkParamsCache.getInstance().getPapers()))));
        paperFieldSearch.getSelectionModel().selectFirst();
        paperFieldSearch.setConverter(new PaperClassConverter());

        Currency defaultCurrency = new Currency();
        defaultCurrency.setId(-1);
        defaultCurrency.setTitle("-");
        currencyFieldSearch.setItems(FXCollections.observableList(FXCollections.observableList(getArrayWithDefaultElement(defaultCurrency, MarkParamsCache.getInstance().getCurrency()))));
        currencyFieldSearch.getSelectionModel().selectFirst();
        currencyFieldSearch.setConverter(new CurrencyClassConverter());


        handleSearch();
        marksListView.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                return new MarkListCell();
            }
        });

        marksListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                selectedMark = (Mark) marksListView.getSelectionModel().getSelectedItem();
                setMark(selectedMark);
            }
        });


    }



    private List getArrayWithDefaultElement(Object elem, List list){
        List items = new ArrayList<>();
        items.add(elem);
        items.addAll(list);
        return items;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    @FXML
    private void handleExit() {
        mainApp.shutdown();
    }

    @FXML
    private void handleNewMark() {
        boolean okClicked = mainApp.showMarkEditDialog(null);
        if (okClicked) {
            handleSearch();
        }
    }

    /**
     * Called when the user clicks the edit button. Opens a dialog to edit
     * details for the selected person.
     */
    @FXML
    private void handleEditMark() {
        if (selectedMark != null) {
            boolean okClicked = mainApp.showMarkEditDialog(selectedMark);
            if (okClicked) {
                handleSearch();
            }

        } else {
            // Nothing selected.
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle("No Selection");
            alert.setHeaderText("No Mark Selected");
            alert.setContentText("Please select a mark in the table.");

            alert.showAndWait();
        }
    }

    @FXML
    private void handleDeleteMark() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "������� ����� �� �����������?", ButtonType.YES, ButtonType.NO);
        alert.initOwner(mainApp.getPrimaryStage());
        alert.setTitle("��������");
        alert.setHeaderText("�� �������?");
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {

            Session session = HibernateUtil.getSession();
            session.beginTransaction();
            session.delete(selectedMark);
            session.getTransaction().commit();

            selectedMark = null;
            handleSearch();
        }
    }

    @FXML
    private void handleSearch(){
        Criteria criteria = HibernateUtil.getSession().createCriteria(Mark.class);
        if ( ((Country) countryFieldSearch.getSelectionModel().getSelectedItem()).getId() >= 0) {
            criteria = criteria.add(Restrictions.eq("country", countryFieldSearch.getSelectionModel().getSelectedItem()));
        }

        if (yearFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("year", yearFieldSearch.getInt()));
        }

        if ( cancellationFieldSearch.getSelectionModel().getSelectedIndex() > 0) {
            criteria = criteria.add(Restrictions.eq("cancellation", (cancellationFieldSearch.getSelectionModel().getSelectedIndex()) == 1));
        }

        if (themeFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("theme", themeFieldSearch.getText()));
        }

        if (seriesFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("series", seriesFieldSearch.getText()));
        }

        if (priceFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("price", priceFieldSearch.getDouble()));
        }

        if (editionFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("edition", editionFieldSearch.getInt()));
        }

        if (separationFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("separation", separationFieldSearch.getText()));
        }

        if ( ((Paper) paperFieldSearch.getSelectionModel().getSelectedItem()).getId() >= 0) {
            criteria = criteria.add(Restrictions.eq("paper", paperFieldSearch.getSelectionModel().getSelectedItem()));
        }

        if (sizeFieldSearch.getText().length() > 0) {
            criteria = criteria.add(Restrictions.eq("size", sizeFieldSearch.getText()));
        }

        if ( ((Color) colorFieldSearch.getSelectionModel().getSelectedItem()).getId() >= 0) {
            criteria = criteria.add(Restrictions.eq("color", colorFieldSearch.getSelectionModel().getSelectedItem()));
        }

        if ( ((Currency) currencyFieldSearch.getSelectionModel().getSelectedItem()).getId() >= 0) {
            criteria = criteria.add(Restrictions.eq("currency", currencyFieldSearch.getSelectionModel().getSelectedItem()));
        }

        marksListView.setItems(FXCollections.observableList(criteria.list()));
        marksListView.refresh();
        setMark(selectedMark);
    }

    @FXML
    private Label countryField;
    @FXML
    private Label yearField;
    @FXML
    private Label cancellationField;
    @FXML
    private Label themeField;
    @FXML
    private Label seriesField;
    @FXML
    private Label priceField;
    @FXML
    private Label editionField;
    @FXML
    private Label separationField;
    @FXML
    private Label paperField;
    @FXML
    private Label sizeField;
    @FXML
    private Label colorField;

    private void setMark(Mark mark) {
        if (mark == null) { //hide
            markToolbar.setVisible(false);
            markVBoxPanel.setVisible(false);
        } else {
            markToolbar.setVisible(true);
            markVBoxPanel.setVisible(true);
            //TODO image
            markImage.setImage(mark.getImage());
            countryField.setText(mark.getCountry().getTitle());
            yearField.setText(String.valueOf(mark.getYear()));
            cancellationField.setText(mark.isCancellation() ? "��" : "���");
            themeField.setText(mark.getTheme());
            seriesField.setText(mark.getSeries());
            priceField.setText(mark.getPrice() + " " + mark.getCurrency().getTitle());
            editionField.setText(String.valueOf(mark.getEdition()));
            separationField.setText(mark.getSeparation());
            paperField.setText(mark.getPaper().getTitle());
            sizeField.setText(mark.getSize());
            colorField.setText(mark.getColor().getTitle());


        }
    }
}
