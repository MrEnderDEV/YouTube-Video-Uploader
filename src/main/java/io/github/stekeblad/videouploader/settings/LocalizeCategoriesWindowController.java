package io.github.stekeblad.videouploader.settings;

import io.github.stekeblad.videouploader.utils.AlertUtils;
import io.github.stekeblad.videouploader.utils.ConfigManager;
import io.github.stekeblad.videouploader.utils.Translations;
import io.github.stekeblad.videouploader.utils.background.OpenInBrowser;
import io.github.stekeblad.videouploader.youtube.utils.CategoryUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.DataFormatException;

public class LocalizeCategoriesWindowController {
    public AnchorPane window;
    public TextField txt_country;
    public TextField txt_lang;
    public TextArea txt_description;
    public Button btn_getCategories;
    public Button btn_cancel;
    public Button btn_codeListCountry;
    public Button btn_codeListLang;
    public Label label_langId;
    public Label label_countryId;

    private ConfigManager configManager = ConfigManager.INSTANCE;
    private CategoryUtils categoryUtils = CategoryUtils.INSTANCE;
    private Translations transLocCatWin;
    private Translations transBasic;

    /**
     * Initialize things when the window is opened, used instead of initialize as that one does not have access to the scene
     */
    public void myInit() {
        // Filter what can be entered into the textFields
        txt_country.textProperty().addListener((observable, oldValue, newValue) -> {
            if(! newValue.matches("[A-Za-z]*") || newValue.length() > 2) {
                txt_country.setText(oldValue);
            }
        });

        txt_lang.textProperty().addListener((observable, oldValue, newValue) -> {
            if(! newValue.matches("[A-Za-z]*") || newValue.length() > 2) {
                txt_lang.setText(oldValue);
            }
        });

        // Load Translations
        try {
            transLocCatWin = new Translations("locCatWindow");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.simpleClose("Error loading translations", "Failed loading translations for localize " +
                    " categories window, the window can not be opened. Sorry!\n\nDetected language: " + Locale.getDefault())
                    .showAndWait();
            return;
        }
        try {
            transBasic = new Translations("baseStrings");
        } catch (Exception e) {
            AlertUtils.simpleClose("Error loading translations", "Failed loading basic translations" +
                    ", the window can not be opened. Sorry!\n\nDetected language: " + Locale.getDefault()).showAndWait();
            return;
        }
        transLocCatWin.autoTranslate(window);

        // Add description text
        txt_description.setText(transLocCatWin.getString("description"));

        // set ToolTips
        btn_codeListCountry.setTooltip(new Tooltip("https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#Officially_assigned_code_elements"));
        btn_codeListLang.setTooltip(new Tooltip("https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes"));

        // Insert the current country and language code in their text fields
        txt_country.setText(configManager.getCategoryCountry());
        txt_lang.setText(configManager.getCategoryLanguage());

        // Set so pressing F1 opens the wiki page for this window
        window.getScene().setOnKeyPressed(event -> {
            System.out.println("'ey, opening webby!");
            if (event.getCode() == KeyCode.F1) {
                OpenInBrowser.openInBrowser("https://github.com/Stekeblad/Stekeblads-Video-Uploader/wiki/Localize-Categories");
                event.consume();
            }
        });
    }

    /**
     * Called when the get categories button is clicked.
     * Performs a quick check of the input before sending it to YouTube
     * @param actionEvent the click event
     */
    public void onGetCategoriesClicked(ActionEvent actionEvent) {

        // test if the codes is of the correct length
        try {
            configManager.setCategoryCountry(txt_country.getText());
            configManager.setCategoryLanguage(txt_lang.getText());
            configManager.saveSettings();
        } catch (DataFormatException e) {
            AlertUtils.simpleClose(transLocCatWin.getString("diag_invalidCodes_short"),
                    transLocCatWin.getString("diag_invalidCodes_full")).show();
            actionEvent.consume();
            return;
        }

        // Authentication with youtube is required, check if the user has given permission, if not then ask for it
        if(configManager.getNeverAuthed()) {
            Optional<ButtonType> buttonChoice = AlertUtils.yesNo(transBasic.getString("auth_short"),
                    transBasic.getString("auth_full")).showAndWait();
            if (buttonChoice.isPresent()) {
                if (buttonChoice.get() == ButtonType.YES) {
                    configManager.setNeverAuthed(false);
                    configManager.saveSettings();
                } else { // ButtonType.NO or closed [X]
                    actionEvent.consume();
                    return;
                }
            }
        }

        // Visually indicate the program is working
        btn_getCategories.setText(transLocCatWin.getString("downloading"));
        btn_cancel.setDisable(true);
        btn_getCategories.setDisable(true);

        // Send the request in the background
        // Tell it what to do
        Task<Void> backgroundTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                categoryUtils.downloadCategories();
                Platform.runLater(() -> {
                    if (categoryUtils.getCategoryNames().size() < 2) { // < 2 because of default "no categories" category
                        AlertUtils.simpleClose(transBasic.getString("error"),
                                transLocCatWin.getString("diag_invalidCodeResp")).showAndWait();
                        btn_getCategories.setText(transLocCatWin.getString("btn_getCategories"));
                        btn_cancel.setDisable(false);
                        btn_getCategories.setDisable(false);
                        return;
                    }
                    onCancelClicked(new ActionEvent());
                });
                return null;
            }
        };

        Thread backgroundThread = new Thread(backgroundTask);
        // Define a handler for exceptions
        backgroundThread.setUncaughtExceptionHandler((t, e) -> Platform.runLater(() -> {
            AlertUtils.simpleClose(transBasic.getString("error"),
                    transLocCatWin.getString("diag_catReqFailed")).showAndWait();
            e.printStackTrace();
            onCancelClicked(new ActionEvent());
        }));

        // Actually do the thing, start the process of getting the categories!
        backgroundThread.start();
        actionEvent.consume();
    }

    /**
     * Called when the cancel button is clicked.
     * Closes the window.
     * @param actionEvent the click event
     */
    public void onCancelClicked(ActionEvent actionEvent) {
        ((Stage) btn_cancel.getScene().getWindow()).close();
        actionEvent.consume();
    }

    /**
     * called when the code list button for countries is clicked.
     * Opens a web page with country codes.
     * @param actionEvent the click event
     */
    public void onCodeListCountryClicked(ActionEvent actionEvent) {
        try {
            URI link = new URI("https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#Officially_assigned_code_elements");
            OpenInBrowser.openInBrowser(link, (t, e) -> Platform.runLater(() -> {
                String desc = String.format(transLocCatWin.getString("diag_cantOpenBrowser"), link);
                AlertUtils.simpleClose(transBasic.getString("sorry"), desc).showAndWait();
            }));
        } catch (URISyntaxException e) {
            System.err.println("URI Error for CountryList");
        }
        actionEvent.consume();
    }

    /**
     * called when the code list button for languages is clicked.
     * Opens a web page with language codes.
     * @param actionEvent the click event
     */
    public void onCodeListLangClicked(ActionEvent actionEvent) {
        try {
            URI link = new URI("https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes");
            OpenInBrowser.openInBrowser(link, (t, e) -> Platform.runLater(() -> {
                String desc = String.format(transLocCatWin.getString("diag_cantOpenBrowser"), link);
                AlertUtils.simpleClose(transBasic.getString("sorry"), desc).showAndWait();
            }));
        } catch (URISyntaxException e) {
            System.err.println("URI Error for LangList");

        }
        actionEvent.consume();
    }
}
