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

    public void ProcessToll(Vehicle vehicle){
        Semaphore lock = vehicle.getSide() == 0 ? tollSemaphoreA : tollSemaphoreB;

        try{
            lock.acquire();
            System.out.printf("%s is waiting for the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
            Thread.sleep((int)Math.random() * 1000); // Simulate waiting for the toll
            System.out.printf("%s has paid the toll on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
            waQueueA.add(vehicle);
            lock.release();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void LoadVehicle(Vehicle vehicle){
        Semaphore ferryLoc = ferrSemaphore;
        try{
            if(currentCapacity + vehicle.getSize() <= FERRY_CAPACITY && vehicle.getSide() == currentSide){
                ferryLoc.acquire();
                System.out.printf("%s is loading on the ferry on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
                Thread.sleep((int)Math.random() * 1000*vehicle.getSize()); // Simulate loading time
                currentCapacity += vehicle.getSize();
                ferryQueue.add(vehicle);
                System.out.printf("%s has loaded on the ferry on side %s\n", vehicle.getVehicleName(), vehicle.getSide() == 0 ? "A" : "B");
                ferryLoc.release();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void departFerry(){
        Semaphore ferryLoc = ferrSemaphore;
        try{
            ferryLoc.acquire();
            for(Vehicle vehicle : ferryQueue){
                System.out.printf("%s is departing from side %s\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
                Thread.sleep((int)Math.random() * 1000); // Simulate travel time
                System.out.printf("%s has departed from side %s\n", vehicle.getVehicleName(), currentSide == 0 ? "A" : "B");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void dischargeFerry(){


    }
}
