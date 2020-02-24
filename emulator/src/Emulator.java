package emulator.src;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import com.google.gson.Gson;

/**
 * Emulates an etch-a-sketch given a set of commands. Outputs to image file.
 * 
 * @author Alan Koval
 */
class Emulator {
    /**
     * Generates an image from the given .json data file.
     * @param args Two arguments required:
     *      1. A relative file path specifying the .json data file.
     *      2. An image output path.
     */
    public static void main(String[] args){
        String jsonFilePath = args[0];
        String outputFilePath = args[1];

        Gson gson = new Gson();

        try (Reader reader = new FileReader(jsonFilePath)) {
                // Convert JSON File to Java Object
                EtchCommandFile file = gson.fromJson(reader, EtchCommandFile.class);
                // make a canvas and draw to it with lines from the given file 
                EtchASketchCanvas sketcher = new EtchASketchCanvas(file);
                sketcher.execute();
                System.out.println("Writing result to image file.");
                sketcher.writeImageToFile(outputFilePath);
                System.out.println("Done!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}