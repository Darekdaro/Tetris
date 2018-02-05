

package tetris;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HorizontalDirection;

/**
 * Manages the score.
 *

 */
final class ScoreManager implements Board.BoardListener {

    private final IntegerProperty score = new SimpleIntegerProperty();

    private final GameController gameController;

    public ScoreManager(GameController gameController) {
        this.gameController = gameController;
        gameController.getBoard().addBoardListener(this);
    }

    public IntegerProperty scoreProperty() {
        return score;
    }

    private void addScore(int score) {
        this.score.set(this.score.get() + score);
    }

    @Override
    public void onDropped() {
    }

    @Override
    public void onRowsEliminated(int rows) {
        switch (rows) {
            case 1:
                addScore(50);
                break;
            case 2:
                addScore(100);
                break;
            case 3:
                addScore(350);
                break;
            case 4:
                addScore(1000);
                break;
        }
    }

    @Override
    public void onGameOver() {
    }

    @Override
    public void onInvalidMove() {
    }

    @Override
    public void onMove(HorizontalDirection horizontalDirection) {
    }

    @Override
    public void onRotate(HorizontalDirection horizontalDirection) {
    }

	public GameController getGameController() {
		return gameController;
	}
}
