import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class FerryController extends Thread {
    private final Semaphore tollSemaphoreA = new Semaphore(2);
    private final Semaphore tollSemaphoreB = new Semaphore(2);

    private final Semaphore ferrSemaphore = new Semaphore(1);

    // keep ferry vehicles on the ferry
    private final Queue<Vehicle> ferryQueue = new LinkedList<>();

    // create witing queues for both sides 
    private final Queue<Vehicle> waQueueA = new LinkedList<>();
    private final Queue<Vehicle> waQueueB = new LinkedList<>();

    private final int FERRY_CAPACITY = 20;

    private int currentCapacity = 0;
    private int currentSide ;

    public FerryController(){
        this.currentSide = (int)Math.random()*2; // ferry starts on side A
    }

    public void ferryMustDepart() throws InterruptedException{

        Queue<Vehicle> sideQueue = currentSide == 0 ? waQueueA : waQueueB;
        if(sideQueue.isEmpty() || currentCapacity == FERRY_CAPACITY){
            ferrSemaphore.acquire();
            try {
                departFerry();
                dischargeFerry();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ferrSemaphore.release();
        }
    }

    @Override 
    public void run(){
        while(true){
            try {
                ferryMustDepart();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void ProcessToll(Vehicle vehicle) throws InterruptedException{
        Semaphore lock = vehicle.getSide() == 0 ? tollSemaphoreA : tollSemaphoreB;

        try{
            lock.acquire();
            System.out.printf("%s is waiting for the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
            Thread.sleep((int)Math.random() * 1000); // Simulate waiting for the toll
            System.out.printf("%s has paid the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
            if(vehicle.getSide() == 0){
                waQueueA.add(vehicle);
            }else{
                waQueueB.add(vehicle);
            }
            lock.release();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void LoadVehicle(Vehicle vehicle) throws InterruptedException{
        ferrSemaphore.acquire();;
        try{
            if(currentCapacity + vehicle.getSize() <= FERRY_CAPACITY && vehicle.getSide() == currentSide){
                System.out.printf("%s is loading on the ferry on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
                Thread.sleep((int)Math.random() * 1000*vehicle.getSize()); // Simulate loading time
                currentCapacity += vehicle.getSize();
                ferryQueue.add(vehicle);
                if(vehicle.getSide() == 0){
                    waQueueA.remove(vehicle);
                }else{
                    waQueueB.remove(vehicle);
                }
                System.out.printf("%s has loaded on the ferry on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
                ferrSemaphore.release();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void departFerry() throws InterruptedException{
        // Semaphore ferryLoc = ferrSemaphore;
        try{
            // ferryLoc.acquire();
            for(Vehicle vehicle : ferryQueue){
                System.out.printf("%s is departing from side %s\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
                Thread.sleep(3000); // Simulate travel time
                System.out.printf("%s has departed from side %s\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
            this.currentSide = 1-(this.currentSide); // Change ferry side
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void dischargeFerry() throws InterruptedException{
        // Semaphore ferryLoc = ferrSemaphore;
        // ferryLoc.acquire();
        try{
            for(Vehicle vehicle : ferryQueue){
                vehicle.setSide(currentSide); // Change side for the vehicle
                System.out.printf("%s is discharging from ferry to %s side\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
                Thread.sleep(1000); // Simulate unloading time
                currentCapacity -= vehicle.getSize();
                ferryQueue.remove(vehicle);
                System.out.printf("%s has reached to %s side\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }
        // ferryLoc.release();
    }
}
