
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
 * klasa reprezentuj¹ca g³ówn¹ tablicê
 *

 */
final class Board extends StackPane {

	/**
    * Liczba ukrytych rzêdów, które znajduj¹ siê niewidoczne nad plansz¹.
    * Jest to obszar, w którym pojawiaj¹ siê tetrominos.
    * Domyœlnie s¹ 2.
    */
    private static final byte HIDDEN_ROWS = 2;

    /**
     * Liczba bloków na wiersz, domyœlnie jest to 10.
     */
    private static final byte BLOCKS_PER_ROW = 10;

    /**
     * Liczba bloków na kolumnê. Domyœlnie jest to 20.
     */
    private static final byte BLOCKS_PER_COLUMN = 20;

    /**
     * Liczba maksymalnych podgl¹dów.
     */
    private static final byte MAX_PREVIEWS = 1;

    /**
     *Przejœcie w dó³.
     */
    private final TranslateTransition moveDownTransition;

    /**
     * przejœcie rotacyjne.
     */
    private final RotateTransition rotateTransition;

    /**
     * Sekwencyjne przejœcie, które sk³ada siê z {@link #moveDownTransition} i {@link PauseTransition}.
     */
    private final SequentialTransition moveTransition;

    /**
     * Przejœcie, które pozwala kawa³kowi przejœæ szybko.
     */
    private final TranslateTransition moveDownFastTransition;

    /**
     * Transformacja przejœcia dla ruchu w lewo / w prawo.
     */
    private final TranslateTransition translateTransition;

    /**
     * Zestaw bie¿¹cych przejœæ. Wszystkie uruchomione przejœcia s¹ wstrzymane, gdy gra jest wstrzymana.
     */
    private final Set<Animation> runningAnimations = new HashSet<>();

    /**
     * Dwuwymiarowa tablica, która definiuje planszê. Jeœli element jest pusty w macierzy, jest pusty, w przeciwnym razie jest zajêty.
     */
    private final Rectangle[][] matrix = new Rectangle[BLOCKS_PER_COLUMN + HIDDEN_ROWS][BLOCKS_PER_ROW];

    /**
     * Lista tetrominos, które bêd¹ nastêpne.
     */
    private final ObservableList<Tetromino> waitingTetrominos = FXCollections.observableArrayList();

    /**
     * Bardzo szybkie przejœcie w dó³.
     */
    private final TranslateTransition dropDownTransition;

    /**
     * Przechowuje, jeœli naciœniêty jest klawisz w dó³. Tak d³ugo, jak jest to mo¿liwe, odtwarzane jest {@link #moveDownFastTransition}.
     */
    private boolean moving = false;

    /**
     * Bie¿¹ca pozycja x i y z macierz¹ bie¿¹cego tetromino.
     */
    private int x = 0, y = 0;

    /**
     * To prawda, podczas gdy tetromino jest upuszczane (za pomoc¹ klawisza spacji).
     */
    private boolean isDropping = false;

    /**
     * Obecne tetromino, które spada.
     */
    private Tetromino currentTetromino;

    /**
     * Trzyma s³uchaczy tablicy.
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
        
        /*poni¿szy kod umo¿liwia edycjê wielkoœci tablicy jako dodatek */      
        
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

        // Inicjalizuj przejœcie w dó³. szybkoœc przejœcia
        moveDownTransition = new TranslateTransition(Duration.seconds(0.3));
        moveDownTransition.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                moving = false;
                y++;
            }
        });

        // Po przesuniêciu siê kawa³ka, poczekaj chwilê, a¿ znowu siê poruszy.
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

        // Ten ruch powinien byæ pauzowany
        registerPausableAnimation(moveTransition);

        // Powoduje szybkie przesuniêcie elementu.
        moveDownFastTransition = new TranslateTransition(Duration.seconds(0.08));
        // Aby sprawiæ, by wygl¹da³ bardziej p³ynnie, u¿yj interpolatora liniowego.
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
Rejestruje animacjê, która jest dodawana do listy uruchomionych animacji, jeœli jest uruchomiona, i jest ponownie usuwana, jeœli jest zatrzymana.
      * Gdy gra siê zatrzyma, wszystkie uruchomione animacje zostan¹ wstrzymane.
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

        // Wype³nij kolejkê oczekuj¹cych tetrominos, jeœli jest pusta.
        while (waitingTetrominos.size() <= MAX_PREVIEWS) {
            waitingTetrominos.add(Tetromino.random(squareSize));
        }

        // Usuñ pierwsz¹ z kolejki i odrodz j¹.
        currentTetromino = waitingTetrominos.remove(0);

        // Zresetuj wszystkie przejœcia.
        rotateTransition.setNode(currentTetromino);
        rotateTransition.setToAngle(0);

        translateTransition.setNode(currentTetromino);
        moveDownTransition.setNode(currentTetromino);
        moveDownFastTransition.setNode(currentTetromino);

        // Dodaj bie¿¹ce tetromino na planszê.
        getChildren().add(currentTetromino);

     // Przenieœ go do w³aœciwej pozycji
        // Od³ó¿ tetromino w œrodku (I, O) lub w lewym œrodku (kolejne kszta³ty klocków).
        x = (matrix[0].length - currentTetromino.getMatrix().length) / 2;
        y = 0;
        // Przet³umacz tetromino na pozycjê wyjœciow¹.
        currentTetromino.setTranslateY((y - Board.HIDDEN_ROWS) * getSquareSize());
        currentTetromino.setTranslateX(x * getSquareSize());
        

        //translateTransition.setToX(currentTetromino.getTranslateX());

        
        // Zacznij go przenosiæ.
        moveDown();
    }

    /**
     * Powiadomienie o tetrominie, ¿e nie mo¿e przejœæ dalej.
     */
    private void tetrominoDropped() {
        if (y == 0) {
            // Jeœli utwór nie móg³ siê ruszyæ i nadal znajdujemy siê w pocz¹tkowej pozycji y, gra siê skoñczy³a.
            currentTetromino = null;
            waitingTetrominos.clear();
            notifyGameOver();
        } else {
            mergeTetrominoWithBoard();
        }
    }

    /**
     * Powiadamia s³uchacza, ¿e kawa³ek odpad³.
     */
    private void notifyOnDropped() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onDropped();
        }
    }

    /**
     * Powiadamia s³uchacza, ¿e gra siê skoñczy³a.
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
     * Powiadamia s³uchaczy, ¿e wiersze zosta³y wyeliminowane.
     *
     * @param numer wiersza.
     */
    private void notifyOnRowsEliminated(int rows) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRowsEliminated(rows);
        }
    }

    /**
     * Powiadamia s³uchaczy, ¿e próbowano wykonaæ niewa¿ny ruch.
     */
    private void notifyInvalidMove() {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onInvalidMove();
        }
    }

    /**
     * Powiadamia s³uchaczy, ¿e próbowano wykonaæ niewa¿ny ruch.
     */
    private void notifyRotate(HorizontalDirection horizontalDirection) {
        for (BoardListener boardListener : boardListeners) {
            boardListener.onRotate(horizontalDirection);
        }
    }

    /**
     * Scala tetromino z plansz¹.
     * Dla ka¿dej p³ytki utwórz prostok¹t na planszy.
     * W koñcu usuwa tetromino z planszy i spawnuje now¹.
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
                    // Przypisz prostok¹t do macierzy tablicy.
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

                // Za³ó¿my, ¿e wiersz jest kompletny. Udowodnijmy, ¿e jest odwrotnie.
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

        // Jeœli przynajmniej jeden wiersz zosta³ wyeliminowany.
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
     // Buforowane pamiêci wêz³ów
        
        
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
     * @return Przejœcie, które o¿ywia spadaj¹cy rz¹d.
     */
    private Transition fallRow(final int i, final int by) {
        ParallelTransition parallelTransition = new ParallelTransition();

        if (by > 0) {
            for (int j = 0; j < matrix[i].length; j++) {
                final Rectangle rectangle = matrix[i][j];

                if (rectangle != null) {
                	// Odwróæ pierwotn¹ pozycjê y, aby umo¿liwiæ przejœcie prostok¹ta do nowego.
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
     * @return Przejœcie, które animuje usuwanie wiersza.
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
     * Czyœci planszê i czekaj¹ce tetrominos.
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
     * Oblicza, czy tetromino przecina³oby siê z tablic¹,
     * * przekazuj¹c matrycê, któr¹ bêdzie mieæ tetromino.
     * <p/>
     * * Przecina siê, jeœli uderzy w kolejne tetromino lub przekroczy granicê lew¹, praw¹ lub doln¹.
     *
     * @param targetMatrix Macierz tetromino.
     * @param targetX      docelowa pozycja X 
     * @param targetY      docelowa pozycjat Y 
     * @return * @return Prawda, jeœli przecina siê z plansz¹, w przeciwnym razie jest fa³szywa.
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
     * Uruchamia planszê poprzez utworzenie nowego tetromino.
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
     * Upuszcza tetromino w dó³ do nastêpnej mo¿liwej pozycji.
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
     * Próbuje obróciæ tetromino.
     *
     * @param kierunek poziomy
     * @return To prawda, jeœli rotacja zakoñczy³a siê powodzeniem, w przeciwnym razie jest nieprawda.
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
     * @return To prawda, jeœli ruch siê powiód³. Fa³sz, jeœli ruch zosta³ zablokowany przez planszê.
     */
    public boolean move(final HorizontalDirection direction) {
        boolean result;
        if (currentTetromino == null || isDropping) {
            result = false;
        } else {
            int i = direction == HorizontalDirection.RIGHT ? 1 : -1;
            x += i;
            //Jeœli siê nie porusza, sprawdŸ tylko aktualn¹ pozycjê y.
            // Jeœli siê porusza, sprawdŸ tak¿e docelow¹ pozycjê y.
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
     * Przenosi tetromino o jedno pole w dó³.
     */
    public void moveDown() {
        if (!isDropping && currentTetromino != null) {
            moveDownFastTransition.stop();
            moving = true;

            // Jeœli jest w stanie przejœæ do nastêpnej pozycji y, zrób to!
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
     * Szybko przesuwa bie¿¹ce tetromino, jeœli nie spada.
     */
    public void moveDownFast() {
        if (!isDropping) {

            // Zatrzymaj normalne przejœcie przejœcia.
            moveTransition.stop();
            // Nastêpnie sprawdŸ, czy nastêpna pozycja nie przecina³aby siê z desk¹.
            if (!intersectsWithBoard(currentTetromino.getMatrix(), x, y + 1)) {
                // Jeœli mo¿e siê ruszaæ, ruszaj!
                moveDownFastTransition.toYProperty().unbind();
                moveDownFastTransition.toYProperty().bind(squareSize.multiply(y + 1 - Board.HIDDEN_ROWS));
                moveDownFastTransition.playFromStart();
            } else {
                // W innym przypadku osi¹gn¹³ on ziemiê.
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
     *Odtwarza planszê ponownie, po jej zatrzymaniu.
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
     * Dostaje oczekuj¹ce tetrominosy, które maj¹ zostaæ odrodzone.
     * <p/>
     * Pierwszy element zostanie odrodzony w nastêpnej kolejnoœci.
     *
     * @return Lista oczekuj¹cych tetrominos.
     */
    public ObservableList<Tetromino> getWaitingTetrominos() {
        return waitingTetrominos;
    }

    public double getSquareSize() {
        return squareSize.get();
    }

    /**
     * Dodaje s³uchacza do tablicy, która otrzymuje powiadomienia o okreœlonych wydarzeniach.
     *
     * @param s³uchacz tablicy
     */
    public void addBoardListener(BoardListener boardListener) {
        boardListeners.add(boardListener);
    }

    /**
     * Usuwa detektor, który zosta³ wczeœniej dodany przez {@link #addBoardListener (tetris.Board.BoardListener)}
     *
     * @param s³uchacz tablicy
     */
    public void removeBoardListener(BoardListener boardListener) {
        boardListeners.remove(boardListener);
    }

    /**
     * Pozwala s³uchaæ okreœlonych wydarzeñ na planszy.
     */
    public static interface BoardListener extends EventListener {


        /**
         * Wywo³ywane, gdy tetromino zostanie upuszczone lub kompletny wiersz zostanie upuszczony po wyeliminowaniu niektórych rzêdów.
         */
        void onDropped();

        /**
         * Wywo³ywane, gdy jeden lub wiêcej wierszy jest pe³nych i dlatego zostaje wyeliminowany.
         *
         * @param numer wiersza
         */
        void onRowsEliminated(int rows);

        /**
         * wywo³anie kiedy bezie koniec gry
         */
        void onGameOver();

        /**
         * Wywo³ywane, gdy wyst¹pi³ b³¹d.
         */
        void onInvalidMove();

        /**
         * Wywo³ywane, gdy element zosta³ przeniesiony.
         *
         * @param horizontalDirection kierunek
         */
        void onMove(HorizontalDirection horizontalDirection);

        /**
         * Wywo³ywane, gdy element zosta³ obrócony.
         *
         * @param horizontalDirection kierunek
         */
        void onRotate(HorizontalDirection horizontalDirection);
    }
}
