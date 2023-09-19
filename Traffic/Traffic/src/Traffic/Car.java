package Traffic;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import Traffic.Stoplight.LightColor;

/*
 * Vincent Testagrossa
 * CMSC 335 - Project 3
 * 
 * 
 * Cars know if they're at an intersection based on 1000m distance and their next light. Traffic signals are sent from 
 * the GUI thread.
 * 
 * Controls the flow through the running loop with atomic boolean. Paused sends a wait() call, which will get resumed upon the 
 * call to resume() which sends a notify() signal.
 */
public class Car implements Runnable, Comparable<Car>{
    /**
     * Need to know the positions and states of all the lights to determine if they need to stop. Each light is assumed to be 1000m apart, so
     * lightCount is used to determine which interSection the car needs to look at. 
     * 
     * Since the lights are going to be updating the Main thread,
     * Cars can get the info from the main thread when they get within a certain distance of the intersection. The main thread knows how many
     * lights it has created, so it makes sense to have the car periodically request that info from Main.
     */
    
    // Requires states to control and read the status for the run method.
    private final AtomicBoolean running = new AtomicBoolean(false);
    public final AtomicBoolean atStopLight = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    // Positional and speed data. If acceleration and deceleration were required,
    // there would be variables for that, plus something like targetSpeed.
    private double x, y, size = 5;                  // x and y positions for the car. Size is in meters (these cars are pretty big).
    private int index,                  // positional index of the car. Determined by the main thread.
                nextLight;              // index of the next light, determined by the car
    private double curSpeed = 0, maxSpeed = 0.25;   // current speed of the car and max speed.


    //Thread info
    private Thread thread;
    private String name;

    LightColor nextLightColor = LightColor.RED;

    public Car(String name, int min, int max) {
        this.name = name;
        x = ThreadLocalRandom.current().nextInt(min, max);
        y = 0;
        nextLight = getNextLight();
        setIndex(0);
        thread = new Thread(this, name);
        thread.start();
    }
    public void start() {
        if (!running.get()){
            curSpeed = maxSpeed;
            System.out.println(name + " is starting...");
            running.set(true);
        }
    }
    public void pause() {
        if (!paused.get()) {
            paused.set(true);
            System.out.println(name + " is waiting...");
        }
    }
    /**
     * This method was causing problems with pausing and resuming at a red light without the else if (just another if).
     * Needs to be mutually exclusive conditions in order to differentiate between paused, waiting at a red light, and 
     * paused at a red light.
     */
    public synchronized void resume() {
        if (paused.get() && running.get()) {
            curSpeed = maxSpeed;
            paused.set(false);
            System.out.println(name + " is resuming...");
            notify();
        } else if (atStopLight.get() && running.get()){
            curSpeed = maxSpeed;
            atStopLight.set(false);
            System.out.println(name + " is leaving the stoplight...");
            notify();
        }
    }
    public void stop() {
        if (running.get()){
            running.set(false);
            System.out.println(name + " is stopping...");
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        start();
        while(running.get()){
            try {
                synchronized(this){
                    while (paused.get()){
                        System.out.println(name + " is waiting...");
                        wait();
                    }
                    while (atStopLight.get()){
                        System.out.println(name + " is at a stop light...");
                        curSpeed = 0;
                        wait();
                    }
                }
                if (running.get() && !paused.get() && !atStopLight.get()){
                    x += curSpeed;
                    checkNextLight();
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                System.out.println(name + " has stopped.");
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }
    }
    public synchronized String getSpeed(){
        //0.25 * 100 = 25 m/s
        //(25 m/s * 60s * 60m)/1000 = 90km/h
        String output = String.valueOf((curSpeed * 100 * 60 * 60)/1000);
        return output;
    }
    public synchronized void setNextLightColor(LightColor nextColor) {
        nextLightColor = nextColor;
    }
    public synchronized int getPositionX() {
        return (int)x;
    }
    public synchronized int getPositionY() {
        return (int)y;
    }
    public synchronized String getName() {
        return name;
    }
    public synchronized int getNextLight() {
        nextLight = getPositionX()/1000;
        return nextLight;
    }
    public synchronized void setIndex(int x){
        index = x;
    }
    public synchronized int getIndex(){
        return index;
    }
    private void checkNextLight(){
        int position = getPositionX();
        if (position >= ((1 + nextLight) * 1000) - (int)(size + size * index)){
            if (nextLightColor == LightColor.RED){
                atStopLight.set(true);
            }
        }
    }
    @Override
    public int compareTo(Car o) {
        if (getPositionX() > o.getPositionX()){
            return -1;
        } else if (getPositionX() < o.getPositionX()){
            return 1;
        }
        return 0;
    }
}
