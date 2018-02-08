
package tetris;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.Light;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * klasa reprezentuj�ca g��wn� tablic�
 *

 */
final class Board extends StackPane {

	/**
    * Liczba ukrytych rz�d�w, kt�re znajduj� si� niewidoczne nad plansz�.
    * Jest to obszar, w kt�rym pojawiaj� si� tetrominos.
    * Domy�lnie s� 2.
    */
    private static final byte HIDDEN_ROWS = 2;

    /**
     * Liczba blok�w na wiersz, domy�lnie jest to 10.
     */
    private static final byte BLOCKS_PER_ROW = 10;

    /**
     * Liczba blok�w na kolumn�. Domy�lnie jest to 20.
     */
    private static final byte BLOCKS_PER_COLUMN = 20;

    /**
     * Liczba maksymalnych podgl�d�w.
     */
    private static final byte MAX_PREVIEWS = 1;

    /**
     *Przej�cie w d�.
     */
    private final TranslateTransition moveDownTransition;

    /**
     * przej�cie rotacyjne.
     */
    private final RotateTransition rotateTransition;

    /**
     * Sekwencyjne przej�cie, kt�re sk�ada si� z {@link #moveDownTransition} i {@link PauseTransition}.
     */
    private final SequentialTransition moveTransition;

    /**
     * Przej�cie, kt�re pozwala kawa�kowi przej�� szybko.
     */
    private final TranslateTransition moveDownFastTransition;

    /**
     * Transformacja przej�cia dla ruchu w lewo / w prawo.
     */
    private final TranslateTransition translateTransition;

    /**
     * Zestaw bie��cych przej��. Wszystkie uruchomione przej�cia s� wstrzymane, gdy gra jest wstrzymana.
     */
    private final Set<Animation> runningAnimations = new HashSet<>();

    /**
     * Dwuwymiarowa tablica, kt�ra definiuje plansz�. Je�li element jest pusty w macierzy, jest pusty, w przeciwnym razie jest zaj�ty.
     */
    private final Rectangle[][] matrix = new Rectangle[BLOCKS_PER_COLUMN + HIDDEN_ROWS][BLOCKS_PER_ROW];

    /**
     * Lista tetrominos, kt�re b�d� nast�pne.
     */
    private final ObservableList<Tetromino> waitingTetrominos = FXCollections.observableArrayList();

    /**
     * Bardzo szybkie przej�cie w d�.
     */
    private final TranslateTransition dropDownTransition;

    /**
     * Przechowuje, je�li naci�ni�ty jest klawisz w d�. Tak d�ugo, jak jest to mo�liwe, odtwarzane jest {@link #moveDownFastTransition}.
     */
    private boolean moving = false;

    /**
     * Bie��ca pozycja x i y z macierz� bie��cego tetromino.
     */
    private int x = 0, y = 0;

    /**
     * To prawda, podczas gdy tetromino jest upuszczane (za pomoc� klawisza spacji).
     */
    private boolean isDropping = false;

    /**
     * Obecne tetromino, kt�re spada.
     */
    private Tetromino currentTetromino;

    /**
     * Trzyma s�uchaczy tablicy.
     */
    private List<BoardListener> boardListeners = new CopyOnWriteArrayList<>();

    private DoubleProperty squareSize = new SimpleDoubleProperty();

    /**
     * tworzenie tablicy.
     */
    public Board() {
        setFocusTraversable(true);


        
        setId("board");
        setMinWidth(35 * BLOCKS_PER_ROW);
        setMinHeight(35 * BLOCKS_PER_COLUMN);

        maxWidthProperty().bind(minWidthProperty());
        maxHeightProperty().bind(minHeightProperty());
        
        /*poni�szy kod umo�liwia edycj� wielko�ci tablicy jako dodatek */      
        
       /* setStyle("-fx-border-color:red");
                minHeightProperty().bind(new DoubleBinding() {
                    {
                       super.bind(widthProperty());
                    }
       
                    @Override
                    protected double computeValue() {
                        return getWidth() * BLOCKS_PER_COLUMN / BLOCKS_PER_ROW;
                    }
                });
                maxHeightProperty().bind(minHeightProperty());*/

        
        
        
        
        clipProperty().bind(new ObjectBinding<Node>() {
            {
                super.bind(widthProperty(), heightProperty());
            }

            @Override
            protected Node computeValue() {
                return new Rectangle(getWidth(), getHeight());
            }
        });

        setAlignment(Pos.TOP_LEFT);

        // Inicjalizuj przej�cie w d�. szybko�c przej�cia
        moveDownTransition = new TranslateTransition(Duration.seconds(0.3));
        moveDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moving = false;
                y++;
            }
        });

        // Po przesuni�ciu si� kawa�ka, poczekaj chwil�, a� znowu si� poruszy.
        PauseTransition pauseTransition = new PauseTransition();
        pauseTransition.durationProperty().bind(moveDownTransition.durationProperty());

        moveTransition = new SequentialTransition();
        moveTransition.getChildren().addAll(moveDownTransition, pauseTransition);
        moveTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moveDown();
            }
        });

        // Ten ruch powinien by� pauzowany
        registerPausableAnimation(moveTransition);

        // Powoduje szybkie przesuni�cie elementu.
        moveDownFastTransition = new TranslateTransition(Duration.seconds(0.08));
        // Aby sprawi�, by wygl�da� bardziej p�ynnie, u�yj interpolatora liniowego.
        moveDownFastTransition.setInterpolator(Interpolator.LINEAR);
        moveDownFastTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                y++;
                moveDownFast();
            }
        });
        registerPausableAnimation(moveDownFastTransition);

        // Przesuwa element w lewo i w prawo ..
        translateTransition = new TranslateTransition(Duration.seconds(0.1));
        registerPausableAnimation(translateTransition);

        // rotacja w miejscu.
        rotateTransition = new RotateTransition(Duration.seconds(0.1));
        dropDownTransition = new TranslateTransition(Duration.seconds(0.1));
        dropDownTransition.setInterpolator(Interpolator.EASE_IN);

        squareSize.bind(new DoubleBinding() {
            {
                super.bind(widthProperty());
            }

            @Override
            protected double computeValue() {
                return getWidth() / BLOCKS_PER_ROW;
            }
        });
    }

    /**
Rejestruje animacj�, kt�ra jest dodawana do listy uruchomionych animacji, je�li jest uruchomiona, i jest ponownie usuwana, je�li jest zatrzymana.
      * Gdy gra si� zatrzyma, wszystkie uruchomione animacje zostan� wstrzymane.
      *
      * animacja @param Animacja.
     */
    private void registerPausableAnimation(final Animation animation) {
        animation.statusProperty().addListener(new ChangeListener<Animation.Status>() {
            @Override
            public void changed(ObservableValue<? extends Animation.Status> observableValue, Animation.Status status, Animation.Status status2) {
                if (status2 == Animation.Status.STOPPED) {
                    runningAnimations.remove(animation);
                } else {
                    runningAnimations.add(animation);
                }
            }
        });
    }

    /**
     * Odradza nowe losowe tetromino.
     */
    private void spawnTetromino() {

        // Wype�nij kolejk� oczekuj�cych tetrominos, je�li jest pusta.
        while (waitingTetrominos.size() <= MAX_PREVIEWS) {
            waitingTetrominos.add(Tetromino.random(squareSize));
        }

        // Usu� pierwsz� z kolejki i odrodz j�.
        currentTetromino = waitingTetrominos.remove(0);

        // Zresetuj wszystkie przej�cia.
        rotateTransition.setNode(currentTetromino);
        rotateTransition.setToAngle(0);

        translateTransition.setNode(currentTetromino);
        moveDownTransition.setNode(currentTetromino);
        moveDownFastTransition.setNode(currentTetromino);

        // Dodaj bie��ce tetromino na plansz�.
        getChildren().add(currentTetromino);

     // Przenie� go do w�a�ciwej pozycji
        // Od�� tetromino w �rodku (I, O) lub w lewym �rodku (kolejne kszta�ty klock�w).
        x = (matrix[0].length - currentTetromino.getMatrix().length) / 2;
        y = 0;
        // Przet�umacz tetromino na pozycj� wyj�ciow�.
        currentTetromino.setTranslateY((y - Board.HIDDEN_ROWS) * getSquareSize());
        currentTetromino.setTranslateX(x * getSquareSize());
        

        //translateTransition.setToX(currentTetromino.getTranslateX());

        
        // Zacznij go przenosi�.
        moveDown();
    }

    /**
     * Powiadomienie o tetrominie, �e nie mo�e przej�� dalej.
     */
    private void tetrominoDropped() {
        if (y == 0) {
            // Je�li utw�r nie m�g� si� ruszy� i nadal znajdujemy si� w pocz�tkowej pozycji y, gra si� sko�czy�a.
            currentTetromino = null;
            waitingTetrominos.clear();
            notifyGameOver();
        } else {
            mergeTetrominoWithBoard();
        }
    }

    /**
     * Powiadamia s�uchacza, �e kawa�ek odpad�.
     */
    private void notifyOnDropped() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onDropped();
        }
    }

    /**
     * Powiadamia s�uchacza, �e gra si� sko�czy�a.
     */
    private void notifyGameOver() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onGameOver();
        }
    }

    /**
     *
     */
    private void notifyOnMove(HorizontalDirection horizontalDirection) {

        for (BoardListener boardListener : boardListeners) {
            boardListener.onMove(horizontalDirection);
        }
    }

    /**
     * Powiadamia s�uchaczy, �e wiersze zosta�y wyeliminowane.
     *
     * @param numer wiersza.
     */
    private void notifyOnRowsEliminated(int rows) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRowsEliminated(rows);
        }
    }

    /**
     * Powiadamia s�uchaczy, �e pr�bowano wykona� niewa�ny ruch.
     */
    private void notifyInvalidMove() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onInvalidMove();
        }
    }

    /**
     * Powiadamia s�uchaczy, �e pr�bowano wykona� niewa�ny ruch.
     */
    private void notifyRotate(HorizontalDirection horizontalDirection) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRotate(horizontalDirection);
        }
    }

    /**
     * Scala tetromino z plansz�.
     * Dla ka�dej p�ytki utw�rz prostok�t na planszy.
     * W ko�cu usuwa tetromino z planszy i spawnuje now�.
     */
    private void mergeTetrominoWithBoard() {
        int[][] tetrominoMatrix = currentTetromino.getMatrix();

        for (int i = 0; i < tetrominoMatrix.length; i++) {
            for (int j = 0; j < tetrominoMatrix[i].length; j++) {

                final int x = this.x + j;
                final int y = this.y + i;

                if (tetrominoMatrix[i][j] == 1 && y < BLOCKS_PER_COLUMN + HIDDEN_ROWS && x < BLOCKS_PER_ROW) {
                    final Rectangle rectangle = new Rectangle();

                    ChangeListener<Number> changeListener = new ChangeListener<Number>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                            rectangle.setWidth(number2.doubleValue());
                            rectangle.setHeight(number2.doubleValue());
                            rectangle.setTranslateX(number2.doubleValue() * x);
                            rectangle.setTranslateY(number2.doubleValue() * ((Integer) rectangle.getProperties().get("y")));
                        }
                    };
                    squareSize.addListener(new WeakChangeListener<>(changeListener));
                    rectangle.setUserData(changeListener);
                    rectangle.getProperties().put("y", y - HIDDEN_ROWS);
                    rectangle.setWidth(squareSize.doubleValue());
                    rectangle.setHeight(squareSize.doubleValue());
                    rectangle.setTranslateX(squareSize.doubleValue() * x);
                    rectangle.setTranslateY(squareSize.doubleValue() * ((Integer) rectangle.getProperties().get("y")));

                    rectangle.setFill(currentTetromino.getFill());
                    ((Light.Distant) currentTetromino.getLighting().getLight()).azimuthProperty().set(225);
                    rectangle.setEffect(currentTetromino.getLighting());

                    rectangle.setArcHeight(7);
                    rectangle.setArcWidth(7);
                    // Przypisz prostok�t do macierzy tablicy.
                    matrix[y][x] = rectangle;
                    getChildren().add(rectangle);
                }
            }
        }

        ParallelTransition fallRowsTransition = new ParallelTransition();
        ParallelTransition deleteRowTransition = new ParallelTransition();
        int fall = 0;

        for (int i = y + currentTetromino.getMatrix().length - 1; i >= 0; i--) {
            if (i < matrix.length) {
                boolean rowComplete = i >= y;

                // Za��my, �e wiersz jest kompletny. Udowodnijmy, �e jest odwrotnie.
                if (rowComplete) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        if (matrix[i][j] == null) {
                            rowComplete = false;
                            break;
                        }
                    }
                }
                if (rowComplete) {
                    deleteRowTransition.getChildren().add(deleteRow(i));
                    fall++;
                } else if (fall > 0) {
                    fallRowsTransition.getChildren().add(fallRow(i, fall));
                }
            }
        }
        final int f = fall;
        fallRowsTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                notifyOnDropped();
            }
        });

        // Je�li przynajmniej jeden wiersz zosta� wyeliminowany.
        if (f > 0) {
            notifyOnRowsEliminated(f);
        }
        final SequentialTransition sequentialTransition = new SequentialTransition();
        sequentialTransition.getChildren().add(deleteRowTransition);
        sequentialTransition.getChildren().add(fallRowsTransition);
        sequentialTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
            	
            	
                //sequentialTransition.getChildren().clear();
                
            	
            	spawnTetromino();
            }
        });
     // Buforowane pami�ci w�z��w
        
        
        //https://javafx-jira.kenai.com/browse/RT-32733
        //currentTetromino.setCache(false);
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        registerPausableAnimation(sequentialTransition);
        sequentialTransition.playFromStart();
        notifyOnDropped();
    }

    /**
     * @param i  indeks wiersza.
     * @param przez Liczbe wierszy.
     * @return Przej�cie, kt�re o�ywia spadaj�cy rz�d.
     */
    private Transition fallRow(final int i, final int by) {
        ParallelTransition parallelTransition = new ParallelTransition();

        if (by > 0) {
            for (int j = 0; j < matrix[i].length; j++) {
                final Rectangle rectangle = matrix[i][j];

                if (rectangle != null) {
                	// Odwr�� pierwotn� pozycj� y, aby umo�liwi� przej�cie prostok�ta do nowego.
                    //rectangle.translateYProperty().unbind();
                    final TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.1), rectangle);
                    rectangle.getProperties().put("y", i - HIDDEN_ROWS + by);

                    translateTransition.toYProperty().bind(squareSize.multiply(i - HIDDEN_ROWS + by));
                    translateTransition.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent actionEvent) {
                            translateTransition.toYProperty().unbind();

                            //rectangle.translateYProperty().bind(squareSize.multiply(i - HIDDEN_ROWS + by));
                        }
                    });
                    parallelTransition.getChildren().add(translateTransition);
                }
                matrix[i + by][j] = rectangle;
            }
        }
        return parallelTransition;
    }

    /**
     * Usuwa wiersz na planszy.
     *
     * @param indeksy wiersza
     * @return Przej�cie, kt�re animuje usuwanie wiersza.
     */
    private Transition deleteRow(int rowIndex) {

        ParallelTransition parallelTransition = new ParallelTransition();

        for (int i = rowIndex; i >= 0; i--) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                if (i > 1) {
                    final Rectangle rectangle = matrix[i][j];

                    if (i == rowIndex && rectangle != null) {
                        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(0.27), rectangle);
                        fadeTransition.setToValue(0);
                        fadeTransition.setCycleCount(3);
                        fadeTransition.setAutoReverse(true);
                        fadeTransition.setOnFinished(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent actionEvent) {
                                getChildren().remove(rectangle);
                            }
                        });
                        parallelTransition.getChildren().add(fadeTransition);
                    }

                }
            }
        }
        return parallelTransition;
    }

    /**
     * Czy�ci plansz� i czekaj�ce tetrominos.
     */
    public void clear() {
        for (int i = 0; i < BLOCKS_PER_COLUMN + HIDDEN_ROWS; i++) {
            for (int j = 0; j < BLOCKS_PER_ROW; j++) {
                matrix[i][j] = null;
            }
        }
        getChildren().clear();
        getChildren().remove(currentTetromino);
        currentTetromino = null;
        waitingTetrominos.clear();
    }

    /**
     * Oblicza, czy tetromino przecina�oby si� z tablic�,
     * * przekazuj�c matryc�, kt�r� b�dzie mie� tetromino.
     * <p/>
     * * Przecina si�, je�li uderzy w kolejne tetromino lub przekroczy granic� lew�, praw� lub doln�.
     *
     * @param targetMatrix Macierz tetromino.
     * @param targetX      docelowa pozycja X 
     * @param targetY      docelowa pozycjat Y 
     * @return * @return Prawda, je�li przecina si� z plansz�, w przeciwnym razie jest fa�szywa.
     */
    private boolean intersectsWithBoard(final int[][] targetMatrix, int targetX, int targetY) {
        Rectangle[][] boardMatrix = matrix;

        for (int i = 0; i < targetMatrix.length; i++) {
            for (int j = 0; j < targetMatrix[i].length; j++) {

                boolean boardBlocks = false;
                int x = targetX + j;
                int y = targetY + i;

                if (x < 0 || x >= boardMatrix[i].length || y >= boardMatrix.length) {
                    boardBlocks = true;
                } else if (boardMatrix[y][x] != null) {
                    boardBlocks = true;
                }

                if (boardBlocks && targetMatrix[i][j] == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Uruchamia plansz� poprzez utworzenie nowego tetromino.
     */
    public void start() {
        clear();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        spawnTetromino();
    }

    /**
     * Upuszcza tetromino w d� do nast�pnej mo�liwej pozycji.
     */
    public void dropDown() {
        if (currentTetromino == null) {
            return;
        }

        moveTransition.stop();
        moveDownFastTransition.stop();
        dropDownTransition.stop();

        do {
            y++;
        }
        while (!intersectsWithBoard(currentTetromino.getMatrix(), x, y));
        y--;
        isDropping = true;
        dropDownTransition.setNode(currentTetromino);
        dropDownTransition.toYProperty().bind(squareSize.multiply(y - Board.HIDDEN_ROWS));
        dropDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                isDropping = false;
                tetrominoDropped();
            }
        });
        registerPausableAnimation(dropDownTransition);
        dropDownTransition.playFromStart();

    }

    /**
     * Pr�buje obr�ci� tetromino.
     *
     * @param kierunek poziomy
     * @return To prawda, je�li rotacja zako�czy�a si� powodzeniem, w przeciwnym razie jest nieprawda.
     */
    public boolean rotate(final HorizontalDirection direction) {
        boolean result = false;
        if (currentTetromino == null) {
            result = false;
        } else {
            int[][] matrix = currentTetromino.getMatrix();

            int[][] newMatrix = new int[matrix.length][matrix.length];


            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (direction == HorizontalDirection.RIGHT) {
                        newMatrix[j][matrix.length - 1 - i] = matrix[i][j];
                    } else {
                        newMatrix[matrix[i].length - 1 - j][i] = matrix[i][j];
                    }
                }
            }

            if (!intersectsWithBoard(newMatrix, x, y)) {
                currentTetromino.setMatrix(newMatrix);

                int f = direction == HorizontalDirection.RIGHT ? 1 : -1;

                rotateTransition.setFromAngle(rotateTransition.getToAngle());
                rotateTransition.setToAngle(rotateTransition.getToAngle() + f * 90);

                KeyValue kv = new KeyValue(((Light.Distant) currentTetromino.getLighting().getLight()).azimuthProperty(), 360 - 225 + 90 - rotateTransition.getToAngle());
                KeyFrame keyFrame = new KeyFrame(rotateTransition.getDuration(), kv);
                Timeline lightingAnimation = new Timeline(keyFrame);

                final ParallelTransition parallelTransition = new ParallelTransition(rotateTransition, lightingAnimation);
                registerPausableAnimation(parallelTransition);
                parallelTransition.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        
                        parallelTransition.getChildren().clear();
                    }
                });
                parallelTransition.playFromStart();
                result = true;
            }
        }

        if (!result) {
            notifyInvalidMove();
        } else {
            notifyRotate(direction);
        }
        return result;
    }

    /**
     * Przenosi tetromino na lewo lub prawo.
     *
     * @param oziomy kierunek
     * @return To prawda, je�li ruch si� powi�d�. Fa�sz, je�li ruch zosta� zablokowany przez plansz�.
     */
    public boolean move(final HorizontalDirection direction) {
        boolean result;
        if (currentTetromino == null || isDropping) {
            result = false;
        } else {
            int i = direction == HorizontalDirection.RIGHT ? 1 : -1;
            x += i;
            //Je�li si� nie porusza, sprawd� tylko aktualn� pozycj� y.
            // Je�li si� porusza, sprawd� tak�e docelow� pozycj� y.
            if (!moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) || moving && !intersectsWithBoard(currentTetromino.getMatrix(), x, y) && !intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                translateTransition.toXProperty().unbind();
                translateTransition.toXProperty().bind(squareSize.multiply(x));
                translateTransition.playFromStart();
                result = true;
            } else {
                x -= i;
                result = false;
            }
        }
        if (!result) {
            notifyInvalidMove();
        } else {
            notifyOnMove(direction);
        }
        return result;
    }

    /**
     * Przenosi tetromino o jedno pole w d�.
     */
    public void moveDown() {
        if (!isDropping && currentTetromino != null) {
            moveDownFastTransition.stop();
            moving = true;

            // Je�li jest w stanie przej�� do nast�pnej pozycji y, zr�b to!
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1) && !isDropping) {
                //moveDownTransition.setFromY(moveDownTransition.getNode().getTranslateY());
                moveDownTransition.toYProperty().unbind();
                moveDownTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveTransition.playFromStart();
            } else {
                tetrominoDropped();
            }
        }
    }

    /**
     * Szybko przesuwa bie��ce tetromino, je�li nie spada.
     */
    public void moveDownFast() {
        if (!isDropping) {

            // Zatrzymaj normalne przej�cie przej�cia.
            moveTransition.stop();
            // Nast�pnie sprawd�, czy nast�pna pozycja nie przecina�aby si� z desk�.
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                // Je�li mo�e si� rusza�, ruszaj!
                moveDownFastTransition.toYProperty().unbind();
                moveDownFastTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveDownFastTransition.playFromStart();
            } else {
                // W innym przypadku osi�gn�� on ziemi�.
                tetrominoDropped();
            }
        }
    }

    /**
     * pauza planszy
     *
     * @see #play()
     */
    public void pause() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.RUNNING) {
                animation.pause();
            }
        }
    }

    /**
     *Odtwarza plansz� ponownie, po jej zatrzymaniu.
     *
     * @see #pause()
     */
    public void play() {
        for (Animation animation : runningAnimations) {
            if (animation.getStatus() == Animation.Status.PAUSED) {
                animation.play();
            }
        }
        requestFocus();
    }

    /**
     * Dostaje oczekuj�ce tetrominosy, kt�re maj� zosta� odrodzone.
     * <p/>
     * Pierwszy element zostanie odrodzony w nast�pnej kolejno�ci.
     *
     * @return Lista oczekuj�cych tetrominos.
     */
    public ObservableList<Tetromino> getWaitingTetrominos() {
        return waitingTetrominos;
    }

    public double getSquareSize() {
        return squareSize.get();
    }

    /**
     * Dodaje s�uchacza do tablicy, kt�ra otrzymuje powiadomienia o okre�lonych wydarzeniach.
     *
     * @param s�uchacz tablicy
     */
    public void addBoardListener(BoardListener boardListener) {
        boardListeners.add(boardListener);
    }

    /**
     * Usuwa detektor, kt�ry zosta� wcze�niej dodany przez {@link #addBoardListener (tetris.Board.BoardListener)}
     *
     * @param s�uchacz tablicy
     */
    public void removeBoardListener(BoardListener boardListener) {
        boardListeners.remove(boardListener);
    }

    /**
     * Pozwala s�ucha� okre�lonych wydarze� na planszy.
     */
    public static interface BoardListener extends EventListener {


        /**
         * Wywo�ywane, gdy tetromino zostanie upuszczone lub kompletny wiersz zostanie upuszczony po wyeliminowaniu niekt�rych rz�d�w.
         */
        void onDropped();

        /**
         * Wywo�ywane, gdy jeden lub wi�cej wierszy jest pe�nych i dlatego zostaje wyeliminowany.
         *
         * @param numer wiersza
         */
        void onRowsEliminated(int rows);

        /**
         * wywo�anie kiedy bezie koniec gry
         */
        void onGameOver();

        /**
         * Wywo�ywane, gdy wyst�pi� b��d.
         */
        void onInvalidMove();

        /**
         * Wywo�ywane, gdy element zosta� przeniesiony.
         *
         * @param horizontalDirection kierunek
         */
        void onMove(HorizontalDirection horizontalDirection);

        /**
         * Wywo�ywane, gdy element zosta� obr�cony.
         *
         * @param horizontalDirection kierunek
         */
        void onRotate(HorizontalDirection horizontalDirection);
    }
}
