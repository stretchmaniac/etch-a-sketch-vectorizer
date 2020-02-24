## Expected Dependencies 
1. Java 8+ jdk, package `openjdk-11-jdk` should be fine for Ubuntu.

## How To Run
1. You'll notice the `gson` dependency in the `lib` folder. This has to be manually put in the classpath while compiling. In addition, so that all the classes are compiled during development, I've kept src/sources.txt updated 
with the names of all the `.java` files so that you can compile them all at once. To compile, navigate to the root folder (`.../etch-a-sketch-vectorizer`) and run
            
        javac -cp ".:emulator/lib/gson-2.8.6.jar" @./emulator/src/sources.txt
2. Run with the following command. The first command line arg (after `emulator/src/Emulator`) is the relative path of the input `.json` file. The second command line arg is the output image file path.

        java -cp ".:emulator/lib/gson-2.8.6.jar" emulator/src/Emulator "emulator/test/polar_test_input.json" "emulator/test/test_output_polar.png"
## Expected Input
A path to a single `.json` file is expected as a command line argument. This 
file should have the following structure:
```json
{
    "startX" : <starting cursor position x>,
    "startY" : <starting cursor position y>,
    "etchWidth" : <width of etch-a-sketch>,
    "etchHeight" : <height of etch-a-sketch>,
    "pointerRadius": <radius of pointer>,
    "commands" : [
        {"type": "LINE", "lineEnd": {"x": <x coord>, "y": <y coord>}},
        {"type": "LINE", "lineEnd": {"x": <x coord>, "y": <y coord>}},
        {"type": "LINE", "lineEnd": {"x": <x coord>, "y": <y coord>}},
        ...
    ]
}
```

(Everything in `<...>` should be replaced by a number). Note that the above example without the ellipses produces three line segments.

## Output

The program will output an image at a location specified by command line argument. You might be surprised at the size of the image. That is so you can peer very closely at the result and realize how much work went into making this emulator :).