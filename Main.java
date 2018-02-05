


package tetris;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public final class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        System.out.println(System.getProperty("javafx.runtime.version"));

        primaryStage.setTitle("Gra_tetris");

        Scene scene = new Scene(new Tetris());

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
