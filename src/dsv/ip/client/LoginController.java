package dsv.ip.client;

import dsv.ip.shared.status.ConnectionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Fredrik Sander
 * Partially auto-generated by Intellij's UI JavaFX designer.
 *
 * LoginController is responsible for controlling the login panels UI.
 */
public class LoginController implements Initializable {
  private XmlRpcClient connection;

  @FXML
  public Label connectionStatus;

  @FXML
  public TextField nameField;

  private boolean connectionInProgress = false;
  private Executor executor = Executors.newCachedThreadPool();
  private ConnectionStatus connectionState = ConnectionStatus.NOT_CONNECTED;

  /**
   * Initializes the controller for the login view.
   * Prepares the XmlRPCClient connection.
   */
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    try {
      connection = new XmlRpcClient(Main.RPCConnectionURL);
    } catch (MalformedURLException e) { e.printStackTrace(); }
  }

  /**
   * Callback for when the connect button is clicked.
   * Attempts to connect to the GamePlatform server.
   */
  @FXML
  private void connectButtonClicked() {
    if (connectionInProgress)
      return;

    connectionInProgress = true;

    changeStatusText("Attempting to connect", true);
    CompletableFuture future = CompletableFuture.runAsync(() -> {
      Vector arguments = new Vector();
      arguments.add(nameField.getText());

      try {
        int response = (int)connection.execute("connection_handler.connect", arguments);
        connectionState = ConnectionStatus.values()[response];
      } catch (XmlRpcException e) {
        e.printStackTrace();
      } catch (IOException e) { }

      connectionInProgress = false;
    }, executor);

    future.thenRun (() -> {

      // Avoid executing operations on non-java FX thread
      Platform.runLater(() -> {
        switch (connectionState) {
          case SUCCESS:
            handleConnectionSuccess();
            break;
          case INVALID_NAME:
            handleInvalidName();
            break;
          case NOT_CONNECTED:
            handleConnectionError();
            break;
          default:
            break;
        }
      });
    });
  }

  /**
   * Sets the stauts text in the Login UI
   *
   * @param newStatus The new message to display.
   * @param active Whether it's an ongoing (active) status or not.
   */
  private void changeStatusText(String newStatus, boolean active) {
    String status = "Status: " + newStatus;
    status += active ? "..." : ".";
    connectionStatus.setText(status);
  }

  /**
   * Responsible for handling the scenario when the user tries to connect with a invalid name.
   * Notifies the user that the name used to connect is not accepted by the server.
   */
  private void handleInvalidName() {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Connection error");
    alert.setHeaderText("Invalid Name");
    alert.setContentText("You have entered an invalid name, please try again.");
    changeStatusText("error invalid name", false);
  }

  /**
   * Responsible for handling the scenario when the connection is successful.
   * Changes scene to the GamePlatform UI.
   */
  private void handleConnectionSuccess() {
    changeStatusText("successfully connected", false);
    Session.getInstance().setUsername(nameField.getText().toLowerCase());

    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("PlatformUI.fxml"));
      Parent root = (Parent)loader.load();

      PlatformController controller = loader.getController();
      controller.setName(nameField.getText());

      Scene scene = new Scene(root);
      Stage stage = (Stage) nameField.getScene().getWindow();
      stage.setScene(scene);
      stage.show();
    } catch (IOException e) { e.printStackTrace(); }
  }

  /**
   * Called when a connection error occurs in the login screen.
   * Shows a alert for the user to notify him/her of the error.
   */
  private void handleConnectionError() {
    Alert alert = new Alert(Alert.AlertType.ERROR, "Could not connect to server! Try again...");
    alert.setTitle("Connection Error");
    alert.show();
  }
}