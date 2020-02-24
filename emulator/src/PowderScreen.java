package emulator.src;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for computation of aluminum powder levels across the surface of the entire 
 * etch-a-sketch as commands are executed. 
 * 
 * @author Alan Koval
 */
class PowderScreen {
    // An 2d array representing the height of aluminum powder at selected (evenly distributed) points on the glass
    private float[][] screen;
    // Extraneous data storage for use in moveStep function. Re-initializing every step causes a serious performance hit.
    PositiondValue[][] screenData;
    boolean[][] screenData2;
    // the location of the thing that scraped the aluminum off the glass (I call it the pointer).
    // Note things that are doubles are in "real coordinates", i.e. what you would specify when drawing a path on the etch-a-sketch.
    // Things that are integers are integers in screen (above), i.e. approximation coordinates.
    private Vector2d pointerLocation;
    // the radius of the pointer. Note that the pointer has a circular cross section. I assume it do be a right cylinder
    private float pointerRadius;
    // the initial coating of aluminum at every point on the screen
    private float initialCoatingThickness;
    // the width/height of the etch-a-sketch
    private Vector2d screenExtent;
    // for floating point computation
    private final double EPSILON = 0.0001;
    // parameters for aluminum deformation "physics" (in quotes because this definitely doesn't qualify as actual physics)
    // How much the movement of one pixel affects the movement of the neighboring pixels
    private final double DRAG_ATTENUATION = 0.5;
    // How much the direction of one pixel affects the direction of movement of neighboring pixels
    private final double DRAG_DIRECTION_CONST = 0.5;
    // How much the direction of motion of the pointer affects the movement of pushed aluminum powder (as opposed to the normal direction)
    private final double POINTER_FRICTION = 0.3;
    // When to stop propagating shifts in aluminum (i.e. magnitude of aluminum transferred is too small)
    private final double MIN_DRAG_TRANSFER = 0.01;

    /**
     * @param coatingThickness The desired thickness of the aluminum coating of the inside surface of the etch-a-sketch.
     * @param screenExtent The width/height of the etch-a-sketch, in cm
     * @param pointsPerUnit The approximation density, in approximation points / cm. Note that there are pointsPerUnit^2 approximation 
     *                      points per cm^2.
     * @param initPointerLocation The initial location of the pointer (where the drawing begins), in cm
     * @param pointerRadius The thickness of the drawing stylus, in cm
     */
    PowderScreen(float coatingThickness, Vector2d screenExtent, double pointsPerUnit, Vector2d initPointerLocation, float pointerRadius){
        initialCoatingThickness = coatingThickness;
        this.screenExtent = screenExtent;
        this.pointerRadius = pointerRadius;

        int approxWidth = (int) Math.ceil(screenExtent.x * pointsPerUnit);
        int approxHeight = (int) Math.ceil(screenExtent.y * pointsPerUnit);

        // init blank screen 
        screen = new float[approxWidth][approxHeight];
        for(int y = 0; y < approxHeight; y++){
            for(int x = 0; x < approxWidth; x++){
                screen[x][y] = coatingThickness;
            }
        }

        screenData = new PositiondValue[screen.length][screen[0].length];
        screenData2 = new boolean[screen.length][screen[0].length];

        pointerLocation = new Vector2d(initPointerLocation);

        // start with no aluminum at pointer location 
        changeAluminumDistributionInDisk(pointerLocation, pointerRadius, (x, y) -> 0);
    }

    Vector2d getPointerLocation(){
        return pointerLocation;
    }

    float[][] getScreen(){
        return screen;
    }

    /**
     * Moves the pointer to a new position in a straight line, calculating the effect the move has on the underlying 
     * aluminum distribution.
     * @param newPosition Where to move the pointer.
     */
    void moveTo(Vector2d newPosition){
        double stepSize = getApproxPixelWidth();
        // we separate the move into microsteps of length stepSize
        while(pointerLocation.distance(newPosition) > EPSILON){
            Vector2d direction = newPosition.subtract(pointerLocation);
            if(direction.length() <= stepSize){
                moveStep(direction);
            } else {
                // the last step is some fraction of stepSize
                moveStep(direction.multLocal(stepSize / direction.length()));
            }
        }
    }

    /**
     * Moves the pointer one microstep in a certain direction while calculating the effect 
     * on the underlying aluminum distribution. This is where all the interesting stuff happens.
     * @param offset A vector of length getApproxPixelWidth() in any direction.
     */
    private void moveStep(Vector2d offset){
        // expected screenData and screenData2 to be empty 
        Vector2i minScreenDataChanged = new Vector2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector2i maxScreenDataChanged = new Vector2i(Integer.MIN_VALUE, Integer.MIN_VALUE);

        // actually move the pointer
        Vector2d oldPointerLocation = new Vector2d(pointerLocation);
        pointerLocation.addLocal(offset);
        // get the pixels (i.e. indices in screen) that now lie under the pointer. These are 
        // displaced by the pointer and will be forced to move to some new location

        // take a very close look at getNonZeroApproxPixelsInDisk so you know what this list means
        List<PositionValue> displacedAmounts = getNonZeroApproxPixelsInDisk(pointerLocation, pointerRadius, oldPointerLocation);
        // the movement of one pixel is going to affect the motion of other pixels. We stratify these motions 
        // into layers, i.e. layer 1 will move, affecting layer 2, which will affect layer 3, etc. No pixels in 
        // a layer affects pixels in the same layer. displacementLayer is the latest layer.
        List<Vector2i> displacementLayer = new ArrayList<>();
        for(PositionValue pv : displacedAmounts){
            Vector2i pixelPos = pv.position;
            // the "value" (amount of aluminum) we wish to transfer away from this pixel
            float value = screen[pixelPos.x][pixelPos.y] * pv.value;
            // outwardVec is the direction specified by the angle this pixel makes with the center of the pointer
            Vector2d outwardVec = toScreenCoords(pixelPos).subtractLocal(pointerLocation).normalizeLocal();
            // we take a weighted average of outwardVec and the direction of motion of the pointer (offset)
            Vector2d displacement = offset.normalize().multLocal(POINTER_FRICTION).addLocal(outwardVec.multLocal(1 - POINTER_FRICTION))
                                    .normalizeLocal().multLocal(offset.length() * 1.5);
            // screenData[x][y] tells the current movement (i.e. displacement and amount of aluminum involved) for pixel (x,y)
            screenData[pixelPos.x][pixelPos.y] = new PositiondValue(displacement, value);
            // screenData2[x][y] is true if pixel (x,y) has been used in a layer already
            screenData2[pixelPos.x][pixelPos.y] = true;
            // rather than re-initialize screenData and screenData2 every iteration (that takes a very long time),
            // we keep track of where we've changed values in screenData/screenData2 so that we can clear the arrays efficiently.
            updateMinMaxPointCounter(minScreenDataChanged, maxScreenDataChanged, pixelPos);
            displacementLayer.add(pixelPos);
        }

        while(displacementLayer.size() > 0){
            List<Vector2i> newDisplacementLayer = new ArrayList<>();
            for(Vector2i displacementPos : displacementLayer){
                PositiondValue amountDisplaced = screenData[displacementPos.x][displacementPos.y];
                // we will calculate the effect the movement occurring at displacementPos has on its neighbors
                List<PositionValue> neighbors = getNonZeroApproxPixelsInDisk(toScreenCoords(displacementPos), getApproxPixelWidth()*1.5, null);
                for(PositionValue pv : neighbors){
                    // check if this neighbor has already been a part of a layer
                    if(!screenData2[pv.position.x][pv.position.y]){
                        // "neighbor offset", the movement based on the angle this neighbor makes with the pixel in question
                        Vector2d nOffset = toScreenCoords(pv.position.subtract(displacementPos));
                        // as before, we take a weighted average of the direction that the pixel is taking and 
                        // the nOffset direction
                        Vector2d dragDir = amountDisplaced.position.normalize()
                                        .multLocal(DRAG_DIRECTION_CONST).addLocal(
                                            nOffset.normalize().multLocal(1 - DRAG_DIRECTION_CONST)
                                        ).normalizeLocal();
                        // we want to attenuate the effect of drag
                        dragDir.multLocal(DRAG_ATTENUATION * amountDisplaced.position.length());
                        float valueTransferred = (float) (DRAG_ATTENUATION * amountDisplaced.value * pv.value);
                        if(valueTransferred < initialCoatingThickness * MIN_DRAG_TRANSFER || dragDir.length() < getApproxPixelWidth() * .2){
                            continue;
                        }
                        valueTransferred = Math.min(screen[pv.position.x][pv.position.y], valueTransferred);
                        updateMinMaxPointCounter(minScreenDataChanged, maxScreenDataChanged, pv.position);
                        if(screenData[pv.position.x][pv.position.y] == null){
                            // this pixel has not been encountered before
                            screenData[pv.position.x][pv.position.y] = new PositiondValue(dragDir, valueTransferred);
                            newDisplacementLayer.add(pv.position);
                        } else {
                            // we've already encountered this pixel as a neighbor before
                            valueTransferred = Math.min(screen[pv.position.x][pv.position.y], valueTransferred + screenData[pv.position.x][pv.position.y].value);
                            screenData[pv.position.x][pv.position.y].value = valueTransferred;
                            // close enough to an average
                            screenData[pv.position.x][pv.position.y].position.addLocal(dragDir).multLocal(.5f);
                        }
                    }
                }

                // actually compute the displacement
                Vector2d destination = toScreenCoords(displacementPos).addLocal(amountDisplaced.position);
                float amountTransferred = amountDisplaced.value;
                List<PositionValue> dstPxs = getNonZeroApproxPixelsInDisk(destination, getApproxPixelWidth(), null);
                float totalValue = 0;
                for(PositionValue dstPV : dstPxs){
                    totalValue += dstPV.value;
                }
                screen[displacementPos.x][displacementPos.y] -= amountTransferred;
                for(PositionValue dstPV :dstPxs){
                    screen[dstPV.position.x][dstPV.position.y] += amountTransferred * dstPV.value / totalValue;
                }
            }

            for(Vector2i newLayerPt : newDisplacementLayer){
                screenData2[newLayerPt.x][newLayerPt.y] = true;
            }
            displacementLayer = newDisplacementLayer;
        }

        // clear screenData / screenData2 
        for(int x = minScreenDataChanged.x; x <= maxScreenDataChanged.x; x++){
            for(int y = minScreenDataChanged.y; y <= maxScreenDataChanged.y; y++){
                screenData[x][y] = null;
                screenData2[x][y] = false;
            }
        }
    }

    private void updateMinMaxPointCounter(Vector2i minCounter, Vector2i maxCounter, Vector2i sample){
        minCounter.x = Math.min(minCounter.x, sample.x);
        minCounter.y = Math.min(minCounter.y, sample.y);
        maxCounter.x = Math.max(maxCounter.x, sample.x);
        maxCounter.y = Math.max(maxCounter.y, sample.y);
    }

    /**
     * Retrieves the indices (i,j) of pixels so that screen[i][j] > 0 and has at least some part in the disk.
     * The "value" part of it gives the proportion of the pixel inside the disk.
     * @param diskCenter The center of the disk, in real coordinates.
     * @param diskRadius The radius of the disk, in real coordinates.
     * @return A list of indices.
     */
    private List<PositionValue> getNonZeroApproxPixelsInDisk(Vector2d diskCenter, double diskRadius, Vector2d emptyDiskCenter){
        final List<PositionValue> list = new ArrayList<>();
        double approxPixWidth = getApproxPixelWidth();
        final double pixelDiagonal = approxPixWidth * Math.sqrt(2);

        Vector2i start = lowerLeftApproxIndex(new Vector2d(diskCenter.x - diskRadius, diskCenter.y - diskRadius));
        Vector2i end = upperRightApproxIndex(new Vector2d(diskCenter.x + diskRadius, diskCenter.y + diskRadius));

        // clip to visible space
        start = clipToScreen(start);
        end = clipToScreen(end);

        Vector2d pixelOrigin = toScreenCoords(new Vector2i(0,0));
        Vector2d unitYDir = toScreenCoords(new Vector2i(0,1)).subtract(pixelOrigin);
        double unitYDirLength = unitYDir.length();

        // iterate through positions 
        for(int x = start.x; x <= end.x; x++){
            boolean skippedMiddle = false; // (as in the middle of a circle)
            for(int y = start.y; y <= end.y; y++){
                
                Vector2d realCenterPos = toScreenCoords(new Vector2i(x, y));
                
                // this next if statement is purely for efficiency purposes. This is one of the slowest functions in the whole program.
                if(emptyDiskCenter != null){
                    Vector2d emptyDiskOffset = emptyDiskCenter.subtract(realCenterPos);
                    if(!skippedMiddle && emptyDiskOffset.length() < diskRadius - pixelDiagonal && emptyDiskOffset.dot(unitYDir) > 0){
                        skippedMiddle = true;
                        // math time!
                        double yToTravel = emptyDiskOffset.projectOnto(unitYDir).length() * 2 / unitYDirLength;
                        y += Math.max(0, Math.floor(yToTravel) - 1);
                        if(y > end.y){
                            break;
                        }
                    }
                }

                Vector2d offset = realCenterPos.subtract(diskCenter);
                double centerDist = offset.length();
                if(centerDist <= diskRadius + pixelDiagonal / 2 && screen[x][y] > 0){
                    // we need to calculate the proportion of the pixel that is inside the disk
                    float prop = 1;
                    if(centerDist > diskRadius - pixelDiagonal){
                        // i.e. there is some part of the pixel not in the circle
                        // this, btw, is a legit terrible approximation. But good enough.
                        prop = 0.5f + (float) ((diskRadius - centerDist) /  (1.2 * approxPixWidth));
                        prop = Math.max(0, prop);
                        prop = Math.min(1, prop);
                    }
                    list.add(new PositionValue(new Vector2i(x,y), prop));
                }
            }
        }

        return list;
    }

    /**
     * Changes all the pixels in a disk to match the given distribution.
     * @param diskCenter The center of the disk, in real coordinates.
     * @param diskRadius The radius of the disk, in real coordinates.
     * @param distribution The function f calculating the new distribution. Note that this function expects coordinates 
     *                     with reference to the center of the disk. Ex. (x, y) -> Math.sqrt(x*x + y*y) would have 
     *                     smallest value at the center of the disk.
     */
    private void changeAluminumDistributionInDisk(Vector2d diskCenter, double diskRadius, DistributionFunction distribution){
        iterateScreenDisk(diskCenter, diskRadius, (x, y, realPos) -> {
            Vector2d offset = realPos.subtract(diskCenter);
            if(offset.length() <= diskRadius){
                screen[x][y] = distribution.eval(offset.x, offset.y);
            }
        });
    }

    /**
     * A helper function to abstract doing things over a disk of pixels. Takes care of deciding whether a certain 
     * pixel lies in a disk, etc.
     * @param diskCenter The center of the disk, in real coordinates.
     * @param diskRadius The radius of the disk, in real coordinates.
     * @param action The action one wishes to do at each pixel in the disk. Accepts pixel coordinates x and y 
     *               and a Vector2d real position as arguments.
     */
    private void iterateScreenDisk(Vector2d diskCenter, double diskRadius, ScreenInteraction action){
        Vector2i start = lowerLeftApproxIndex(new Vector2d(diskCenter.x - diskRadius, diskCenter.y - diskRadius));
        Vector2i end = upperRightApproxIndex(new Vector2d(diskCenter.x + diskRadius, diskCenter.y + diskRadius));

        // clip to visible space
        start = clipToScreen(start);
        end = clipToScreen(end);

        // iterate through positions 
        for(int x = start.x; x <= end.x; x++){
            for(int y = start.y; y <= end.y; y++){
                // apply distribution 
                Vector2d screenPt = toScreenCoords(new Vector2i(x, y));
                action.execute(x, y, screenPt);
            }
        }
    }

    /**
     * Finds the pixel that is the nearest pixel with lower real x and y coordinates than the given point.
     * @param screenCoords Any (real) point on the etch-a-sketch.
     * @return The pixel coordinates of the lower left approximation pixel. 
     */
    private Vector2i lowerLeftApproxIndex(Vector2d screenCoords){
        // screen[0][0] is defined to be at (0,0)
        // screen[maxX][maxY] is defined be at screenExtent 
        int maxX = screen.length - 1;
        int maxY = screen[0].length - 1;
        double xPercent = screenCoords.x / screenExtent.x;
        double yPercent = screenCoords.y / screenExtent.y;

        return new Vector2i((int) Math.floor(xPercent * maxX), (int) Math.floor(yPercent * maxY));
    } 
    /**
     * Finds the pixel that is the nearest pixel with higher real x and y coordinates than the given point.
     * @param screenCoords Any (real) point on the etch-a-sketch.
     * @return The pixel coordinates of the upper right approximation pixel.
     */
    private Vector2i upperRightApproxIndex(Vector2d screenCoords){
        int maxX = screen.length - 1;
        int maxY = screen[0].length - 1;
        double xPercent = screenCoords.x / screenExtent.x;
        double yPercent = screenCoords.y / screenExtent.y;

        return new Vector2i((int) Math.ceil(xPercent * maxX), (int) Math.ceil(yPercent * maxY));
    }
    /**
     * Transforms pixel coordinates to screen coordinates.
     * @param approxCoords The (x,y) coordinates of the pixel (index in screen)
     * @return The position of the center of pixel, in screen coordinates.
     */
    private Vector2d toScreenCoords(Vector2i approxCoords){
        int maxX = screen.length - 1;
        int maxY = screen[0].length - 1;

        double propX = approxCoords.x / ((double) maxX);
        double propY = approxCoords.y / ((double) maxY);

        return new Vector2d(propX * screenExtent.x, propY * screenExtent.y);
    }
    /**
     * @return Retrieves the value of toScreenCoords((1,0)).x - toScreenCoords((0,0)).x
     */
    private double getApproxPixelWidth(){
        return screenExtent.x / (screen.length - 1);
    }
    private Vector2i clipToScreen(Vector2i point){
        int maxX = screen.length - 1;
        int maxY = screen[0].length - 1;
        int newX = Math.max(0, point.x);
        newX = Math.min(maxX, newX);
        int newY = Math.max(0, point.y);
        newY = Math.min(maxY, newY);
        return new Vector2i(newX, newY);
    }

    private interface DistributionFunction {
        float eval(double relativeX, double relativeY);
    }
    private interface ScreenInteraction {
        void execute(int pixelX, int pixelY, Vector2d realPos);
    }
    private class PositionValue {
        Vector2i position;
        float value;
        PositionValue(Vector2i pos, float val){
            position = pos;
            value = val;
        }
        @Override
        public String toString(){
            return "pos: " + position + ", value: " + value;
        }
    }
    private class PositiondValue {
        Vector2d position;
        float value; 
        PositiondValue(Vector2d pos, float val){
            position = pos;
            value = val;
        }
    }
}