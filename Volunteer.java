package foodwasterescue;

public class Volunteer extends User {
    public Volunteer(String name) {
        super(name, "VOLUNTEER");
    }

    @Override
    public String getDashboardTitle() {
        return "Volunteer Dashboard";
    }
}
