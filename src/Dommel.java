import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * Created by Vincent on 9/25/2016.
 */
public class Dommel {
    private static final int amountOfSoftwareEngineers = 6; // should be 6
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
        //Initializing the semaphores
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

        //Creating and starting the threads
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
    }

    class Jaap implements Runnable {

        public Jaap() {
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 2000));

                    //Get the amount of software engineers in queue. This is a critical section.
                    softwareEngineerQueueMutex.acquire();
                    int engineersAmount = softwareEngineersInQueue;
                    softwareEngineerQueueMutex.release();

                    //Check if there are any waiting users
                    if (waitingUser.tryAcquire()){
                        //acquire all software engineers.
                        softwareEngineerQueueMutex.acquire();
                        waitingSoftwareEngineer.acquire(softwareEngineersInQueue);
                        softwareEngineerQueueMutex.release();

                        userQueueMutex.acquire();
                        int amount = usersInQueue;
                        userQueueMutex.release();

                        //Check if all the users are at the company
                        userCompanyInvitation.release(amount);
                        userAtCompany.acquire(amount);

                        //Invite all software engineers to the meeting room.
                        softwareEngineerQueueMutex.acquire();
                        softwareEngineerInvitation.release(softwareEngineersInQueue);
                        softwareEngineerQueueMutex.release();

                        //Invite all the users to the meeting room
                        userMeetingInvitation.release(amount);

                        //Use a semaphore to make sure only one user gets in the meeting room. The rest are going back to work is this is 0.
                        softwareEngineerNeededInMeetingRoom.release();

                        //Makes sure all the users are in the meeting room before continuing.
                        userInMeetingRoom.acquire(amount);

                        //Start the meeting
                        userMeeting(amount + 1);

                        //Check if there are at least 3 waiting software engineers
                    } else if (waitingSoftwareEngineer.tryAcquire(3)){

                        //acquire the rest of them and invite them so they can race to the door.
                        waitingSoftwareEngineer.acquire(engineersAmount - 3);
                        softwareEngineerInvitation.release(engineersAmount);

                        //Make sure only 3 of them enter the room.
                        softwareEngineerNeededInMeetingRoom.release(3);

                        //Start the meeting.
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

                //Simulate the meeting
                Thread.sleep((int) (Math.random() * 3000));

                meetingMutex.acquire();
                meetingInProgress = false;
                meetingMutex.release();

                //Open the door.
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

                //Simulate the meeting.
                Thread.sleep((int) (Math.random() * 3000));

                meetingMutex.acquire();
                meetingInProgress = false;
                meetingMutex.release();

                //Open the door.
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
                        //Wait for the meeting to finish so we can get back to work.
                        meetingFinished.acquire();
                        state = 1;
                    } catch (InterruptedException e) {

                    }
                }
            }
        }

        public void goToMeeting(){
            try {
                //Simulate walking to the meeting.
                Thread.sleep(100);

                //Check to see if there are any more software engineers needed in the meeting room.
                if (softwareEngineerNeededInMeetingRoom.tryAcquire()) {
                    state = 2;
                } else {
                    //Go back to work.
                    System.out.println("too late");
                    state = 0;
                }
            } catch (InterruptedException e) {

            }
        }

        public void work() {
            try {
                //Simulate working.
                Thread.sleep((int) (Math.random() * 1000));
            } catch (InterruptedException e) {

            }
        }

        public void notifyAvailable() {
            try {
                //Check if there is a meeting in progress.
                meetingMutex.acquire();
                boolean meeting = meetingInProgress;
                meetingMutex.release();

                if(!meeting) {
                    //Let Jaap know we're available.
                    waitingSoftwareEngineer.release();
                    softwareEngineerQueueMutex.acquire();
                    softwareEngineersInQueue++;
                    softwareEngineerQueueMutex.release();

                    //Start wainting.
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
                            //Wait for the meeting to finish so we can get back to work.
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
                //Simulate using software.
                Thread.sleep((int) (Math.random() * 100000));

            } catch (InterruptedException e) {

            }
        }

        public void complain(){
            try {
                //Let Jaap know we've found a bug.
                waitingUser.release();

                userQueueMutex.acquire();
                usersInQueue++;
                userQueueMutex.release();

                //Wait for an invitation.
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
                //Simulate traveling to the company.
                Thread.sleep((int) (Math.random() * 1000));

                //Notify the company we've arrived.
                userAtCompany.release();

                //Wait for a meeting invitation.
                userMeetingInvitation.acquire();
                goToMeeting();
            } catch (InterruptedException e) {

            }
        }

        public void goToMeeting(){
            try {
                //Simulate walking to the meeting.
                Thread.sleep(100);

                //Let Jaap know we've arrived in the meeting room.
                userInMeetingRoom.release();
            } catch (InterruptedException e) {

            }
        }
    }
}
