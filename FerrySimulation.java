import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FerrySimulation {
    public static void main(String[] args) {
        System.out.println("Starting Ferry Simulation");
        FerryController ferryController = new FerryController();
        List<Vehicle> vehicles = createVehicles(ferryController);
        
        ExecutorService vehicleExecutor = Executors.newFixedThreadPool(30);
        ferryController.start();
        
        for (Vehicle vehicle : vehicles) {
            vehicleExecutor.execute(vehicle);
        }
        vehicleExecutor.shutdown();
        //Simulation is end avarage 2 min so if it takes 3 min send timeout message and end the simulation. 
        try {
            if (!vehicleExecutor.awaitTermination(3, TimeUnit.MINUTES)) {
                System.out.println("Timeout! Stopping simulation...");
                vehicleExecutor.shutdownNow();
            }
            // Stop the ferry controller
            ferryController.interrupt();
            ferryController.join(5000);
            
            if (ferryController.isAlive()) {
                System.out.println("Ferry controller did not stop properly");
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
            vehicleExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Simulation completed!");
    }
    
    //Method for the crate vehicle environment and all vehicles are work as a thread.
    private static List<Vehicle> createVehicles(FerryController controller) {
        List<Vehicle> vehicles = new ArrayList<>();

        for (int i = 1; i <= 12; i++) vehicles.add(new Car("Car-" + i, controller));
        for (int i = 1; i <= 10; i++) vehicles.add(new Minibus("Minibus-" + i, controller));
        for (int i = 1; i <= 8; i++) vehicles.add(new Truck("Truck-" + i, controller));
        
        System.out.println("Created " + vehicles.size() + " vehicles");
        return vehicles;
    }
}