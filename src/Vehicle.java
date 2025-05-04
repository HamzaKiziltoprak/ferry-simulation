abstract public class Vehicle extends Thread {
    private int side;
    private int size;
    private String name;
    private int tripCount = 0;
    private FerryController controller;

    public Vehicle(int size, String name, FerryController controller) {
        this.size = size;
        this.name = name;
        this.controller = controller;
        this.side = (int)(Math.random() * 2);
    }

    @Override
    public void run() {
        while (tripCount < 2) {
            try {
                controller.ProcessToll(this);
                controller.LoadVehicle(this);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public int getSide() { return side; }
    public int getSize() { return size; }
    public String getVehicleName() { return name; }
    public void setSide(int side) { this.side = side; }

    public int getTripCount() {
        return tripCount;
    }
    
    public void incrementTripCount() {
        tripCount++;
    }
    
}
