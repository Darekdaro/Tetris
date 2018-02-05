

package tetris;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;


public final class Tetris extends HBox {

    /**
     * Przechowuje, jeœli zosta³ naciœniêty klawisz strza³ki w dó³, aby zapobiec powtarzaj¹cym siê zdarzeniom.
     */
    private boolean movingDown = false;


    public Tetris() {

        setId("tetris");

        sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observableValue, Scene scene, Scene scene2) {
                if (scene2 != null) {
                    scene2.getStylesheets().add("tetris/styles.css");
                }
            }
        });
        final GameController gameController = new GameController();

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(gameController.getBoard());

        stackPane.getChildren().add(gameController.getNotificationOverlay());
        stackPane.setAlignment(Pos.TOP_CENTER);

        getChildren().add(stackPane);

        InfoBox infoBox = new InfoBox(gameController);
        infoBox.setMaxHeight(Double.MAX_VALUE);
        //poni¿sza linijka umo¿liwia pe³en rozmiar gry
        infoBox.maxHeightProperty().bind(gameController.getBoard().heightProperty());

        HBox.setHgrow(infoBox, Priority.ALWAYS);
        getChildren().add(infoBox);

        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.P) {
                    gameController.pausedProperty().set(!gameController.pausedProperty().get());
                    keyEvent.consume();
                }
            }
        });
        setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent mouseEvent) {
                gameController.getBoard().requestFocus();

            }
        });

        setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent keyEvent) {

                if (keyEvent.getCode() == KeyCode.LEFT && !gameController.pausedProperty().get()) {
                    gameController.getBoard().move(HorizontalDirection.LEFT);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.RIGHT && !gameController.pausedProperty().get()) {
                    gameController.getBoard().move(HorizontalDirection.RIGHT);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.UP && !gameController.pausedProperty().get()) {
                    gameController.getBoard().rotate(HorizontalDirection.LEFT);
                    keyEvent.consume();
                }

                if (keyEvent.getCode() == KeyCode.DOWN) {
                    if (!movingDown) {
                        if (!gameController.pausedProperty().get()) {
                            gameController.getBoard().moveDownFast();
                        }
                        movingDown = true;
                        keyEvent.consume();
                    }
                }
                if (keyEvent.getCode() == KeyCode.SPACE && !gameController.pausedProperty().get()) {
                    gameController.getBoard().dropDown();
                    keyEvent.consume();
                }
            }
        });

        setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.DOWN) {
                    movingDown = false;
                    gameController.getBoard().moveDown();
                }
            }
        });

    }
}
