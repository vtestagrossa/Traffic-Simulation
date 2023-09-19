package Traffic;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import java.text.SimpleDateFormat;
/*
 * Vincent Testagrossa
 * CMSC 335 - Project 3
 * 
 * 
 * Uses a callback method to push the time to the label in the swing app once per second. Synchronizes the timing
 * for the lights using elapsedTime.
 * Controls the flow through the running loop with atomic boolean. Paused sends a wait() call, which will get resumed upon the 
 * call to resume() which sends a notify() signal.
 */
public class Clock implements Runnable {
    private JFrame frame;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private String setTimeCallBack;
    private String name;

    private String pattern = "hh:mm:ss";
    private SimpleDateFormat formatter = new SimpleDateFormat(pattern);
    private Date currentTime;
    private long startTime, elapsedTime, offset;


    public Thread thread;
    public Clock(String name, JFrame frame, String callBackMethod) {
        this.name = name;
        this.frame = frame;
        this.setTimeCallBack = callBackMethod;
        startTime = System.currentTimeMillis();
        elapsedTime = 0;
        offset = 0;
        thread = new Thread(this, name);
        thread.start();
    }

    public void start() {
        if (!running.get()){
            System.out.println(name + " is starting...");
            running.set(true);
        }
    }
    public void pause() {
        if (!paused.get() && running.get()){
            offset = elapsedTime; //save the current elapsed time
            startTime = System.currentTimeMillis(); //reset the start time while we wait for the thread to wait().
            paused.set(true);
            System.out.println(name + " is paused...");
        }
    }
    public synchronized void resume() {
        if (paused.get() && running.get()){
            startTime = System.currentTimeMillis(); //reset the start time to restart the elapsed time clock
            paused.set(false);
            System.out.println(name + " is resuming...");
            notify();
        }
    }
    public void stop() {
        if (thread != null) {
            System.out.println(name + " is stopping...");
            thread.interrupt();
            running.set(false);
        }
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()){
            try {
                synchronized(this){
                    while(paused.get()) {
                        wait();
                    }
                }
                if (running.get()){
                    /*
                     * Since the methods only update 1 second intervals, shorter sleep times means a more responsive thread without
                     * changing the interval.
                     */
                    Thread.sleep(10);
                    String time = getCurrentTime();
                    String elapsed = getElapsedToString();
                    frame.getClass().getMethod(setTimeCallBack, String.class, String.class).invoke(frame, time, elapsed);
                }
            } catch (InterruptedException e) {
                System.out.println(name + " has stopped.");
                return;
            } catch (Exception e) {
                
            }
        }
    }

    public synchronized long getElapsedTime(){
        return elapsedTime;
    }
    
    private String getCurrentTime(){
        currentTime = new Date(System.currentTimeMillis());
        String output = "Current time: " + formatter.format(currentTime) + "\n";
        return output;
    }

    private String getElapsedToString() {
        //Builds a string based on a max time of 24 hours elapsed. Does not track days or higher.
        String output = "Elapsed time: ";
        long hours = 0, minutes = 0, seconds = 0; 
        elapsedTime = (System.currentTimeMillis() - startTime) + offset;
        long elapsedSeconds = elapsedTime/1000;
        seconds = elapsedSeconds%60;
        minutes = elapsedSeconds/60%60;
        hours = elapsedSeconds/60/60%24;

        output += String.format("%02d", hours) + ":";
        output += String.format("%02d", minutes) + ":";
        output += String.format("%02d", seconds);

        return output;
    }
}
