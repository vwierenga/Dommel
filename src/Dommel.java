import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by Vincent on 9/25/2016.
 */
public class Dommel {
    private static final int amountOfSoftwareEngineers = 3; // should be 6
    private static final int amountOfUsers = 10;
    private static final int softwareEngineerQueueLength = 3;
    private ArrayList<SoftwareEngineer> softwareEngineers;
    private ArrayList<User> users;
    private int softwareEngineersInQueue = 0;
    private int softwareEngineersInMeetingRoom = 0;
    private int usersInQueue = 0;
    private boolean meetingInProgress = false;
    private Semaphore waitingUser, userCompanyInvitation, userAtCompany, userMeetingInvitation, userInMeetingRoom, waitingSoftwareEngineer, softwareEngineerInvitation, softwareEngineerInMeetingRoom, test;
    private Semaphore userQueueMutex, softwareEngineerQueueMutex, meetingMutex;

    public static void main(String [] args)
	{
        new Dommel().run();
    }

    public void run() {
        System.out.println("test");
        waitingUser = new Semaphore(0);
        userCompanyInvitation = new Semaphore(0);
        userAtCompany = new Semaphore(0);
        userMeetingInvitation = new Semaphore(0);
        userInMeetingRoom = new Semaphore(0);

        waitingSoftwareEngineer = new Semaphore(0);
        softwareEngineerInvitation = new Semaphore(0);
        softwareEngineerInMeetingRoom = new Semaphore(0);

        userQueueMutex = new Semaphore(1);

        softwareEngineerQueueMutex = new Semaphore(1);

        meetingMutex = new Semaphore(1);


        Jaap jaap = new Jaap();
        User user1 = new User();

        for(int i=0; i < amountOfSoftwareEngineers; i++){
            SoftwareEngineer engineer = new SoftwareEngineer();
            Thread engineerThread = new Thread(engineer);
            engineerThread.start();
        }

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
                    Thread.sleep((int) (Math.random() * 2000));
                    System.out.println("Jaap cycle");

                    if (waitingUser.tryAcquire()){
                        waitingSoftwareEngineer.acquire();

                        userQueueMutex.acquire();
                        int amount = usersInQueue;
                        userQueueMutex.release();

                        userCompanyInvitation.release(amount);
                        userAtCompany.acquire(amount);
                        softwareEngineerInvitation.release();
                        userMeetingInvitation.release(amount);

                        softwareEngineerInMeetingRoom.acquire();
                        userInMeetingRoom.acquire(amount);

                        userMeeting();

                    } else {
                        waitingSoftwareEngineer.acquire(3);
                        softwareEngineerInvitation.release(3);
                        softwareEngineerInMeetingRoom.acquire(3);
                        softwareEngineerMeeting();
                    }

                } catch (InterruptedException e) {

                }
            }
        }

        public void userMeeting() {

            System.out.println("user meeting in progress");
            try {
                meetingMutex.acquire();
                meetingInProgress = true;
                meetingMutex.release();
                Thread.sleep((int) (Math.random() * 3000));
                meetingMutex.acquire();
                meetingInProgress = false;
                meetingMutex.release();
            } catch (InterruptedException e) {

            }
        }

        public void softwareEngineerMeeting() {
            System.out.println("software engineer meeting in progress");
            try {
                meetingMutex.acquire();
                meetingInProgress = true;
                meetingMutex.release();
                Thread.sleep((int) (Math.random() * 3000));
                meetingMutex.acquire();
                meetingInProgress = false;
                meetingMutex.release();
            } catch (InterruptedException e) {

            }
        }
    }

    class SoftwareEngineer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 1000));

                    meetingMutex.acquire();
                    boolean meeting = meetingInProgress;
                    meetingMutex.release();

                    if(!meeting) {
                        waitingSoftwareEngineer.release();

                        softwareEngineerQueueMutex.acquire();
                        softwareEngineersInQueue++;
                        softwareEngineerQueueMutex.release();

                        System.out.println("SoftwareEngineer ready");
                        softwareEngineerInvitation.acquire();

                        softwareEngineerQueueMutex.acquire();
                        softwareEngineersInQueue--;
                        System.out.println("engineers in queue " + softwareEngineersInQueue);
                        softwareEngineerQueueMutex.release();

                        goToMeeting();
                    }
                } catch (InterruptedException e) {

                }
            }
        }

        public void goToMeeting(){
            try {
                Thread.sleep(100);
                softwareEngineerInMeetingRoom.release();
            } catch (InterruptedException e) {

            }
        }
    }

    class User implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 1000));
                    waitingUser.release();

                    userQueueMutex.acquire();
                    usersInQueue++;
                    userQueueMutex.release();

                    userCompanyInvitation.acquire();

                    userQueueMutex.acquire();
                    usersInQueue--;
                    userQueueMutex.release();

                    goToCompany();
                } catch (InterruptedException e) {

                }
            }
        }

        public void goToCompany(){

            try {
                Thread.sleep((int) (Math.random() * 1000));
                userAtCompany.release();
                userMeetingInvitation.acquire();
                goToMeeting();
            } catch (InterruptedException e) {

            }
        }

        public void goToMeeting(){
            try {
                Thread.sleep(100);
                userInMeetingRoom.release();
            } catch (InterruptedException e) {

            }
        }
    }
}
