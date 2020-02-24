package emulator.src;

import java.util.List;
import java.awt.*;

/**
 * Class for dealing with higher level drawing commands. Lots of data shuffling, not much substance.
 * 
 * @author Alan Koval
 */
class EtchASketchCanvas {
    // PowerScreen does all the heavy lifting
    PowderScreen simulator;
    // The list of commands extracted from the input file
    private List<EtchCommand> commands;
    // An etch-a-sketch has an aluminum coating which is scratched off to form the image. 
    // This is the default "shaken" coating thickness, i.e. when the etch-a-sketch is empty.
    private final float DEFAULT_COATING_THICKNESS = .01f;

    /**
     * Prepare an EtchASketchCanvas with the given file.
     * @param file An EtchCommandFile extracted from an input .json file
     */
    EtchASketchCanvas(EtchCommandFile file){
        Vector2d pointerLocation = new Vector2d(file.startX, file.startY);
        Vector2d etchExtent = new Vector2d(file.etchWidth, file.etchHeight);
        this.commands = file.commands;

        // args list for your convenience:
        // float coatingThickness, Vector2d screenExtent, double pointsPerUnit, Vector2d initPointerLocation, float pointerRadius
        simulator = new PowderScreen(DEFAULT_COATING_THICKNESS, etchExtent, (float) (5 / file.pointerRadius), pointerLocation, (float) file.pointerRadius);
    }

    /**
     * Execute all commands in the EtchCommandFile given in the constructor
     */
    void execute(){
        int counter = 1;
        int size = commands.size();
        for(EtchCommand command : commands){
            executeCommand(command);
            System.out.println("Finished command " + counter + " / " + size + ".");
            counter++;
        }
    }

    /**
     * Execute a single command.
     * @param command The command to execute.
     */
    private void executeCommand(EtchCommand command){
        if(command.type == EtchCommand.COMMAND_TYPE.LINE){
            simulator.moveTo(command.lineEnd);
        }
    }

    /**
     * Write the result of the emulation to an image file. The number of pixels will be determined by the 
     * pointerRadius and etchExtent given in the input file for maximum precision.
     * @param relativePath A relative file path, ending in some common lossless image format (I've tested .png).
     */
    void writeImageToFile(String relativePath){
        float[][] data = simulator.getScreen();
        Picture pic = new Picture(data.length, data[0].length);
        for(int x = 0; x < data.length; x++){
            for(int y = 0; y < data[x].length; y++){
                float val = data[x][y] / DEFAULT_COATING_THICKNESS;
                val = Math.min(.7f, val);
                val = Math.max(val, 0);
                float computedVal = .8f*val + .3f;
                Color color = new Color(computedVal, computedVal, computedVal);
                // switch the y axis to be upright once again
                pic.set(x, data[x].length - y - 1, color);
            }
        }

        pic.save(relativePath);
    }
}