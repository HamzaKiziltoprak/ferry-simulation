abstract public class Vehicle extends Thread {
    private int side;
    private int size;
    private String name;
    private int tripCount = 0;
    private FerryController controller;
    private boolean hasPaidToll = false; // Gişe ödemesi için yeni değişken
    
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
                // Check if we've completed our trips during execution
                if (tripCount >= 2) {
                    break;
                }
                
                // Bu tarafta henüz ödeme yapılmadıysa gişeden geç
                if (!hasPaidToll) {
                    controller.ProcessToll(this);
                }
                
                controller.LoadVehicle(this);
                
                // Wait a bit before trying again if we haven't been loaded
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
    
    // Yeni getter ve setter metodları
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