package emulator.test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import emulator.src.EtchCommand;
import emulator.src.EtchCommandFile;
import emulator.src.Vector2d;

/**
 * Outputs a json file for testing purposes in the shape of a polar plot of your
 * choosing
 * 
 * @author Alan Koval
 */
class PolarPlotter {
    public static void main(String[] args) {
        PolarPlotter plotter = new PolarPlotter();

        // specify plot here!
        PlotterFunction func = (theta) -> (Math.sin(5 * theta) + .5) / 1.5;

        EtchCommandFile file = new EtchCommandFile();
        file.pointerRadius = 0.0025;
        Vector2d startLoc = plotter.getStart(func);
        file.startX = startLoc.x;
        file.startY = startLoc.y;
        file.etchWidth = 2;
        file.etchHeight = 2;

        file.commands = plotter.getCommands(func);

        // write to file
        Gson gson = new Gson();
        try {
            String result = gson.toJson(file);
            PrintWriter out = new PrintWriter("emulator/test/polar_test_input.json");
            out.print(result);
            out.close();
        } catch (Exception e) {
            System.out.println("Failed to write to file!");
            e.printStackTrace();
        }
    }

    private List<EtchCommand> getCommands(PlotterFunction func){
        List<EtchCommand> list = new ArrayList<>();
        for(double theta = 0; theta <= 2 * Math.PI; theta += .01){
            EtchCommand command = new EtchCommand();
            command.lineEnd = toScreenCoords(func.getR(theta), theta);
            command.type = EtchCommand.COMMAND_TYPE.LINE;
            list.add(command);
        }

        return list;
    }

    private Vector2d getStart(PlotterFunction func){
        return toScreenCoords(func.getR(0), 0);
    }

    private Vector2d toScreenCoords(double r, double theta){
        return new Vector2d(1 + r * Math.cos(theta), 1 + r * Math.sin(theta));
    }

    interface PlotterFunction {
        double getR(double theta);
    }
}