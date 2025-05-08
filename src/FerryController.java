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

    private final AtomicInteger completedTrips = new AtomicInteger(0);
    private final int TOTAL_TRIPS_NEEDED = 30; // 30 vehicles, each needs to make 2 trips
    
    // Simülasyon zaman takibi için başlangıç zamanı
    private final long startTime = System.currentTimeMillis();
    
    public FerryController() {
        this.currentSide = (int)(Math.random() * 2);
    }
    
    // Geçen süreyi saniye cinsinden hesaplayan yardımcı metot
    private double getElapsedTimeInSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Ferry işleme döngüsü
                Thread.sleep(2000);  // Feribot yolculuk süresi

                // Ferry'yi boşalt
                dischargeFerry();
                
                // Ferry'yi doldurmak için zaman ver (maksimum 10 saniye)
                Thread.sleep(10000);
                
                // Ferry'yi hareket ettir
                departFerry();
            }
        } catch (InterruptedException e) {
            System.out.println("Ferry controller interrupted! [" + getElapsedTimeInSeconds() + "s]");
            Thread.currentThread().interrupt();
        }
    }

    public void ProcessToll(Vehicle vehicle) throws InterruptedException {
        // Araç zaten feribotta veya gişeyi geçmişse atlayın
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
            
            vehicle.setHasPaidToll(true);  // Bu aracın gişeyi geçtiğini işaretle
            
            synchronized (this) {
                if (vehicle.getSide() == 0) waQueueA.add(vehicle);
                else waQueueB.add(vehicle);
            }
        } finally {
            lock.release();
        }
    }

    public synchronized void LoadVehicle(Vehicle vehicle) throws InterruptedException {
        // Araç zaten feribotta mı kontrol et
        if (ferryQueue.contains(vehicle)) {
            return;
        }
        
        // Araç doğru tarafta mı ve feribota binmek için gişeyi geçmiş mi?
        if (vehicle.getSide() != currentSide || !vehicle.hasPaidToll()) {
            return;
        }
        
        ferrySemaphore.acquire();
        try {
            // Feribot hala kapasitesi dahilinde mi?
            if (vehicle.getSide() == currentSide && currentCapacity + vehicle.getSize() <= FERRY_CAPACITY) {
                System.out.printf("%s is loading on the ferry on side %s (Current cap: %d) [%.1fs]\n",
                        vehicle.getVehicleName(), 
                        currentSide == 0 ? "A" : "B", 
                        currentCapacity,
                        getElapsedTimeInSeconds());

                Thread.sleep((int)(Math.random() * 1000 * vehicle.getSize()));
                currentCapacity += vehicle.getSize();
                ferryQueue.add(vehicle);

                // Araçları bekleme kuyruğundan çıkar
                if (vehicle.getSide() == 0) waQueueA.remove(vehicle);
                else waQueueB.remove(vehicle);

                System.out.printf("%s has loaded on the ferry on side %s [%.1fs]\n",
                        vehicle.getVehicleName(), 
                        currentSide == 0 ? "A" : "B",
                        getElapsedTimeInSeconds());
            }
        } finally {
            ferrySemaphore.release();
        }
    }

    private void departFerry() throws InterruptedException {
        // Eğer feribotta hiç araç yoksa hareket etme
        if (ferryQueue.isEmpty()) {
            System.out.printf("Ferry is empty, waiting for vehicles on side %s [%.1fs]\n", 
                    currentSide == 0 ? "A" : "B", 
                    getElapsedTimeInSeconds());
            return;
        }
        
        // Kapasite kullanım yüzdesini hesapla
        double capacityPercentage = (currentCapacity * 100.0) / FERRY_CAPACITY;
        
        System.out.printf("Ferry is departing from side %s with %d vehicles (Capacity: %d/%d units, %.1f%% full) [%.1fs]\n", 
                currentSide == 0 ? "A" : "B", 
                ferryQueue.size(),
                currentCapacity, 
                FERRY_CAPACITY,
                capacityPercentage,
                getElapsedTimeInSeconds());
                
        Thread.sleep(500);
        this.currentSide = 1 - this.currentSide;
        
        System.out.printf("Ferry is now traveling [%.1fs]\n", getElapsedTimeInSeconds());
    }

    private synchronized void dischargeFerry() throws InterruptedException {
        List<Vehicle> vehiclesToRemove = new ArrayList<>(ferryQueue);
        
        System.out.printf("Ferry has arrived at side %s [%.1fs]\n", 
                currentSide == 0 ? "A" : "B",
                getElapsedTimeInSeconds());
        
        for (Vehicle vehicle : vehiclesToRemove) {
            vehicle.setSide(currentSide);
            vehicle.incrementTripCount();
            vehicle.setHasPaidToll(false);  // Yeni tarafta gişe ödeme durumunu sıfırla
            
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