package emulator.src;

/**
 * Utility class for integer vectors.
 * 
 * @author Alan Koval
 */
class Vector2i {
    int x,y;
    Vector2i() { }
    Vector2i(int x, int y){
        this.x = x;
        this.y = y;
    }
    Vector2i(Vector2i other){
        this.x = other.x;
        this.y = other.y;
    }
    Vector2i subtractLocal(Vector2i other){
        x -= other.x;
        y -= other.y;
        return this;
    }
    Vector2i subtract(Vector2i other){
        return new Vector2i(x - other.x, y - other.y);
    }
    double distance(Vector2i other){
        return subtract(other).length();
    }
    double length(){
        return Math.sqrt(x*x + y*y);
    }
    @Override
    public String toString(){
        return "Vector2i(" + x + ", " + y + ")";
    }
}