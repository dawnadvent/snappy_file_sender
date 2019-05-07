package com.suvankarmitra;

import com.suvankarmitra.data.FTConnection;
import com.suvankarmitra.sender.FTSender;
import com.suvankarmitra.utils.Toast;
import com.suvankarmitra.utils.Util;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class FXMLController implements Initializable {
    /*Sender tab fields*/
    @FXML private TextField _senderRecIP1;
    @FXML private TextField _senderRecIP2;
    @FXML private TextField _senderRecIP3;
    @FXML private TextField _senderRecIP4;
    @FXML private TextField _senderFilePath;
    @FXML private TextField _senderPassword;
    @FXML private Button _sendButton;
    @FXML private Button _senderFileChooser;
    @FXML private ImageView _senderCopyIP;
    @FXML private ProgressBar _senderProcessFilePB;
    @FXML private Label _senderProcessFilePercentage;
    @FXML private Button _senderCancelButton;
    @FXML private ImageView _senderBusyLight;
    @FXML private Label _senderReadyLabel;
    @FXML private TextField _senderStatus;
    @FXML private ToggleButton _senderLocalIPToggle;
    private String senderIPAddress;
    private String senderFilePath;
    private String senderPassword;
    private String senderLocalIP;
    private String senderPublicIP;
    private boolean senderTaskCancelled = false;

    /*Receiver tab fields*/
    @FXML private TextField _receiverFileName;
    @FXML private TextField _receiverFilePath;
    @FXML private Button _receiverFileChooser;
    @FXML private Button _receiverCancelButton;
    @FXML private Label _receiverOwnIP;
    @FXML private ProgressBar receiverProcessingFilePB;
    @FXML private ProgressBar _receiverReceivingFilePB;

    /*About tab fields*/
    @FXML private Label _aboutCopyright;
    @FXML Label javaInfo, osInfo, buildInfo, buildDate;

    private int PORT = 5050;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        /*About tab*/
        initAboutTab();

        /*Sender tab*/
        initSenderTab();
    }

    private void initAboutTab() {
        osInfo.setText(System.getProperty("os.name") + ", "+ System.getProperty("os.arch") +", "+ System.getProperty("os.version"));
        javaInfo.setText(System.getProperty("java.version") +", "+ System.getProperty("java.vendor"));
        buildInfo.setText("1.0.0");
        buildDate.setText("05-May-2019");
        int year = Calendar.getInstance().get(Calendar.YEAR);
        _aboutCopyright.setText("\u00a9"+" Suvankar Mitra "+year);
        _aboutCopyright.setAlignment(Pos.CENTER);
    }

    private void initSenderTab() {
        _senderCancelButton.setDisable(true);
        _senderReadyLabel.setAlignment(Pos.CENTER_RIGHT);

        try {
            this.senderLocalIP = Util.checkLocalIP();
            senderIPAddress = this.senderLocalIP;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            showError(e);
        }

        try {
            this.senderPublicIP = Util.checkPublicIP();
        } catch (IOException e) {
            e.printStackTrace();
            showError(e);
        }

        _senderLocalIPToggle.setOnAction(event -> {
            if(_senderLocalIPToggle.getText().equalsIgnoreCase("local-ip")) {
                _senderLocalIPToggle.setText("PUBLIC-IP");
                senderIPAddress = senderPublicIP;
            } else {
                _senderLocalIPToggle.setText("LOCAL-IP");
                senderIPAddress = senderLocalIP;
            }
            setIPAddress();
        });

        setIPAddress();

        _senderCopyIP.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            System.out.println("Copy to clipboard");
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(senderIPAddress);
            clipboard.setContent(content);
            event.consume();
            Toast.makeText((Stage) _senderCopyIP.getScene().getWindow(), "IP copied to clipboard",
                    1500,500,500);
        });

        _senderFileChooser.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File");
            File selectedFile = chooser.showOpenDialog(_senderFileChooser.getScene().getWindow());
            if(selectedFile != null) _senderFilePath.setText(selectedFile.getAbsolutePath());
        });

        AtomicReference<FTSender> ftSender = new AtomicReference<>();

        _sendButton.setOnAction(event -> {
            _sendButton.setDisable(true);
            _senderFileChooser.setDisable(true);
            _senderFilePath.setDisable(true);
            _senderPassword.setDisable(true);
            _senderCancelButton.setDisable(false);
            _senderBusyLight.setImage(new Image(getClass().getResourceAsStream("red_dark.png")));
            _senderReadyLabel.setText("BUSY");

            if(_senderFilePath.getText().isEmpty()) {
                showAlertWithHeaderText("File not selected", "Error", "You must choose a file to send!", Alert.AlertType.ERROR);
                resetSenderUI();
                return;
            } else {
                senderFilePath = _senderFilePath.getText().trim();
            }

            if(_senderPassword.getText().isEmpty()) {
                showAlertWithHeaderText("Password field empty","Error", "Password field must not be empty!", Alert.AlertType.ERROR);
                resetSenderUI();
                return;
            } else {
                senderPassword = _senderPassword.getText();
            }

            FTConnection ftConnection = new FTConnection();
            ftConnection.setIp(senderIPAddress);
            ftConnection.setPort(PORT);
            ftConnection.setPassword(senderPassword);

            new Thread(()->{
                try {
                    ftSender.set(new FTSender(ftConnection, senderFilePath));
                    Platform.runLater(()->_senderStatus.setText("Compressing file"));
                    ftSender.get().processFile();

                    final boolean[] lightChange = {true};
                    do {
                        double processFileProgress = ftSender.get().getProcessFileProgress();
                        Platform.runLater(()->{
                            _senderProcessFilePB.setProgress(processFileProgress);
                            _senderProcessFilePercentage.setText((int)(Math.ceil(processFileProgress*100))+"%");
                            if(lightChange[0]) {
                                _senderBusyLight.setImage(new Image(getClass().getResourceAsStream("red_dark.png")));
                                lightChange[0] = !lightChange[0];
                            } else {
                                _senderBusyLight.setImage(new Image(getClass().getResourceAsStream("red_light.png")));
                                lightChange[0] = !lightChange[0];
                            }
                        });
                        Thread.sleep(500);
                    } while (!ftSender.get().isProcessingFileDone() || senderTaskCancelled);

                    // Task is cancelled, return immediately
                    if(senderTaskCancelled) {
                        senderTaskCancelled = false;
                        return;
                    }
                    //Final state
                    Platform.runLater(()->{
                        double processFileProgress = ftSender.get().getProcessFileProgress();
                        _senderProcessFilePB.setProgress(processFileProgress);
                        _senderProcessFilePercentage.setText(Math.ceil(processFileProgress*100)+"%");
                        _senderReadyLabel.setText("WAITING");
                        _senderBusyLight.setImage(new Image(getClass().getResourceAsStream("yellow_dark.png")));
                    });

                    Platform.runLater(()->_senderStatus.setText("Done compressing file"));

                    Platform.runLater(()->_senderStatus.setText("Waiting for receiver to connect."));
                    ftSender.get().openConnectionAndWait();
                    String remoteIP = ftSender.get().getFtConnection().getRemoteIP();
                    Platform.runLater(()->_senderStatus.setText("Connected to "+remoteIP));

                } catch (Exception e) {
                    Platform.runLater(()->{
                        showError(e);
                    });
                    resetSenderUI();
                    e.printStackTrace();
                } finally {
                    try {
                        if (ftSender.get() != null) ftSender.get().close();
                    } catch (IOException e) {
                        showError(e);
                        e.printStackTrace();
                    }
                }
            }).start();
        });

        _senderCancelButton.setOnAction(event->{
            try {
                senderTaskCancelled = true;
                ftSender.get().close();
                resetSenderUI();
                Toast.makeText((Stage) _senderCancelButton.getScene().getWindow(),"Task cancelled",2000,500,500);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void resetSenderUI() {
        Platform.runLater(()->{
            _sendButton.setDisable(false);
            _senderFileChooser.setDisable(false);
            _senderFilePath.setDisable(false);
            _senderFilePath.setText("");
            _senderPassword.setDisable(false);
            _senderPassword.setText("");
            _senderProcessFilePB.setProgress(0d);
            _senderProcessFilePercentage.setText("0%");
            _senderCancelButton.setDisable(true);
            _senderBusyLight.setImage(new Image(getClass().getResourceAsStream("green_dark.png")));
            _senderReadyLabel.setText("READY");
            _senderStatus.setText("Status ready");
        });
    }

    private void setIPAddress() {
        Platform.runLater(()->{
            if(senderIPAddress!=null) {
                String ip[] = senderIPAddress.split("\\.");
                _senderRecIP1.setText(ip[0]); _senderRecIP2.setText(ip[1]);
                _senderRecIP3.setText(ip[2]); _senderRecIP4.setText(ip[3]);
            }
        });
    }

    // Show a Information Alert without Header Text
    private void showAlertWithHeaderText(String header, String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        // Header Text: null
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error alert");
        alert.setHeaderText(e.getMessage());

        VBox dialogPaneContent = new VBox();

        Label label = new Label("Stack Trace:");

        String stackTrace = Util.getStackTrace(e);
        TextArea textArea = new TextArea();
        textArea.setText(stackTrace);

        dialogPaneContent.getChildren().addAll(label, textArea);

        // Set content for Dialog Pane
        alert.getDialogPane().setContent(dialogPaneContent);

        alert.showAndWait();
    }
}
