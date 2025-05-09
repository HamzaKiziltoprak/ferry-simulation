import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FerrySimulation {
    public static void main(String[] args) {
        FerryController ferryController = new FerryController();
        List<Vehicle> vehicles = createVehicles(ferryController);

        ExecutorService vehicleExecutor = Executors.newFixedThreadPool(30);
        ferryController.start();

        for (Vehicle vehicle : vehicles) {
            vehicleExecutor.execute(vehicle);
        }

        vehicleExecutor.shutdown();
        try {
            if (!vehicleExecutor.awaitTermination(2, TimeUnit.MINUTES)) {
                vehicleExecutor.shutdownNow();
            }
            ferryController.interrupt();
            ferryController.join();
        } catch (InterruptedException e) {
            vehicleExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Simülasyon tamamlandı!");
    }

    private static List<Vehicle> createVehicles(FerryController controller) {
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= 12; i++) vehicles.add(new Car("Car-" + i, controller));
        for (int i = 1; i <= 10; i++) vehicles.add(new Minibus("Minibus-" + i, controller));
        for (int i = 1; i <= 8; i++) vehicles.add(new Truck("Truck-" + i, controller));
        return vehicles;
    }
}
