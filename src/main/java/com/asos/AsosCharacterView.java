package com.asos;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * The animated Asos mascot shown in the corner widget.
 *
 * A small vector face (no image assets) with a handful of moods driven by
 * app events: it blinks and bobs while idle, bounces happily when a step is
 * completed, shakes with a frown on mistakes, and looks up thoughtfully when
 * a hint appears. All animations run on the JavaFX animation timer - no
 * background threads.
 */
public class AsosCharacterView extends Group {

    private static final double SIZE = 44;

    private final Circle body;
    private final Ellipse leftEye;
    private final Ellipse rightEye;
    private final QuadCurve mouth;

    private final TranslateTransition idleBob;
    private PauseTransition nextBlink;
    private PauseTransition moodReset;

    public AsosCharacterView() {
        double r = SIZE / 2;

        body = new Circle(r, r, r);
        body.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366f1")),
                new Stop(1, Color.web("#8b5cf6"))));
        body.setEffect(new DropShadow(10, Color.web("#6366f1", 0.45)));

        leftEye = createEye(r - 8, r - 4);
        rightEye = createEye(r + 8, r - 4);

        mouth = new QuadCurve();
        mouth.setStroke(Color.WHITE);
        mouth.setStrokeWidth(2.2);
        mouth.setStrokeLineCap(StrokeLineCap.ROUND);
        mouth.setFill(Color.TRANSPARENT);
        setSmile(0.45); // gentle default smile

        getChildren().addAll(body, leftEye, rightEye, mouth);

        // Idle: gentle bobbing
        idleBob = new TranslateTransition(Duration.seconds(1.8), this);
        idleBob.setByY(2.5);
        idleBob.setCycleCount(TranslateTransition.INDEFINITE);
        idleBob.setAutoReverse(true);
        idleBob.setInterpolator(Interpolator.EASE_BOTH);
        idleBob.play();

        // Idle: blinking at slightly irregular intervals (feels alive)
        scheduleNextBlink();
    }

    private Ellipse createEye(double x, double y) {
        Ellipse eye = new Ellipse(x, y, 3.4, 4.6);
        eye.setFill(Color.WHITE);
        return eye;
    }

    /**
     * Shape the mouth: positive curve = smile, negative = frown.
     * Magnitude 0..1 controls how strong the expression is.
     */
    private void setSmile(double amount) {
        double r = SIZE / 2;
        double halfWidth = 7 + 3 * Math.abs(amount);
        mouth.setStartX(r - halfWidth);
        mouth.setStartY(r + 8);
        mouth.setEndX(r + halfWidth);
        mouth.setEndY(r + 8);
        mouth.setControlX(r);
        mouth.setControlY(r + 8 + 9 * amount);
    }

    private void setEyesOffset(double dx, double dy) {
        double r = SIZE / 2;
        leftEye.setCenterX(r - 8 + dx);
        leftEye.setCenterY(r - 4 + dy);
        rightEye.setCenterX(r + 8 + dx);
        rightEye.setCenterY(r - 4 + dy);
    }

    // ------------------------------------------------------------------
    // Idle blinking
    // ------------------------------------------------------------------

    private void scheduleNextBlink() {
        nextBlink = new PauseTransition(Duration.seconds(2.5 + Math.random() * 3.5));
        nextBlink.setOnFinished(e -> {
            blink();
            scheduleNextBlink();
        });
        nextBlink.play();
    }

    private void blink() {
        Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(leftEye.scaleYProperty(), 1),
                        new KeyValue(rightEye.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(70),
                        new KeyValue(leftEye.scaleYProperty(), 0.08),
                        new KeyValue(rightEye.scaleYProperty(), 0.08)),
                new KeyFrame(Duration.millis(140),
                        new KeyValue(leftEye.scaleYProperty(), 1),
                        new KeyValue(rightEye.scaleYProperty(), 1)));
        blink.play();
    }

    // ------------------------------------------------------------------
    // Moods (each reverts to idle automatically)
    // ------------------------------------------------------------------

    /** Step completed / course started: big smile + joyful bounce. */
    public void playHappy() {
        startMood(3.0);
        setSmile(1.0);
        setEyesOffset(0, -1);

        TranslateTransition jump = new TranslateTransition(Duration.millis(160), this);
        jump.setByY(-8);
        jump.setCycleCount(4);
        jump.setAutoReverse(true);
        jump.setInterpolator(Interpolator.EASE_OUT);
        jump.setOnFinished(e -> setTranslateY(0));
        jump.play();

        ScaleTransition pop = new ScaleTransition(Duration.millis(160), this);
        pop.setToX(1.12);
        pop.setToY(1.12);
        pop.setCycleCount(2);
        pop.setAutoReverse(true);
        pop.play();
    }

    /** Mistake detected: frown + worried shake. */
    public void playConcerned() {
        startMood(3.0);
        setSmile(-0.7);
        setEyesOffset(0, 1);

        TranslateTransition shake = new TranslateTransition(Duration.millis(60), this);
        shake.setByX(4);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> setTranslateX(0));
        shake.play();
    }

    /** Hint offered: thoughtful look up and to the side. */
    public void playThinking() {
        startMood(3.5);
        setSmile(0.15);
        setEyesOffset(2.5, -2.5);
    }

    /**
     * Pause idle motion, apply the mood, and schedule the return to idle.
     */
    private void startMood(double seconds) {
        idleBob.pause();
        if (moodReset != null) {
            moodReset.stop();
        }
        moodReset = new PauseTransition(Duration.seconds(seconds));
        moodReset.setOnFinished(e -> returnToIdle());
        moodReset.play();
    }

    private void returnToIdle() {
        setSmile(0.45);
        setEyesOffset(0, 0);
        setTranslateX(0);
        setScaleX(1);
        setScaleY(1);
        idleBob.play();
    }
}
