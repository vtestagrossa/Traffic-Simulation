
package Traffic;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Traffic.Stoplight.LightColor;

import java.awt.GridLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Vincent Testagrossa
 * CMSC 335 - Project 3
 *
 * Has buttons to start, pause, and stop the simulation. Has buttons to add/remove a car, and to add/remove a stoplight. Default
 * running configuration has 1 of each, but many can be added.
 * 
 * Displays 0 or more cars in sorted lists with the traffic light they're approaching at the top. The JLabel for the light changes
 * color to match the output from the thread, and the name is displayed as the text. Each car displays its name, current position,
 * sort order and the next light it will approach.
 * 
 * 
 * 
 * All of the dynamic swing elements are held in CopyOnWriteArrayLists and repopulated at 500ms intervals, or whenever a change is made
 * via buttons. There was a tradeoff in performance vs real-time with removing and adding elements to the panels, redrawing the changes,
 * and being able to ensure that the arraylists were still populated with the same number of elements as before (to not overflow or underflow
 * the index). 
 * Controls the flow through the running loop with atomic boolean. Paused sends a wait() call, which will get resumed upon the 
 * call to resume() which sends a notify() signal.
 */
public class TestingTime extends JFrame implements Runnable{
    static JLabel timeLabel, elapsedTimeLabel;
    private CopyOnWriteArrayList<JLabel> carData = new CopyOnWriteArrayList<JLabel>();
    private CopyOnWriteArrayList<JLabel> lightData = new CopyOnWriteArrayList<JLabel>();
    private CopyOnWriteArrayList<JPanel> trafficPanels = new CopyOnWriteArrayList<JPanel>();
    
    private JPanel topPanel, dataPanel;

    private JButton startButton = new JButton("Start"), 
                    pauseButton = new JButton("Pause"),
                     stopButton = new JButton("Stop"),
                   addCarButton = new JButton("Add Car"),
                removeCarButton = new JButton("Remove Car"),
                 addLightButton = new JButton("Add Stoplight"),
              removeLightButton = new JButton("Remove Stoplight");

    private Clock clock;
    private CopyOnWriteArrayList<Car> cars = new CopyOnWriteArrayList<Car>();
    private CopyOnWriteArrayList<Stoplight> stoplights = new CopyOnWriteArrayList<Stoplight>();

    private AtomicBoolean paused = new AtomicBoolean(false);
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean addingCar = new AtomicBoolean(false);
    private AtomicBoolean addingLight = new AtomicBoolean(false);
    private AtomicBoolean removingCar = new AtomicBoolean(false);
    private AtomicBoolean removingLight = new AtomicBoolean(false);

    private int carCount = 3;
    private int lightCount = 3;
    private long localTimer = 1000, startTimer = 0;;

    private boolean guiActive = true;

    private static Thread gui;
    
    public static void main(String[] args) throws Exception {
        TestingTime frame = new TestingTime();
        gui = new Thread(frame, "GUI");
        gui.start();
        System.out.println(Thread.currentThread().getName());
    }
    public TestingTime(){
        super("Real-Time Traffic Light Simulation");
        setLayout(new GridLayout(2, 1));
        timeLabel = new JLabel("Current Time: ");
        elapsedTimeLabel = new JLabel("Elapsed Time: ");
        buildTopPanel();
        initButtons();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 800);
        setVisible(true);

    }

    //Builds the top part of the interface with all of the buttons, plus the clock's labels.
    private void buildTopPanel(){
        topPanel = new JPanel();
        GroupLayout topLayout = new GroupLayout(topPanel);
        topPanel.setLayout(topLayout);

        topLayout.setAutoCreateGaps(true);
        topLayout.setAutoCreateContainerGaps(true);

        topLayout.setHorizontalGroup(topLayout.createSequentialGroup()
        .addContainerGap(50, 100)
        .addGroup(topLayout.createSequentialGroup()
            .addComponent(startButton)
            .addComponent(pauseButton)
            .addComponent(stopButton))
            .addGap(30)
            .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(addCarButton)
                .addComponent(removeCarButton))
            .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(addLightButton)
                .addComponent(removeLightButton))
            .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(timeLabel)    
                .addComponent(elapsedTimeLabel))
        .addContainerGap(50, 100)
        );

        topLayout.setVerticalGroup(topLayout.createSequentialGroup()
            .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(startButton)
                .addComponent(pauseButton)
                .addComponent(stopButton)
                .addGroup(topLayout.createSequentialGroup()
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(addCarButton)
                    .addComponent(addLightButton)
                    .addComponent(timeLabel))
                .addGroup(topLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(removeCarButton)
                    .addComponent(removeLightButton)
                    .addComponent(elapsedTimeLabel))))
        );
        dataPanel = new JPanel();
        dataPanel.setLayout(new FlowLayout());
        add(topPanel);
        add(dataPanel);
    }
    public void updateTime(String time, String elapsed){
        timeLabel.setText(time);
        elapsedTimeLabel.setText(elapsed);
    }

    /**
     * Adds the event handlers for each of the buttons:
     * 
     * startButton:
     * Check if the simulation is running. If it is, print the message. If it isn't, start all of the threads and
     * set the AtomicBoolean to true.
     * 
     * pauseButton:
     * Checks if the simulation is paused. If it isn't, pauses all of the threads and changes the text to resume.
     * If it is, resumes all of the threads and changes the text back to pause. Toggles the paused AtomicBoolean.
     * 
     * stopButton:
     * Checks if the simulation is running. If it is, calls the stop method for all the threads.
     * 
     * addCar/removeCar/addlight/removelight:
     * sets the atomicboolean for each function, which is handled by the run method.
     */
    public void initButtons(){
        startButton.addActionListener((ActionEvent e) -> {
            /**
             * Need to create new instances of the threaded objects here. Since stop() interrupts the threads and they
             * return from the catch block, they will be garbage collected.
             */
            if (!running.get()){
                initCars(carCount);
                initLights(lightCount);
                updateGUI();
                for (Car car : cars){
                    if (car != null){
                        car.start();
                    }
                }
                for (Stoplight light : stoplights){
                    if (light != null){
                        light.start();
                    }
                }
                clock = new Clock("Clock", this, "updateTime");
                clock.start();
                running.set(true);
            }
            else {
                System.out.println("System is already running.");
            }
        });
        pauseButton.addActionListener((ActionEvent e) -> {
            if (!paused.get() && running.get()) {
                for (Car car : cars){
                    if (car != null){
                        car.pause();
                    }
                }
                clock.pause();
                pauseButton.setText("Resume");
                paused.set(true);
            }
            else if (paused.get() && running.get()) {
                for (Car car : cars){
                    if (car != null){
                        car.resume();
                    }
                }
                clock.resume();
                pauseButton.setText("Pause");
                paused.set(false);
            }
            else if (!running.get()) {
                System.out.println("The threads are not started yet.");
            }
        });
        stopButton.addActionListener((ActionEvent e) -> {
            /**
             * Need to reinstantiate the threads, since they can't be restarted.
             */
            if (running.get()) {
                for (Car car : cars){
                    if (car != null){
                        car.stop();
                    }
                }
                clock.stop();
                running.set(false);
                if (paused.get()){
                    pauseButton.setText("Pause");
                    paused.set(false);
                }
            }
            else {
                System.out.println("System is already stopped.");
            }
        });

        /**
         * Sets atomicboolean values if the simulation is currently executing to temporarily pause the updates, so that there's no 
         * indexoutofboundsexception because the cars and lights get removed or added in the middle of the loop. If the simulation
         * isn't currently running (stopped), the counters used in the creation of the threads can simply be incremented/decremented
         * to add and remove cars and stoplights.
         */
        addCarButton.addActionListener((ActionEvent e) -> {
            if (running.get()){
                addingCar.set(true);
            } else {
                System.out.println("Car" + carCount + " added...");
            }
            carCount += 1;
        });
        removeCarButton.addActionListener((ActionEvent e) -> {
            if (running.get()){
                if (!cars.isEmpty()){
                    removingCar.set(true);
                }
            }
            else if (carCount > 0) {
                carCount -= 1;
            }
        });
        addLightButton.addActionListener((ActionEvent e) -> {
            if (running.get()){
                addingLight.set(true);
            } else {
                System.out.println("Stoplight" + lightCount + " added...");
            }
            lightCount += 1;
        });
        removeLightButton.addActionListener((ActionEvent e) -> {
            if (running.get()){
                if (!stoplights.isEmpty()){
                    removingLight.set(true);
                }
            }
            else if (lightCount > 1) {
                lightCount -= 1;
            }
        });
    }
    private void addCar() {
        int i = cars.size();
        Car car = new Car("Car" + i, i*10, 6 + i * 10);
        cars.add(car);
        updateGUI();
    }
    private void removeCar() {
        // ensure no underflow and remove the last item.
        int i = cars.size() - 1;
        if (!cars.isEmpty()){
            cars.get(i).stop();
            cars.remove(i);
            carCount -= 1;
        }
        updateGUI();
    }
    private void addLight(){
        int i = stoplights.size();
        Stoplight light = new Stoplight("Stoplight" + i, i);
        stoplights.add(light);
        updateGUI();
    }
    private void removeLight(){
        // ensure no underflow and remove the last item.
        int i = stoplights.size() - 1;
        if (!stoplights.isEmpty() && lightCount > 1){
            stoplights.get(i).stop();
            stoplights.remove(i);
            lightCount -= 1;
        }
        updateGUI();
    }
    @Override
    public void run() {
        while (guiActive){
            if (addingCar.get()){
                addCar();
                addingCar.set(false);
            }else if (removingCar.get()){
                removeCar();
                removingCar.set(false);
            }

            if (addingLight.get()){
                addLight();
                addingLight.set(false);
            } else if (removingLight.get()){
                removeLight();
                removingLight.set(false);
            }

            if (!paused.get() && running.get()){
                setIndices();
                outputCarData();
                outputLightData();
                updateLightStatus();
                synchronizeTimer();
            }
        }
    }
    private void updateLightStatus(){
        for (Stoplight light : stoplights){
            for (Car car : cars) {
                if (car.getNextLight() == light.getPosition()){
                    car.setNextLightColor(light.getColor());
                    if (light.getColor() == LightColor.GREEN){
                        car.resume();
                    }
                }
            }
        }
    }
    private void initCars(int count) {
        if (!cars.isEmpty()){
            cars.removeAll(cars);
        }
        for (int i = 0; i < count; i++) {
            // Makes a gap between each car, but adds some randomness to the start positions.
            cars.add(new Car("Car" + i, i*10, 6 + i * 10));
        }
        //sorts the cars based on positionX
        cars.sort(null);
    }
    private void initLights(int count){
        if (!stoplights.isEmpty()){
            stoplights.removeAll(stoplights);
        }
        for (int i = 0; i < count; i++){
            stoplights.add(new Stoplight("Stoplight" + i, i));
        }
    }

    // Updates the labels for car and light data
    private void outputCarData() {
        for (int i = 0; i < carData.size(); i++){
            String output = cars.get(i).getName()
                         + " X:" 
                         + cars.get(i).getPositionX() 
                         + " Y:" 
                         + cars.get(i).getPositionY()
                         + " Speed:"
                         + cars.get(i).getSpeed() + " km/h"
                         + " Order: "
                         + cars.get(i).getIndex()
                         + " Next: "
                         + cars.get(i).getNextLight();
            carData.get(i).setText(output);
            
        }
        // Hacked together solution to keep the GUI from updating too often. 
        if (localTimer >= 500){
            updateGUI();
            startTimer = System.currentTimeMillis();
            localTimer = 0;
        }
        localTimer = System.currentTimeMillis() - startTimer;
    }
    private void outputLightData() {
        for (int i = 0; i < lightData.size(); i++){
            Stoplight.LightColor color = stoplights.get(i).getColor();
            String output = stoplights.get(i).getName();
            lightData.get(i).setText(output);
            switch(color){
                case RED:
                    lightData.get(i).setForeground(Color.RED);
                    break;
                case YELLOW:
                    lightData.get(i).setForeground(Color.YELLOW);
                    break;
                case GREEN:
                    lightData.get(i).setForeground(Color.GREEN);
                    break;
            }
        }
    }
    private void setIndices() {
        
        if (!stoplights.isEmpty()){

            //loop through the existing stopLights
            for (int i = 0; i <= stoplights.size(); i++) {
                int count = 0;
                //loop through each car and check if it's before the current stoplight
                for (int j = 0; j < cars.size(); j++){
                    if (cars.get(j).getNextLight() == i){

                        //the car is before this stoplight, so assign its index and increment the counter
                        cars.get(j).setIndex(count);
                        count++;
                    }
                }
            }
        }
        else {
            // there are no stoplights (should only be for testing purposes)

            //loop through the cars and assign their index based on the loop index.
            for (int i = 0; i < cars.size(); i++) {
                cars.get(i).setIndex(i);
            }
        }
    }
    // Needs to rebuild the structures for the cars and the intersections, pass the new intersection count to the cars, then
    // redraw all of the components/graphics.
    private void updateGUI() {
        if (!carData.isEmpty() || !lightData.isEmpty() || !trafficPanels.isEmpty()){
            carData.removeAll(carData);
            lightData.removeAll(lightData);
            trafficPanels.removeAll(trafficPanels);
            dataPanel.removeAll();
            //need to removeAll and rebuild the whole frame.
            validate();
            repaint();
        }
        for (Car car : cars){
            String carLabel = car.getName() + " Position X:" + car.getPositionX() + " Position Y:" + car.getPositionY();
            carData.add(new JLabel(carLabel));
        }
        for (int i = 0; i < stoplights.size(); i++){
            String lightLabel = stoplights.get(i).getName() + " Color: " + stoplights.get(i).getColor();
            lightData.add(new JLabel(lightLabel));
            JPanel panel = new JPanel();
            trafficPanels.add(panel);
            trafficPanels.get(i).setLayout(new BoxLayout(trafficPanels.get(i), BoxLayout.Y_AXIS));
            panel.add(lightData.get(i));
            for (int j = 0; j < cars.size(); j++){
                if (cars.get(j).getNextLight() == stoplights.get(i).getPosition()){
                    panel.add(carData.get(j));
                }
            }
            dataPanel.add(panel);
        }
        pack();
        revalidate();
        repaint();
    }
    private void synchronizeTimer(){
        // Sends a synchronized timer to all the stoplights.
        for (Stoplight light : stoplights){
            light.updateTimer(clock.getElapsedTime());
        }

    }
}
