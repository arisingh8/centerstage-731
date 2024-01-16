package org.firstinspires.ftc.teamcode.tests;

import com.arcrobotics.ftclib.command.CommandScheduler;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.subsystems.Elevator;
import org.firstinspires.ftc.teamcode.utils.TelemetryHandler;

@TeleOp(group = "test")
public class LiftTestOpMode extends LinearOpMode {
    private final CommandScheduler scheduler = CommandScheduler.getInstance();
    private final TelemetryHandler telemetryHandler = new TelemetryHandler(telemetry);

    @Override
    public void runOpMode() throws InterruptedException {
        Elevator elevatorSubsystem = new Elevator(hardwareMap, telemetryHandler);
        GamepadEx gamepad = new GamepadEx(gamepad1);

        // fix with default commands
        //gamepad.getGamepadButton(GamepadKeys.Button.Y)
        //        .whenActive(new InstantCommand(() -> elevatorSubsystem.setLiftState(Elevator.LiftState.OUTTAKE), elevatorSubsystem))
        //        .whenInactive(new InstantCommand(() -> elevatorSubsystem.setLiftState(Elevator.LiftState.INTAKE), elevatorSubsystem));
        //liftSubsystem.setDefaultCommand(new PerpetualCommand(new InstantCommand(() -> liftSubsystem.setLiftState(Lift.LiftState.INTAKE), liftSubsystem)));


        gamepad.getGamepadButton(GamepadKeys.Button.Y)
                .whenActive(new InstantCommand(() -> elevatorSubsystem.setElevatorHeight(Elevator.ElevatorState.MAXIMUM), elevatorSubsystem));
        gamepad.getGamepadButton(GamepadKeys.Button.B)
                .whenActive(new InstantCommand(() -> elevatorSubsystem.setElevatorHeight(Elevator.ElevatorState.IDLE), elevatorSubsystem));
        gamepad.getGamepadButton(GamepadKeys.Button.A)
                .whenActive(new InstantCommand(() -> elevatorSubsystem.setElevatorHeight(Elevator.ElevatorState.MINIMUM), elevatorSubsystem));
        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            scheduler.run();
            telemetryHandler.update();
        }

        scheduler.reset();
    }
}
