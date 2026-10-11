package foodwasterescue;

public abstract class User {
    private final String name;
    private final String role;

    protected User(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() { return name; }
    public String getRole() { return role; }

    public abstract String getDashboardTitle();
}
