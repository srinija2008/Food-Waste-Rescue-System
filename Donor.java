package foodwasterescue;

public class Donor extends User {
    public Donor(String name) {
        super(name, "DONOR");
    }

    @Override
    public String getDashboardTitle() {
        return "Food Donor Dashboard";
    }
}
