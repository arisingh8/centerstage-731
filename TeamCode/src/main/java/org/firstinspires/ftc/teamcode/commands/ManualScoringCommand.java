package org.firstinspires.ftc.teamcode.commands;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.command.CommandBase;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.ScoringMech;
import org.firstinspires.ftc.teamcode.utils.RangeController;
import org.firstinspires.ftc.teamcode.utils.Rumbler;
import org.firstinspires.ftc.teamcode.utils.TelemetryHandler;

import java.util.function.BooleanSupplier;

@Config
public class ManualScoringCommand extends CommandBase {
    public static double RELEASE_TIME = 0.6;
    public static double LIFT_UP_FRACTION = 0.75;
    public static double LIFT_DOWN_FRACTION = 0.04;
    public static double SCORING_INCREMENT = 0.03;
    public static double MAX_SCORING_FRACTION = 1.0;
    public static double MIN_SCORING_FRACTION = 0.5;
    public static double SERVO_RESET_TIME = 0.5;

    private final ScoringMech scoringMechSubsystem;
    private final BooleanSupplier intakeButton, scoreButton, hangButton, downButton, cancelButton;
    private final Rumbler rumbler;
    private final ElapsedTime eTime = new ElapsedTime(ElapsedTime.Resolution.SECONDS);

    private double lastScoringPosition = 0.8;
    private boolean oldUpButton = false;
    private boolean oldDownButton = false;

    private enum ScoringState {
        IDLE,
        INTAKING,
        ELEVATE_TO_SCORE,
        SCORING,
        RELEASING,
        ELEVATE_TO_HANG,
        WAITING_FOR_HANG,
        HANGING,
        SERVO_RESET,
        RESETTING
    }

    private ScoringState scoringState = ScoringState.INTAKING;

    private final TelemetryHandler telemetryHandler;

    public ManualScoringCommand(ScoringMech scoringMechSubsystem, BooleanSupplier intakeButton,
                                BooleanSupplier scoreButton, BooleanSupplier hangButton,
                                BooleanSupplier downButton, BooleanSupplier cancelButton,
                                Rumbler rumbler, TelemetryHandler telemetryHandler) {
        this.scoringMechSubsystem = scoringMechSubsystem;
        this.intakeButton = intakeButton;
        this.scoreButton = scoreButton;
        this.hangButton = hangButton;
        this.downButton = downButton;
        this.cancelButton = cancelButton;
        this.rumbler = rumbler;
        this.telemetryHandler = telemetryHandler;

        addRequirements(scoringMechSubsystem);
    }

    @Override
    public void execute() {
        switch (scoringState) {
            case IDLE:
                if (intakeButton.getAsBoolean()) {
                    if (scoringMechSubsystem.getNumPixelsInBucket() == 2) {
                        rumbler.rumble(500);
                    } else {
                        scoringMechSubsystem.startIntake();
                        scoringMechSubsystem.setWheelState(ScoringMech.WheelState.INTAKE);
                        scoringState = ScoringState.INTAKING;
                    }
                }
                if (scoreButton.getAsBoolean() && scoringMechSubsystem.getNumPixelsInBucket() > 0) {
                    scoringMechSubsystem.setElevatorHeight(lastScoringPosition);
                    scoringState = ScoringState.ELEVATE_TO_SCORE;
                }
                if (hangButton.getAsBoolean()) {
                    scoringMechSubsystem.setElevatorHeight(LIFT_UP_FRACTION);
                    scoringState = ScoringState.ELEVATE_TO_HANG;
                }
                break;
            case INTAKING:
                if (!intakeButton.getAsBoolean() || scoringMechSubsystem.getNumPixelsInBucket() == 2) {
                    scoringMechSubsystem.stopIntake();
                    scoringMechSubsystem.setWheelState(ScoringMech.WheelState.STOPPED);
                    scoringState = ScoringState.IDLE;
                }
                break;
            case ELEVATE_TO_SCORE:
                if (!scoreButton.getAsBoolean()) {
                    scoringState = ScoringState.SCORING;
                }
                break;
            case SCORING:
                if (scoringMechSubsystem.canLiftServosExtend()) {
                    scoringMechSubsystem.setLiftServoState(ScoringMech.LiftServoState.OUTTAKE);
                }

                if (scoreButton.getAsBoolean() && !scoringMechSubsystem.isElevatorBusy()) {
                    oldUpButton = false;
                    oldDownButton = false;

                    scoringMechSubsystem.setWheelState(ScoringMech.WheelState.OUTTAKE);
                    scoringState = ScoringState.RELEASING;
                    eTime.reset();
                }

                if (intakeButton.getAsBoolean() && !oldUpButton) {
                    lastScoringPosition += SCORING_INCREMENT;
                }
                if (downButton.getAsBoolean() && !oldDownButton) {
                    lastScoringPosition -= SCORING_INCREMENT;
                }
                lastScoringPosition = RangeController.clamp(lastScoringPosition, MIN_SCORING_FRACTION, MAX_SCORING_FRACTION);
                scoringMechSubsystem.setElevatorHeight(lastScoringPosition);
                oldUpButton = intakeButton.getAsBoolean();
                oldDownButton = downButton.getAsBoolean();

                if (cancelButton.getAsBoolean()) {
                    scoringMechSubsystem.setElevatorHeight(0);
                    scoringMechSubsystem.setLiftServoState(ScoringMech.LiftServoState.INTAKE);
                    scoringState = ScoringState.RESETTING;
                }

                break;
            case RELEASING:
                if (eTime.time() > RELEASE_TIME) {
                    scoringMechSubsystem.setWheelState(ScoringMech.WheelState.STOPPED);
                    if (scoringMechSubsystem.getNumPixelsInBucket() > 0) {
                        scoringState = ScoringState.SCORING;
                    } else {
                        scoringMechSubsystem.setWheelState(ScoringMech.WheelState.STOPPED);
                        scoringMechSubsystem.setElevatorHeight(0);
                        scoringMechSubsystem.setLiftServoState(ScoringMech.LiftServoState.INTAKE);
                        scoringState = ScoringState.RESETTING;
                    }
                }
                break;
            case ELEVATE_TO_HANG:
                if (!hangButton.getAsBoolean()) {
                    scoringState = ScoringState.WAITING_FOR_HANG;
                }
                break;
            case WAITING_FOR_HANG:
                if (scoringMechSubsystem.canLiftServosExtend()) {
                    scoringMechSubsystem.setLiftServoState(ScoringMech.LiftServoState.LIFT);
                }
                if (hangButton.getAsBoolean() && !scoringMechSubsystem.isElevatorBusy()) {
                    scoringMechSubsystem.setElevatorHeight(LIFT_DOWN_FRACTION);
                    scoringState = ScoringState.HANGING;
                }
                break;
            case HANGING:
                if (cancelButton.getAsBoolean()) {
                    scoringMechSubsystem.setLiftServoState(ScoringMech.LiftServoState.INTAKE);
                    scoringState = ScoringState.SERVO_RESET;
                    eTime.reset();
                }
                break;
            case SERVO_RESET:
                if (eTime.time() > SERVO_RESET_TIME) {
                    scoringMechSubsystem.setElevatorHeight(0);
                    scoringState = ScoringState.RESETTING;
                }
                break;
            case RESETTING:
                if (!scoringMechSubsystem.isElevatorBusy()) {
                    scoringState = ScoringState.IDLE;
                }
                break;
        }

        telemetryHandler.addData("scoring state", scoringState);
    }
}
