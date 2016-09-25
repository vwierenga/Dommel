import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by Vincent on 9/25/2016.
 */
public class Dommel {
    private static final int amountOfSoftwareEngineers = 6;
    private static final int amountOfUsers = 10;
    private static final int softwareEngineerQueueLength = 3;
    private ArrayList<SoftwareEngineer> softwareEngineers;
    private ArrayList<User> users;
    private int softwareEngineersInQueue = 0;
    private int usersInQueue = 0;
    private Semaphore waitingUser, userMeetingInvitation, test;

    public static void main(String [] args)
	{
        new Dommel().run();
    }

    public void run() {
        System.out.println("test");
        userMeetingInvitation = new Semaphore(0);
        waitingUser = new Semaphore(0);

        Jaap jaap = new Jaap();
        User user1 = new User();

        Thread jaapThread = new Thread(jaap);
        Thread user1Thread = new Thread(user1);

        jaapThread.start();
        user1Thread.start();


        try{
            jaapThread.join();
            user1Thread.join();


        } catch (InterruptedException e){

        }
    }

    class Jaap implements Runnable {

        public Jaap() {
        }

        @Override
        public void run() {
            System.out.println("Jaap Begin");
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 1000));
                    waitingUser.acquire();
                    userMeetingInvitation.release();
                    System.out.println("Jaap");
                } catch (InterruptedException e) {

                }
            }
        }
    }

    class SoftwareEngineer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000);
                    System.out.println("");
                } catch (InterruptedException e) {

                }
            }
        }
    }

    class User implements Runnable {
        @Override
        public void run() {
            System.out.println("Customer Begin");
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 1000));
                    waitingUser.release();
                    userMeetingInvitation.acquire();
                    System.out.println("User");
                } catch (InterruptedException e) {

                }
            }
        }
    }
}
