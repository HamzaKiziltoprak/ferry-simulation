import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class FerryController extends Thread {
    private final Semaphore tollSemaphoreA = new Semaphore(2);
    private final Semaphore tollSemaphoreB = new Semaphore(2);
    private final Semaphore ferrySemaphore = new Semaphore(1);  // Fixed typo: ferrSemaphore -> ferrySemaphore

    private final Queue<Vehicle> ferryQueue = new LinkedList<>();
    private final Queue<Vehicle> waQueueA = new LinkedList<>();
    private final Queue<Vehicle> waQueueB = new LinkedList<>();

    private final int FERRY_CAPACITY = 20;
    private int currentCapacity = 0;
    private int currentSide;

    private final AtomicInteger completedTrips = new AtomicInteger(0);
    private final int TOTAL_TRIPS_NEEDED = 30; // 30 vehicles, each needs to make 2 trips

    public FerryController() {
        this.currentSide = (int)(Math.random() * 2);
    }

    private synchronized void ferryMustDepart() throws InterruptedException {
        Queue<Vehicle> sideQueue = currentSide == 0 ? waQueueA : waQueueB;

        // Check if we need to depart (queue has vehicles or ferry is full)
        if (!sideQueue.isEmpty() || currentCapacity > 0) {
            ferrySemaphore.acquire();
            try {
                // Only depart if we have loaded vehicles
                if (currentCapacity > 0) {
                    departFerry();
                    dischargeFerry();
                }
                // If all trips are complete, end the simulation
                if (completedTrips.get() >= TOTAL_TRIPS_NEEDED) {
                    System.out.println("All vehicles have completed their trips!");
                    Thread.currentThread().interrupt();
                }
            } finally {
                ferrySemaphore.release();
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                ferryMustDepart();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void ProcessToll(Vehicle vehicle) throws InterruptedException {
        Semaphore lock = vehicle.getSide() == 0 ? tollSemaphoreA : tollSemaphoreB;
        lock.acquire();
        try {
            System.out.printf("%s is waiting for the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
            Thread.sleep((int)(Math.random() * 1000));
            System.out.printf("%s has paid the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");

            synchronized (this) {
                if (vehicle.getSide() == 0) waQueueA.add(vehicle);
                else waQueueB.add(vehicle);
            }
        } finally {
            lock.release();
        }
    }

    public synchronized void LoadVehicle(Vehicle vehicle) throws InterruptedException {
        // Check if the vehicle is already on the ferry or has completed trips
        if (ferryQueue.contains(vehicle) || vehicle.getTripCount() >= 2) {
            return;
        }
        
        ferrySemaphore.acquire();
        try {
            // Re-check that the vehicle is in the correct waiting queue
            Queue<Vehicle> waitQueue = vehicle.getSide() == 0 ? waQueueA : waQueueB;
            if (!waitQueue.contains(vehicle)) {
                return;
            }
            
            if (vehicle.getSide() == currentSide && currentCapacity + vehicle.getSize() <= FERRY_CAPACITY) {
                System.out.printf("%s is loading on the ferry on side %s (Current cap: %d)\n",
                        vehicle.getVehicleName(), currentSide == 0 ? "A" : "B", currentCapacity);

                Thread.sleep((int)(Math.random() * 1000 * vehicle.getSize()));
                currentCapacity += vehicle.getSize();
                ferryQueue.add(vehicle);

                // Remove vehicle from waiting queue
                if (vehicle.getSide() == 0) waQueueA.remove(vehicle);
                else waQueueB.remove(vehicle);

                System.out.printf("%s has loaded on the ferry on side %s\n",
                        vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
        } finally {
            ferrySemaphore.release();
        }
    }

    private void departFerry() throws InterruptedException {
        System.out.printf("Ferry is departing from side %s with %d vehicles\n", 
                currentSide == 0 ? "A" : "B", ferryQueue.size());
        Thread.sleep(500);
        this.currentSide = 1 - this.currentSide;
    }

    private synchronized void dischargeFerry() throws InterruptedException {
        List<Vehicle> vehiclesToRemove = new ArrayList<>(ferryQueue);
        
        for (Vehicle vehicle : vehiclesToRemove) {
            vehicle.setSide(currentSide);
            vehicle.incrementTripCount();
            
            System.out.printf("%s is discharging to side %s (Trip count: %d)\n", 
                    vehicle.getVehicleName(), currentSide == 0 ? "A" : "B", vehicle.getTripCount());
            Thread.sleep(500);  // Reduced discharge time for faster simulation
            currentCapacity -= vehicle.getSize();
            ferryQueue.remove(vehicle);
            
            // Check if vehicle has completed all trips
            if (vehicle.getTripCount() >= 2) {
                int completed = completedTrips.incrementAndGet();
                System.out.printf("%s has completed all trips! (%d/%d vehicles done)\n", 
                        vehicle.getVehicleName(), completed, TOTAL_TRIPS_NEEDED);
            } else {
                // Add to waiting queue on new side
                if (currentSide == 0) waQueueA.add(vehicle);
                else waQueueB.add(vehicle);
                System.out.printf("%s is now waiting on side %s for next trip\n", 
                        vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
        }
        
        System.out.printf("All vehicles discharged. Completed vehicles: %d/%d\n", 
                completedTrips.get(), TOTAL_TRIPS_NEEDED);
    }
}