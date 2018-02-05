
package tetris;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 *Podgl¹d nastêpnego tetromino.

 */
final class Preview extends StackPane {

    private Map<Tetromino, Node> cloneToNode = new HashMap<>();

    private Map<Tetromino, Tetromino> tetrominoToClone = new HashMap<>();

    public Preview(GameController gameController) {

        final ObservableList<Tetromino> tetrominos = gameController.getBoard().getWaitingTetrominos();

        tetrominos.addListener(new ListChangeListener<Tetromino>() {
            @Override
            public void onChanged(Change<? extends Tetromino> change) {

                while (change.next()) {
                    if (change.wasRemoved()) {
                        for (final Tetromino tetromino : change.getRemoved()) {
                            final Tetromino clone = tetrominoToClone.remove(tetromino);
                            final Node group = cloneToNode.remove(clone);
                            FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(0.1), group);
                            fadeOutTransition.setToValue(0);
                            fadeOutTransition.setFromValue(1);
                            fadeOutTransition.setOnFinished(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent actionEvent) {
                                    getChildren().remove(group);
                                }
                            });
                            fadeOutTransition.playFromStart();
                        }
                    }
                    if (change.wasAdded()) {
                        if (change.getList().size() == 1) return;
                        for (Tetromino added : change.getAddedSubList()) {

                            SequentialTransition sequentialTransition = new SequentialTransition();
                            Tetromino clone = added.clone();

                            Group group = new Group();
                            DropShadow dropShadow = new DropShadow();
                            dropShadow.setColor(Color.DARKGREY);
                            dropShadow.setRadius(20);
                            group.setEffect(dropShadow);
                            group.setOpacity(0);
                            group.getChildren().add(clone);
                            getChildren().add(group);
                            //group.setScaleX(0);
                            //group.setScaleY(0);
                            //g/roup.setScaleZ(0);
                            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(0.2), group);
                            scaleTransition.setFromX(0);
                            scaleTransition.setFromZ(0);
                            scaleTransition.setFromY(0);

                            scaleTransition.setToX(1);
                            scaleTransition.setToZ(1);
                            scaleTransition.setToY(1);

                            //scaleTransition.play();

                            FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.1), group);
                            fadeTransition.setFromValue(0);
                            fadeTransition.setToValue(1);
                            sequentialTransition.getChildren().add(new PauseTransition(Duration.seconds(0.1)));
                            sequentialTransition.getChildren().add(fadeTransition);
                            sequentialTransition.playFromStart();

                            tetrominoToClone.put(added, clone);
                            cloneToNode.put(clone, group);
                        }


                    }
                }
                //                if (gameController.getBoard().getTetr)

            }
        });

        setPrefHeight(140);
        setPrefWidth(140);
        setAlignment(Pos.CENTER);

        if (!tetrominos.isEmpty()) {
            getChildren().addAll(tetrominos.get(0));
        }

    }
}
