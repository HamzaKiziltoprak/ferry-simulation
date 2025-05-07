import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class FerrySimulation {
    private static final Semaphore tollSemaphoreA = new Semaphore(2);
    private static final Semaphore tollSemaphoreB = new Semaphore(2);
    private static final int FERRY_CAPACITY = 20;
    private static final int TOTAL_VEHICLES = 30;
    private static final AtomicInteger completedVehicles = new AtomicInteger(0);
    private static final CountDownLatch completionLatch = new CountDownLatch(TOTAL_VEHICLES);

    public static void main(String[] args) throws InterruptedException {
        FerryController ferryController = new FerryController();
        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_VEHICLES);

        for (int i = 1; i <= 12; i++) executor.execute(new Car("Car" + i, ferryController));
        for (int i = 1; i <= 10; i++) executor.execute(new Minibus("Minibus" + i, ferryController));
        for (int i = 1; i <= 8; i++) executor.execute(new Truck("Truck" + i, ferryController));

        new Thread(() -> {
            try {
                while (completedVehicles.get() < TOTAL_VEHICLES) {
                    ferryController.checkAndDepart();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        completionLatch.await();
        System.out.println("\nTüm araçlar işlemini tamamladı!");
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    static class FerryController {
        private int currentLoad = 0;
        private int currentSide = 0;
        private long lastDepartureTime = System.currentTimeMillis();
        private final Lock lock = new ReentrantLock();
        private final Condition ferryCondition = lock.newCondition();
        private final BlockingQueue<Vehicle> waitingQueueA = new LinkedBlockingQueue<>();
        private final BlockingQueue<Vehicle> waitingQueueB = new LinkedBlockingQueue<>();

        public void processVehicle(Vehicle vehicle) throws InterruptedException {
            Semaphore toll = (vehicle.currentSide == 0) ? tollSemaphoreA : tollSemaphoreB;
            toll.acquire();
            try {
                System.out.printf("%s %s tarafı gişesinde%n", vehicle.name, (vehicle.currentSide == 0 ? "A" : "B"));
                Thread.sleep(500);
                if (vehicle.currentSide == 0) {
                    waitingQueueA.put(vehicle);
                } else {
                    waitingQueueB.put(vehicle);
                }
            } finally {
                toll.release();
            }
        }

        public void boardVehicle(Vehicle vehicle) throws InterruptedException {
            lock.lock();
            try {
                BlockingQueue<Vehicle> queue = (vehicle.currentSide == 0) ? waitingQueueA : waitingQueueB;
                while (vehicle.currentSide != currentSide || currentLoad + vehicle.size > FERRY_CAPACITY || !queue.contains(vehicle)) {
                    ferryCondition.await();
                }

                // Kuyruktan çıkar
                queue.remove(vehicle);
                currentLoad += vehicle.size;
                System.out.printf("%s feribota bindi. Yük: %d/%d%n", vehicle.name, currentLoad, FERRY_CAPACITY);
            } finally {
                lock.unlock();
            }
        }

        public void checkAndDepart() throws InterruptedException {
            lock.lock();
            try {
                BlockingQueue<Vehicle> currentQueue = (currentSide == 0) ? waitingQueueA : waitingQueueB;

                boolean hasWaitingVehicles = !currentQueue.isEmpty();
                boolean capacityHalfOrMore = currentLoad >= FERRY_CAPACITY * 0.5;
                boolean waitedLongEnough = System.currentTimeMillis() - lastDepartureTime > 5000;

                if (currentLoad > 0 && (capacityHalfOrMore || waitedLongEnough)) {
                    System.out.printf("%nFeribot %s tarafından hareket ediyor (Yük: %d/%d)%n",
                            (currentSide == 0 ? "A" : "B"), currentLoad, FERRY_CAPACITY);
                    Thread.sleep(3000);

                    currentSide = 1 - currentSide;
                    currentLoad = 0;
                    lastDepartureTime = System.currentTimeMillis();
                    System.out.printf("Feribot %s tarafına ulaştı%n", (currentSide == 0 ? "A" : "B"));

                    ferryCondition.signalAll();
                } else if (hasWaitingVehicles) {
                    ferryCondition.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    abstract static class Vehicle implements Runnable {
        final String name;
        final int size;
        int currentSide;
        int tripCount = 0;
        final FerryController controller;

        public Vehicle(String name, int size, FerryController controller) {
            this.name = name;
            this.size = size;
            this.currentSide = ThreadLocalRandom.current().nextInt(0, 2);
            this.controller = controller;
        }

        @Override
        public void run() {
            try {
                while (tripCount < 2) {
                    controller.processVehicle(this);
                    controller.boardVehicle(this);

                    currentSide = 1 - currentSide;
                    tripCount++;

                    if (tripCount < 2) {
                        System.out.printf("%s %s tarafında bekliyor%n", name, (currentSide == 0 ? "A" : "B"));
                        Thread.sleep(2000);
                    }
                }

                int completed = completedVehicles.incrementAndGet();
                System.out.printf("%s tamamlandı (%d/%d)%n", name, completed, TOTAL_VEHICLES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        }
    }

    static class Car extends Vehicle {
        public Car(String name, FerryController controller) {
            super(name, 1, controller);
        }
    }

    static class Minibus extends Vehicle {
        public Minibus(String name, FerryController controller) {
            super(name, 2, controller);
        }
    }

    static class Truck extends Vehicle {
        public Truck(String name, FerryController controller) {
            super(name, 3, controller);
        }
    }
}