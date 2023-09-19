package Traffic;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Vincent Testagrossa
 * CMSC 335 - Project 3
 * 
 * 
 * GUI thread sends the elapsedTime from clock, which is used to set a 60 second timer. That timer controls the timing of
 * the lights.
 * Controls the flow through the running loop with atomic boolean. Paused sends a wait() call, which will get resumed upon the 
 * call to resume() which sends a notify() signal.
 */
public class Stoplight implements Runnable{
    /**
     * Only really needs to know about the state of the lights and position of the stoplight. Cars will need to know how many lights there are
     * and all of their positions, so they'll need a collection internally and to check the state for each periodically. Positions are determined
     * by the program creating the intersections, which should ideally add them where it makes sense.
     */
    public enum LightColor {
        RED, YELLOW, GREEN
    }
    private LightColor color = LightColor.GREEN;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean paused = new AtomicBoolean(false);

    private Thread thread;
    private String name;
    private int position;
    private int timer, offset;

    public Stoplight(String name, int position) {
        this.name = name;
        this.position = position;
        timer = 0;
        offset = ThreadLocalRandom.current().nextInt(0, 59);
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
        if (running.get() && !paused.get()){
            paused.set(true);
            System.out.println(name + " is paused...");
        }
    }
    public synchronized void resume() {
        if (running.get() && paused.get()){
            paused.set(false);
            System.out.println(name + " is resuming...");
            notify();
        }
    }
    public void stop() {
        if(running.get()){
            System.out.println(name + " is stopping...");
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        System.out.println(name + " Timer " + offset);
        start();
        while (running.get()){
            try {
                synchronized(this){
                    if (paused.get()){
                        System.out.println(name + " is waiting...");
                        wait();
                    }
                }
                if (!paused.get()){
                    changeLight();
                }
            } catch (InterruptedException e) {
                System.out.println(name + " has stopped.");
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void changeLight() {
        /* switch(timer){
            case 20:
                color = LightColor.YELLOW;
                break;
            case 23:
                color = LightColor.RED;
                break;
            case 59:
                color = LightColor.GREEN;
                break;
        } */
        if (timer >= 0 && timer <= 20){
            color = LightColor.GREEN;
        } 
        else if (timer > 20 && timer <= 23){
            color = LightColor.YELLOW;
        }
        else if (timer > 23 && timer <= 59){
            color = LightColor.RED;
        }
        else if (timer >= 60){
            System.out.println("Something is wrong");
        }
    }
    public synchronized void updateTimer(long time){
        timer = (((int)time/1000) + offset)%60; //60 second timer controlled by the Clock and handled by the main thread.
    }
    public synchronized String getName() {
        return name;
    }
    public synchronized LightColor getColor(){
        return color;
    }
    public synchronized int getPosition() {
        return position;
    }
}
