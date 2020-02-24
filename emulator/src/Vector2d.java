package emulator.src;

/**
 * Utility class for floating point 2D vectors.
 * 
 * @author Alan Koval
 */
public class Vector2d {
    public double x,y;
    public Vector2d(){ }

    public Vector2d(Vector2d other){
        x = other.x;
        y = other.y;
    }
    public Vector2d(Vector2i other){
        x = other.x;
        y = other.y;
    }

    public Vector2d(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Vector2d add(Vector2d other){
        return new Vector2d(x + other.x, y + other.y);
    }
    public Vector2d addLocal(Vector2d other){
        x += other.x;
        y += other.y;
        return this;
    }

    public Vector2d subtract(Vector2d other){
        return new Vector2d(x - other.x, y - other.y);
    }
    public Vector2d subtractLocal(Vector2d other){
        x -= other.x;
        y -= other.y;
        return this;
    }

    public Vector2d mult(double scale){
        return new Vector2d(x * scale, y * scale);
    }
    public Vector2d multLocal(double scale){
        x *= scale;
        y *= scale;
        return this;
    }

    public double dot(Vector2d other){
        return x * other.x + y * other.y;
    }
    public double length(){
        return Math.sqrt(x*x + y*y);
    }
    public double distance(Vector2d other){
        return subtract(other).length();
    }
    public Vector2d normalizeLocal(){
        double L = length();
        if(L == 0){
            x = 0;
            y = 0;
        } else {
            x /= L;
            y /= L;
        }
        return this;
    }
    public Vector2d normalize(){
        double L = length();
        if(L == 0){
            return new Vector2d(0,0);
        }
        return mult(1 / L);
    }
    public Vector2d projectOnto(Vector2d other){
        if(other.x * other.x + other.y * other.y == 0){
            return new Vector2d();
        }
        return other.mult(dot(other)/other.dot(other));
    }

    @Override
    public String toString(){
        return "Vector2d(" + x + "," + y + ")";
    }
}