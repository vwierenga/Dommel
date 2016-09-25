import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by Vincent on 9/25/2016.
 */
public class Company {
    private static final int amountOfSoftwareEngineers = 6;
    private static final int amountOfUsers = 10;
    private static final int softwareEngineerQueueLength = 3;
    private ArrayList<SoftwareEngineer> softwareEngineers;
    private ArrayList<User> users;
    private int softwareEngineersInQueue = 0;
    private int usersInQueue = 0;
    //private Semaphore ;

    public Company() {

    }
}
