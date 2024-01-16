package view;

public class BoundingBox {
    double x;
    double y;
    double width;
    double height;

    public BoundingBox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean containsPoint(double pX, double pY) {
        return pX >= x && pX <= x + width && pY >= y && pY <= y + height;
    }
}
