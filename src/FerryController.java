import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class FerryController extends Thread {
    private final Semaphore tollSemaphoreA = new Semaphore(2);
    private final Semaphore tollSemaphoreB = new Semaphore(2);
    private final Semaphore ferrSemaphore = new Semaphore(1);

    private final Queue<Vehicle> ferryQueue = new LinkedList<>();
    private final Queue<Vehicle> waQueueA = new LinkedList<>();
    private final Queue<Vehicle> waQueueB = new LinkedList<>();

    private final int FERRY_CAPACITY = 20;
    private int currentCapacity = 0;
    private int currentSide;

    private final AtomicInteger completedTrips = new AtomicInteger(0);

    public FerryController() {
        this.currentSide = (int)(Math.random() * 2);
    }

    private void ferryMustDepart() throws InterruptedException {
        Queue<Vehicle> sideQueue = currentSide == 0 ? waQueueA : waQueueB;

        if (!sideQueue.isEmpty() || currentCapacity == FERRY_CAPACITY) {
            ferrSemaphore.acquire();
            try {
                departFerry();
                dischargeFerry();
            } finally {
                ferrSemaphore.release();
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

    public void LoadVehicle(Vehicle vehicle) throws InterruptedException {
        ferrSemaphore.acquire();
        try {
            if (vehicle.getSide() == currentSide && currentCapacity + vehicle.getSize() <= FERRY_CAPACITY) {
                System.out.printf("%s is loading on the ferry on side %s (Current cap: %d)\n",
                        vehicle.getVehicleName(), currentSide == 0 ? "A" : "B", currentCapacity);

                Thread.sleep((int)(Math.random() * 1000 * vehicle.getSize()));
                currentCapacity += vehicle.getSize();
                ferryQueue.add(vehicle);

                synchronized (this) {
                    if (vehicle.getSide() == 0) waQueueA.remove(vehicle);
                    else waQueueB.remove(vehicle);
                }

                System.out.printf("%s has loaded on the ferry on side %s\n",
                        vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
        } finally {
            ferrSemaphore.release();
        }
    }

    private void departFerry() throws InterruptedException {
        System.out.printf("Ferry is departing from side %s\n", currentSide == 0 ? "A" : "B");
        Thread.sleep(500);
        this.currentSide = 1 - this.currentSide;
    }

    private void dischargeFerry() throws InterruptedException {
        Iterator<Vehicle> iterator = ferryQueue.iterator();
        int dischargedCount = 0;

        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            vehicle.setSide(currentSide);
            vehicle.incrementTripCount(); // ekledik
            System.out.printf("%s is discharging to side %s\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            Thread.sleep(1000);
            currentCapacity -= vehicle.getSize();
            iterator.remove();
        
            if (vehicle.getTripCount() < 2) {
                // Araç karşıya geçince tekrar bekleme kuyruğuna giriyor
                if (currentSide == 0) waQueueA.add(vehicle);
                else waQueueB.add(vehicle);
            } else {
                completedTrips.incrementAndGet(); // İşini bitirdiyse sayaç artıyor
            }
        }
        System.out.printf("Tamamlanan araçlar: %d/30\n", completedTrips.get());
        
    }
}
