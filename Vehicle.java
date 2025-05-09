abstract public class Vehicle extends Thread {
    private int side;
    private int size;
    private String name;
    private int tripCount = 0;
    private FerryController controller;
    private boolean hasPaidToll = false;
    
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
                if (tripCount >= 2) {
                    break;
                }
                
                if (!hasPaidToll) {
                    controller.ProcessToll(this);
                }
                
                controller.LoadVehicle(this);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(name + " thread terminating. Trip count: " + tripCount);
    }
    
    public int getSide() { return side; }
    public int getSize() { return size; }
    public String getVehicleName() { return name; }
    public void setSide(int side) { this.side = side; }

    public boolean hasPaidToll() {
        return hasPaidToll;
    }
    
    public void setHasPaidToll(boolean hasPaidToll) {
        this.hasPaidToll = hasPaidToll;
    }
    
    public synchronized int getTripCount() {
        return tripCount;
    }
    
    public synchronized void incrementTripCount() {
        tripCount++;
    }
}