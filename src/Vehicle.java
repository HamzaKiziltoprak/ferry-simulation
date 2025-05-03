abstract public class Vehicle extends Thread{
    private int side;
    private int size;
    private String Name;
    private int tripCount = 0;
    private FerryController controller;


    public Vehicle(int size,String name,FerryController controller){

        this.size = size;
        this.Name = name;
        this.controller = controller;
        this.side = (int)(Math.random()*2); // 0 or 1
    }

    @Override
    public void run(){
        while (this.tripCount < 2) {
            try {
                controller.ProcessToll(this);
                controller.LoadVehicle(this);
                
                this.tripCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getSide() {
        return side;
    }

    public int getSize() {
        return size;
    }

    public String getVehicleName() {
        return Name;
    }

    public void setSide(int side) {
        this.side = side;
        
    }
    
}

