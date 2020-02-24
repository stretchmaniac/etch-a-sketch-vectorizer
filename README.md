# etch-a-sketch-vectorizer

The goal of this project is to convert an image into a vectorized path so that 
a robot can draw the image on a real-life etch-a-sketch.

## How to Contribute

Unless someone really wants to, I am not going to manage pull requests. If you 
want to contribute, contact me (Alan Koval) with your github account info and 
I will add you as a contributor. Please adhere to the following guidelines 
to maintain some semblance of organization in this repository:

1. After cloning the repo, make a branch that you will put all your work on. Include your name in the branch name, and push the branch back to the repo. For example:

        git checkout -b alan-koval       # make new branch including my name
        git push -u origin alan-koval    # push the branch
    Do your work on your branch and merge to and from `master` when you deem fit.
2. Make a separate folder for any sub-projects. Use descriptive names.
3. Inside your project folders, **include a `.txt` or `.md` file detailing how to run your program**. This includes what compilers/packages you would need to install to get it to run it on a fresh Ubuntu installation. Ideally include exactly what kind of input your program expects and what it will output.
4. If you finish a feature, experimental or not, edit this readme with a short description of what it does. 

So that everyone can use their favorite programming language, restrict your 
input when possible to language-agnostic file types. I would recommend 
using `.json` files for output data; most languages have decent 
serialization libraries (i.e. Java's `gson`, Python's `json` package, `JSON.parse(...)` in JavaScript, etc). 

## Problem Statement

**Input** 
1. An image. Can be `.png`, `.bmp` or other lossless format.
2. A value of `alpha_L`, the signed proportion of the width of the etch-a-sketch traversed when the left stepper motor is rotated by `1 rad` in the counterclockwise direction. By signed we mean that that `alpha_L < 0` if and only if the pointer moves to the left when the stepper motor is rotated in the counterclockwise direction. 
3. A value of `alpha_R`, the signed proportion of the height of the etch-a-sketch traversed when the left stepper motor is rotated by `1 rad` in the counterclockwise direction.

**Required Output**

A `.json` file enumerating a sequence of stepper motor commands. Each command must give a left knob angular velocity, a right knob 
angular velocity, and a duration of the command in seconds. When fed to a machine that executes the commands in order on a real etch-a-sketch, the machine must produce on the etch-a-sketch a visually appealing rendition of the input image.

## Emulator

See `emulator` folder. Takes a simple `.json` input file with line segments and 
converts it into an image fle of what it may look like on a real etch-a-sketch. Here's an example of what it outputs: 

![image of etch-a-sketch output](https://i.imgur.com/ajmryX7.png)