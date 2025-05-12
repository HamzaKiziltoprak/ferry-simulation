import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class FerryController extends Thread {
    private final Semaphore tollSemaphoreA = new Semaphore(2);
    private final Semaphore tollSemaphoreB = new Semaphore(2);
    private final Semaphore ferrySemaphore = new Semaphore(1);

    private final Queue<Vehicle> ferryQueue = new LinkedList<>();
    private final Queue<Vehicle> waQueueA = new LinkedList<>();
    private final Queue<Vehicle> waQueueB = new LinkedList<>();

    private final int FERRY_CAPACITY = 20;
    private int currentCapacity = 0;
    private int currentSide;
    private boolean isInTransit = false;
    private final AtomicInteger completedTrips = new AtomicInteger(0);
    private final int TOTAL_TRIPS_NEEDED = 30;
    private final long startTime = System.currentTimeMillis();
    
    //Method for the ferry start random side
    public FerryController() {
        this.currentSide = (int)(Math.random() * 2);
    }
    //Method for the get time before output print
    private double getElapsedTimeInSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
    //Loop thread runs until work done
@Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Ferry is now at dock and ready for loading
                isInTransit = false;
                
                // Give time for vehicles to load
                Thread.sleep(10000);

                // Depart ferry (sets isInTransit = true)
                departFerry();
                
                // Travel time
                Thread.sleep(2000);
                
                // Discharge ferry when arrived
                dischargeFerry();
            }
        } catch (InterruptedException e) {
            System.out.println("Ferry controller interrupted! [" + getElapsedTimeInSeconds() + "s]");
            Thread.currentThread().interrupt();
        }
    }
    //Method of the toll process return waiting in toll information and paid toll fee information
    public void ProcessToll(Vehicle vehicle) throws InterruptedException {
        if(ferryQueue.contains(vehicle) || vehicle.hasPaidToll()) {
            return;
        }
        
        Semaphore lock = vehicle.getSide() == 0 ? tollSemaphoreA : tollSemaphoreB;
        lock.acquire();
        try {
            System.out.printf("%s is waiting for the toll on side %s [%.1fs]\n", 
                vehicle.getVehicleName(), 
                vehicle.getSide() == 0 ? "A" : "B", 
                getElapsedTimeInSeconds());
                
            Thread.sleep((int)(Math.random() * 1000));
            
            System.out.printf("%s has paid the toll on side %s [%.1fs]\n", 
                vehicle.getVehicleName(), 
                vehicle.getSide() == 0 ? "A" : "B", 
                getElapsedTimeInSeconds());
            
            vehicle.setHasPaidToll(true);
            
            synchronized (this) {
                if (vehicle.getSide() == 0) waQueueA.add(vehicle);
                else waQueueB.add(vehicle);
            }
        } finally {
            lock.release();
        }
    }
    //Method for the vehicles load to the ferry work as synchronized
public synchronized void LoadVehicle(Vehicle vehicle) throws InterruptedException {
    if (ferryQueue.contains(vehicle)) {
        return;
    }

    // İlk kontrol - feribot transit durumundaysa veya araç uygun değilse yükleme yapma
    if (vehicle.getSide() != currentSide || !vehicle.hasPaidToll() || isInTransit) {
        return;
    }
    
    ferrySemaphore.acquire();
    try {
        // Transit durumunu ve diğer koşulları tekrar kontrol et
        if (!isInTransit && vehicle.getSide() == currentSide && currentCapacity + vehicle.getSize() <= FERRY_CAPACITY) {
            System.out.printf("%s is loading on the ferry on side %s (Current cap: %d) [%.1fs]\n",
                    vehicle.getVehicleName(), 
                    currentSide == 0 ? "A" : "B", 
                    currentCapacity,
                    getElapsedTimeInSeconds());

            Thread.sleep((int)(Math.random() * 1000 * vehicle.getSize()));
            currentCapacity += vehicle.getSize();
            ferryQueue.add(vehicle);
            if (vehicle.getSide() == 0) waQueueA.remove(vehicle);
            else waQueueB.remove(vehicle);

            System.out.printf("%s has loaded on the ferry on side %s [%.1fs][%d/%d]\n",
                    vehicle.getVehicleName(), 
                    currentSide == 0 ? "A" : "B",
                    getElapsedTimeInSeconds(),
                    currentCapacity,FERRY_CAPACITY);
        }
    } finally {
        ferrySemaphore.release();
    }
}
    //Method for the ferry depart this method controls if ferry is empty it can't move until least a car load to the ferry
private void departFerry() throws InterruptedException {
    // Feribot hareket etmeden önce semafor al
    ferrySemaphore.acquire();
    try {
        if (ferryQueue.isEmpty()) {
            System.out.printf("Ferry is empty, waiting for vehicles on side %s [%.1fs]\n", 
                    currentSide == 0 ? "A" : "B", 
                    getElapsedTimeInSeconds());
            return;
        }

        // Transit durumunu güvenli şekilde ayarla
        isInTransit = true;
        
        double capacityPercentage = (currentCapacity * 100.0) / FERRY_CAPACITY;
        
        System.out.printf("Ferry is departing from side %s with %d vehicles (Capacity: %d/%d units, %.1f%% full) [%.1fs]\n", 
                currentSide == 0 ? "A" : "B", 
                ferryQueue.size(),
                currentCapacity, 
                FERRY_CAPACITY,
                capacityPercentage,
                getElapsedTimeInSeconds());
    } finally {
        ferrySemaphore.release();
    }
        Thread.sleep(500);
    this.currentSide = 1 - this.currentSide;
    
    System.out.printf("Ferry is now traveling [%.1fs]\n", getElapsedTimeInSeconds());
}
    //Method for the after departed the ferry to the other side, send vehicles out of area(vehicles have to enter toll queue again) and controls vehicles which it should be interrupted 
    private synchronized void dischargeFerry() throws InterruptedException {
        List<Vehicle> vehiclesToRemove = new ArrayList<>(ferryQueue);
        
        System.out.printf("Ferry has arrived at side %s [%.1fs]\n", 
                currentSide == 0 ? "A" : "B",
                getElapsedTimeInSeconds());
        
        for (Vehicle vehicle : vehiclesToRemove) {
            vehicle.setSide(currentSide);
            vehicle.incrementTripCount();
            vehicle.setHasPaidToll(false);
            
            System.out.printf("%s is discharging to side %s (Trip count: %d) [%.1fs]\n", 
                    vehicle.getVehicleName(), 
                    currentSide == 0 ? "A" : "B", 
                    vehicle.getTripCount(),
                    getElapsedTimeInSeconds());
                    
            Thread.sleep(500);
            currentCapacity -= vehicle.getSize();
            ferryQueue.remove(vehicle);
            
            if (vehicle.getTripCount() >= 2) {
                int completed = completedTrips.incrementAndGet();
                System.out.printf("%s has completed all trips! (%d/%d vehicles done) [%.1fs]\n", 
                        vehicle.getVehicleName(), 
                        completed, 
                        TOTAL_TRIPS_NEEDED,
                        getElapsedTimeInSeconds());
            } else {
                System.out.printf("%s is now on side %s and needs to go through toll again [%.1fs]\n", 
                        vehicle.getVehicleName(), 
                        currentSide == 0 ? "A" : "B",
                        getElapsedTimeInSeconds());
            }
        }
        
        System.out.printf("All vehicles discharged. Completed vehicles: %d/%d [%.1fs]\n", 
                completedTrips.get(), 
                TOTAL_TRIPS_NEEDED,
                getElapsedTimeInSeconds());
    }
}