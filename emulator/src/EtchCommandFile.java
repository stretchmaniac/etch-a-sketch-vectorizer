package emulator.src;

import java.util.List;

/**
 * The structure of the .json file expected as imput to Emulator.java
 * 
 * @author Alan Koval
 */
public class EtchCommandFile {
    public double startX; // the expected start x-coordinate of the pointer 
    public double startY; // the expected start y-coordinate of the pointer
    public double etchWidth; // the width of the etch-a-sketch, in cm
    public double etchHeight; // the height of the etch-a-sketch, in cm
    public double pointerRadius; // the thickness of the etch-a-sketch lines, in cm
    public List<EtchCommand> commands; // a list of commands, i.e. line segments
}