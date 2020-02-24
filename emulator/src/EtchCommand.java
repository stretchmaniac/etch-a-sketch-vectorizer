package emulator.src;

/**
 * A single motion by the emulator. Right now only supports lines, but could be very easily 
 * extended to arbitrary paths (arcs, etc) if someone should find it necessary.
 * 
 * @author Alan Koval
 */
public class EtchCommand {
    public enum COMMAND_TYPE {
        LINE
    };
    public COMMAND_TYPE type;

    // parameters for COMMAND_TYPE.LINE:
    public Vector2d lineEnd;

    // parameters for other COMMAND_TYPEs, if they should exist, go here
}