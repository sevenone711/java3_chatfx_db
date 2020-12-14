package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private ListView<String> clientList;

    private Socket socket;
    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;
    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private File historyFile;

    public void setAuthenticated(boolean authenticated) throws IOException {
        this.authenticated = authenticated;
        msgPanel.setManaged(authenticated);
        msgPanel.setVisible(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        clientList.setManaged(authenticated);
        clientList.setVisible(authenticated);
        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
        //тут выводим историю чата
        addTextAreaHistorichat();



    }

    private void addTextAreaHistorichat () throws IOException {

        int lineNumber = 0;
        if (nickname != ""){
            if (setMessageHistoryFile().exists()){

                try (FileReader fr = new FileReader(setMessageHistoryFile())) {
                    LineNumberReader lnr = new LineNumberReader(fr);
                    while (lnr.readLine() != null) {
                        lineNumber++;

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(setMessageHistoryFile()))) {   //Такой способ дает автоматическое закрытие считывающего или записыувающего потока, тоесть не нобязательно применять close()

                if(lineNumber <= 100){

                    for (int i = 1; i <= lineNumber ; i++){
                        String line = "";
                        line = bufferedReader.readLine();

                        textArea.appendText(line +  "\n");
                    }
                } else {
                    int tempLineNumber = 0;
                    tempLineNumber = lineNumber - 100;
                    for (int i = 1; i <= lineNumber ; i++){
                        if(i > tempLineNumber){
                            String line = "";
                            line = bufferedReader.readLine();

                            textArea.appendText(line +  "\n");
                        }
                    }
                }




            }catch (IOException e){
                e.printStackTrace();
            }
            }else {
                historyFile.createNewFile();
            }
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createRegWindow();
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("bye");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        try {
            setAuthenticated(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/regok")) {
                                regController.addMessage("Регистрация прошла успешно");
                            }
                            if (str.equals("/regno")) {
                                regController.addMessage("Регистрация не получилась\n" +
                                        "Возможно предложенные лоин или никнейм уже заняты");
                            }

                            if (str.startsWith("/authok ")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);

                                break;
                            }

                            if(str.equals("/end")){
                                throw new RuntimeException("Сервер нас вырубил по таймауту");
                            }

                        } else {
                            textArea.appendText(str + "\n");
                            writeTextHistoriFile(str);
                        }
                    }

                    //Цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                            if (str.equals("/end")) {
                                break;
                            }
                        } else {
                            textArea.appendText(str + "\n");
                            writeTextHistoriFile(str);
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        setAuthenticated(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTextHistoriFile (String str) {
        try (BufferedWriter bufferedWriter =
                     new BufferedWriter(new FileWriter(setMessageHistoryFile(), true))) {

            bufferedWriter.write(str + "\n");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            login = loginField.getText().trim();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String username) {
        String title = String.format("СпэйсЧат [ %s ]", username);
        if (username.equals("")) {
            title = "СпэйсЧат";
        }
        String chatTitle = title;
        Platform.runLater(() -> {
            stage.setTitle(chatTitle);
        });
    }

    @FXML
    public void clickClientlist(MouseEvent mouseEvent) {
        String msg = String.format("/w %s ", clientList.getSelectionModel().getSelectedItem());
        textField.setText(msg);
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("СпэйсЧат Регистрация");
            regStage.setScene(new Scene(root, 350, 300));
            regStage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void showRegWindow(ActionEvent actionEvent) {
        regStage.show();
    }

    public void tryToReg(String login, String password, String nickname) {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File setMessageHistoryFile() {
        historyFile = new File("client/src/main/resources/hisotyfiles/history_" + login + ".txt");
        return historyFile;
    }
    }
