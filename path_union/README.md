## Required Dependencies

1. Node JS (package `nodejs` on Ubuntu). If this is your first time using node, you'll probably want to install `npm` as well.

## How To Run

Run the following command from your terminal:
```
nodejs path_union/src/path_union.js ./path_to_input/file.json ./path_to_output/file.json
```
Two command line arguments are expected. The first is a path to an input `JSON` file. The second is the path to where you want the output to be written to.

## Expected Input

The input is a `JSON` file, in the following format 

```
{
    "paths": [
        [{"x": <x0>, "y": <y0>}, {"x": <x1>, "y": <y1>}, ..., {"x": <xn>, "y": <yn>}],
        .
        .
        .
        [{"x": <x0>, "y": <y0>}, {"x": <x1>, "y": <y1>}, ..., {"x": <xk>, "y": <yk>}]
    ]
}
```

Here every `<xi>` and `<yi>` should be replaced with floating point numbers. Each path is a polygonal path with points of the form `{"x": ..., "y": ...}`, with both endpoints inclusive in the path. Note that the first and last point are not connected. 

## Output

The output file will have the following structure:
```
{
    "path": [{"x": <x0>, "y": <y0>}, {"x": <x1>, "y": <y1>}, ..., {"x": <xq>, "y": <yq>}]
}
```
This is a single path that traverses every path given in the input with minimal length connective line segments.