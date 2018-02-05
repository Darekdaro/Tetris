

package tetris;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


final class GameController {

    private final SoundManager soundManager;

    private final Board board;

    private final NotificationOverlay notificationOverlay;

    private final ScoreManager scoreManager;

    private final BooleanProperty paused = new SimpleBooleanProperty();

    public GameController() {
        this.board = new Board();
        this.soundManager = new SoundManager(this);
        this.scoreManager = new ScoreManager(this);

        notificationOverlay = new NotificationOverlay(this);
        paused.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                if (aBoolean2) {
                    pause();
                } else {
                    play();
                }
            }
        });
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public void start() {
        soundManager.playFromStart();
        board.start();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    private void pause() {
        soundManager.pause();
        board.pause();
    }

    public void stop() {
        soundManager.stop();
        board.clear();
        scoreManager.scoreProperty().set(0);
        paused.set(false);
    }

    public Board getBoard() {
        return board;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public void play() {
        paused.set(false);
        soundManager.play();
        board.play();
    }

    public NotificationOverlay getNotificationOverlay() {
        return notificationOverlay;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }
}
