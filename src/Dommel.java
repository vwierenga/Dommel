import java.util.ArrayList;
import java.util.Random;
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
    private Semaphore waitingUser, userCompanyInvitation, userAtCompany, userMeetingInvitation, userInMeetingRoom, waitingSoftwareEngineer, softwareEngineerInvitation, softwareEngineerNeededInMeetingRoom, meetingFinished;
    private Semaphore userQueueMutex, softwareEngineerQueueMutex, meetingMutex;

    public static void main(String [] args)
	{
        new Dommel().run();
    }

    public void run() {
        waitingUser = new Semaphore(0);
        userCompanyInvitation = new Semaphore(0);
        userAtCompany = new Semaphore(0);
        userMeetingInvitation = new Semaphore(0);
        userInMeetingRoom = new Semaphore(0);

        waitingSoftwareEngineer = new Semaphore(0);
        softwareEngineerInvitation = new Semaphore(0);
        softwareEngineerNeededInMeetingRoom = new Semaphore(0);

        meetingFinished = new Semaphore(0);

        userQueueMutex = new Semaphore(1);

        softwareEngineerQueueMutex = new Semaphore(1);

        meetingMutex = new Semaphore(1);

        Jaap jaap = new Jaap();

        for(int i=0; i < amountOfUsers; i++){
            User user = new User();
            Thread userThread = new Thread(user);
            userThread.start();
        }

        for(int i=0; i < amountOfSoftwareEngineers; i++){
            SoftwareEngineer engineer = new SoftwareEngineer();
            Thread engineerThread = new Thread(engineer);
            engineerThread.start();
        }

        Thread jaapThread = new Thread(jaap);

        jaapThread.start();


        try{
            jaapThread.join();


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

                        softwareEngineerNeededInMeetingRoom.release();
                        userInMeetingRoom.acquire(amount);

                        userMeeting(amount + 1);

                    } else {
                        waitingSoftwareEngineer.acquire(3);
                        softwareEngineerInvitation.release(3);
                        softwareEngineerNeededInMeetingRoom.release(3);
                        softwareEngineerMeeting();
                    }

                } catch (InterruptedException e) {

                }
            }
        }

        public void userMeeting(int amountOfPeopleInMeeting) {

            System.out.println("user meeting in progress");
            try {
                meetingMutex.acquire();
                meetingInProgress = true;
                meetingMutex.release();
                Thread.sleep((int) (Math.random() * 3000));
                meetingMutex.acquire();
                meetingInProgress = false;
                meetingMutex.release();

                meetingFinished.release(amountOfPeopleInMeeting);
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

                meetingFinished.release(3);
            } catch (InterruptedException e) {

            }
        }
    }

    class SoftwareEngineer implements Runnable {
        private int state = 0; // 0 = working, 1 = available, 2 = in meeting

        @Override
        public void run() {
            while (true) {
                if (state == 0){
                    work();
                    state = new Random().nextInt(2);
                } else if (state == 1) {
                    notifyAvailable();
                } else {
                    try {
                        meetingFinished.acquire();
                        state = 1;
                    } catch (InterruptedException e) {

                    }
                }
            }
        }

        public void goToMeeting(){
            try {
                Thread.sleep(100);
                if (softwareEngineerNeededInMeetingRoom.tryAcquire()) {
                    state = 2;
                } else {
                    state = 0;
                }
            } catch (InterruptedException e) {

            }
        }

        public void work() {
            try {
                Thread.sleep((int) (Math.random() * 1000));
            } catch (InterruptedException e) {

            }
        }

        public void notifyAvailable() {
            try {
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
                } else {
                    state = 0;
                }
            } catch (InterruptedException e) {

            }
        }
    }

    class User implements Runnable {
        private int state = 0; // 0 = using software, 1 = complaining, 2 = in meeting

        @Override
        public void run() {
            while (true) {
                while (true) {
                    if (state == 0){
                        useSoftware();
                        state = new Random().nextInt(2);
                    } else if (state == 1) {
                        complain();
                    } else {
                        try {
                            meetingFinished.acquire();
                            state = 0;
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }
        }

        public void useSoftware(){
            try {
                Thread.sleep((int) (Math.random() * 1000));

            } catch (InterruptedException e) {

            }
        }

        public void complain(){
            try {
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
