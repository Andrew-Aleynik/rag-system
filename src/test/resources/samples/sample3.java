package com.andrewaleynik.reportdesigner.reportdesigner.controllers;

import com.andrewaleynik.reportdesigner.reportdesigner.App;
import com.andrewaleynik.reportdesigner.reportdesigner.datamodels.ElementDataModel;
import com.andrewaleynik.reportdesigner.reportdesigner.models.Element;
import com.andrewaleynik.reportdesigner.reportdesigner.util.AlertFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElementsTreeTabController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementsTreeTabController.class);
    private final ElementDataModel elementDataModel;
    @FXML
    private ComboBox<Element> rootElementsComboBox;
    @FXML
    private TreeView<Element> elementsTreeView;


    public ElementsTreeTabController(ElementDataModel elementDataModel) {
        this.elementDataModel = elementDataModel;
    }

    @FXML
    public void initialize() {
        initializeRootElementsComboBox();
        initializeElementsTreeView();
    }

    private void initializeRootElementsComboBox() {
        rootElementsComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });

        rootElementsComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });

        rootElementsComboBox.setOnAction(event -> {
            Element selected = rootElementsComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                refreshTreeView(selected);
            }
        });

        rootElementsComboBox.setItems(elementDataModel.getRootElements());
    }

    private void initializeElementsTreeView() {
        elementsTreeView.setCellFactory(new Callback<>() {
            @Override
            public TreeCell<Element> call(TreeView<Element> param) {
                return new TreeCell<>() {
                    private final HBox hbox = new HBox(5);
                    private final Label codeLabel = new Label();
                    private final Label separator = new Label("-");
                    private final Label nameLabel = new Label();
                    private final Button addButton = new Button("+");
                    private final Button editButton = new Button("Редактировать");
                    private final Button deleteButton = new Button("Удалить");

                    {
                        hbox.getChildren().setAll(codeLabel, separator, nameLabel, addButton, editButton, deleteButton);
                        separator.setStyle("-fx-text-fill: gray;");
                        addButton.setStyle("-fx-font-weight: bold; -fx-padding: 1 3;");

                        addButton.setOnAction(event -> {
                            Element currentElement = getItem();
                            if (currentElement != null) {
                                showAddChildElementForm(currentElement);
                            }
                        });

                        editButton.setOnAction(event -> {
                            Element currentElement = getItem();
                            if (currentElement != null) {
                                showEditElementForm(currentElement);
                            }
                        });

                        deleteButton.setOnAction(event -> {
                            Element currentElement = getItem();
                            if (currentElement != null) {
                                LOGGER.debug("Current element: " + currentElement);
                                elementDataModel.deleteElement(currentElement);
                                elementDataModel.refreshRootElements();
                                refreshTreeView(rootElementsComboBox.getValue());
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Element item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            codeLabel.setText(item.getCode());
                            nameLabel.setText(item.getName());
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
    }

    private void refreshTreeView(Element rootElement) {
        if (rootElement == null) {
            elementsTreeView.setRoot(null);
        } else {
            Map<Element, Boolean> expandedStates = saveExpandedStates();

            TreeItem<Element> root = createTreeItem(rootElement, expandedStates);
            elementsTreeView.setRoot(root);

            restoreExpandedStates(root, expandedStates);

            root.setExpanded(true);
        }
    }

    private Map<Element, Boolean> saveExpandedStates() {
        Map<Element, Boolean> expandedStates = new HashMap<>();
        if (elementsTreeView.getRoot() != null) {
            saveExpandedStatesRecursive(elementsTreeView.getRoot(), expandedStates);
        }
        return expandedStates;
    }

    private void saveExpandedStatesRecursive(TreeItem<Element> item, Map<Element, Boolean> expandedStates) {
        if (item != null && item.getValue() != null) {
            expandedStates.put(item.getValue(), item.isExpanded());
            for (TreeItem<Element> child : item.getChildren()) {
                saveExpandedStatesRecursive(child, expandedStates);
            }
        }
    }

    private TreeItem<Element> createTreeItem(Element element, Map<Element, Boolean> expandedStates) {
        TreeItem<Element> item = new TreeItem<>(element);

        if (expandedStates.containsKey(element)) {
            item.setExpanded(expandedStates.get(element));
        }

        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            for (Element child : element.getChildren()) {
                item.getChildren().add(createTreeItem(child, expandedStates));
            }
        }

        return item;
    }

    private void restoreExpandedStates(TreeItem<Element> root, Map<Element, Boolean> expandedStates) {
        if (root == null) return;

        restoreExpandedStatesRecursive(root, expandedStates);
    }

    private void restoreExpandedStatesRecursive(TreeItem<Element> item, Map<Element, Boolean> expandedStates) {
        if (item != null && item.getValue() != null) {
            Boolean wasExpanded = expandedStates.get(item.getValue());
            if (wasExpanded != null) {
                item.setExpanded(wasExpanded);
            }
            for (TreeItem<Element> child : item.getChildren()) {
                restoreExpandedStatesRecursive(child, expandedStates);
            }
        }
    }

    @FXML
    private void showAddRootElementForm() {
        try {
            elementDataModel.refreshSelectedParentElement(null);
            elementDataModel.refreshSelectedEditElement(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(App.FxmlPaths.ADD_ROOT_ELEMENT_FORM));
            loader.setControllerFactory(App.getControllerFactory());
            Parent root = loader.load();
            ElementFormController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Добавление структурной модели");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootElementsComboBox.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                rootElementsComboBox.getSelectionModel().select(elementDataModel.getNewElement());
            }
        } catch (IOException e) {
            LOGGER.error("Error opening form: {}", e.getMessage(), e);
            AlertFactory.showError("Ошибка при открытии формы", e.getMessage());
        }
    }

    @FXML
    public void showElementQualitiesExternalInfluencesFlagTable() {

    }

    @FXML
    public void showPreviewExportToPdf() {
        try {
            Element currentRoot = rootElementsComboBox.getValue();
            File pdfFile = elementDataModel.exportElementsTreeToPdf(currentRoot);

            LOGGER.debug("PDF was created");

            FXMLLoader loader = new FXMLLoader(getClass().getResource(App.FxmlPaths.EXPORT_PREVIEW));
            loader.setControllerFactory(App.getControllerFactory());
            Parent root = loader.load();
            PreviewController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Предпросмотр");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootElementsComboBox.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            controller.setPdfFile(pdfFile);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.error("Error opening form: {}", e.getMessage(), e);
            AlertFactory.showError("Ошибка при открытии формы", e.getMessage());
        }
    }

    private void showAddChildElementForm(Element parentElement) {
        try {
            elementDataModel.refreshSelectedParentElement(parentElement);
            elementDataModel.refreshSelectedEditElement(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(App.FxmlPaths.ADD_CHILD_ELEMENT_FORM));
            loader.setControllerFactory(App.getControllerFactory());
            Parent root = loader.load();
            ElementFormController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Добавление дочернего элемента");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootElementsComboBox.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);

            Element selectedRoot = rootElementsComboBox.getSelectionModel().getSelectedItem();
            dialogStage.showAndWait();
            rootElementsComboBox.getSelectionModel().select(selectedRoot);
            refreshTreeView(rootElementsComboBox.getSelectionModel().getSelectedItem());
        } catch (IOException e) {
            LOGGER.error("Error opening form: {}", e.getMessage(), e);
            AlertFactory.showError("Ошибка при открытии формы", e.getMessage());
        }
    }

    private void showEditElementForm(Element editingElement) {
        try {
            elementDataModel.refreshSelectedParentElement(null);
            elementDataModel.refreshSelectedEditElement(editingElement);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(App.FxmlPaths.EDIT_ELEMENT_FORM));
            loader.setControllerFactory(App.getControllerFactory());
            Parent root = loader.load();
            ElementFormController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактирование элемента");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootElementsComboBox.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);

            Element selectedRoot = rootElementsComboBox.getSelectionModel().getSelectedItem();
            dialogStage.showAndWait();
            rootElementsComboBox.getSelectionModel().select(selectedRoot);
            refreshTreeView(rootElementsComboBox.getSelectionModel().getSelectedItem());
        } catch (IOException e) {
            LOGGER.error("Error opening form: {}", e.getMessage(), e);
            AlertFactory.showError("Ошибка при открытии формы", e.getMessage());
        }
    }
}