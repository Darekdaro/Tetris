
package tetris;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HorizontalDirection;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;


final class SoundManager implements Board.BoardListener {

    private final MediaPlayer mediaPlayer1;

    private DoubleProperty volume = new SimpleDoubleProperty();

    private DoubleProperty soundVolume = new SimpleDoubleProperty();

    private BooleanProperty mute = new SimpleBooleanProperty();

    public SoundManager(GameController gameController) {
        mediaPlayer1 = new MediaPlayer(new Media(getClass().getResource("/tetris/Alla Turca Turkish March.mp3").toExternalForm()));
        mediaPlayer1.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer1.volumeProperty().bind(volumeProperty());
        mediaPlayer1.muteProperty().bind(muteProperty());
        gameController.getBoard().addBoardListener(this);

        for (Sound sound : Sound.values()) {
            sound.getAudioClip().volumeProperty().bind(SoundManager.this.soundVolumeProperty());
        }
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public DoubleProperty soundVolumeProperty() {
        return soundVolume;
    }

    public BooleanProperty muteProperty() {
        return mute;
    }

    public void pause() {
        mediaPlayer1.pause();
    }

    public void play() {
        mediaPlayer1.play();
    }

    public void stop() {
        mediaPlayer1.stop();
    }

    public void playFromStart() {
        mediaPlayer1.stop();
        mediaPlayer1.play();
    }

    public void onDropped() {
        if (!mute.get()) {
            Sound.DROPPED.getAudioClip().play();
        }
    }

    @Override
    public void onRowsEliminated(int rows) {
        if (!mute.get()) {
            if (rows < 4) {
                Sound.VANISH.getAudioClip().play();
                Sound.VANISH2.getAudioClip().play();
            } else {
                Sound.TETRIS.getAudioClip().play();
            }
        }
    }

    @Override
    public void onGameOver() {
        mediaPlayer1.stop();
        if (!mute.get()) {
            Sound.GAME_OVER.getAudioClip().play();
        }
    }

    @Override
    public void onInvalidMove() {
        if (!mute.get()) {
            Sound.INVALID_MOVE.getAudioClip().play();
        }
    }

    @Override
    public void onMove(HorizontalDirection horizontalDirection) {
        if (!mute.get()) {
            Sound.MOVE.getAudioClip().play();
        }
    }

    @Override
    public void onRotate(HorizontalDirection horizontalDirection) {
        if (!mute.get()) {
            Sound.ROTATE.getAudioClip().play();
        }
    }

    private enum Sound {

        ROTATE("tetris/cartoon130.wav"),
        TETRIS("tetris/cartoon034.mp3"),
        DROPPED("tetris/cartoon035.wav"),
        INVALID_MOVE("tetris/cartoon155.mp3"),
        MOVE("tetris/cartoon136.wav"),
        VANISH("tetris/cartoon017.mp3"),
        VANISH2("tetris/pop-Oliverev-8163_hifi.mp3"),
        GAME_OVER("tetris/cartoon014.mp3");

        private AudioClip audioClip;

        private Sound(String name) {
            audioClip = new AudioClip(getClass().getResource("/" + name).toExternalForm());
        }

        public AudioClip getAudioClip() {
            return audioClip;
        }

    }
}
