/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.event.simulator.randomdatafeedsimulation.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.executionplandelpoyer.ExecutionPlanDeployer;
import org.wso2.carbon.event.executionplandelpoyer.ExecutionPlanDto;
import org.wso2.carbon.event.executionplandelpoyer.StreamDefinitionDto;
import org.wso2.carbon.event.querydeployer.bean.Event;
import org.wso2.carbon.event.simulator.EventSimulator;
import org.wso2.carbon.event.simulator.randomdatafeedsimulation.bean.RandomDataSimulationConfig;
import org.wso2.carbon.event.simulator.randomdatafeedsimulation.bean.StreamAttributeDto;
import org.wso2.carbon.event.simulator.randomdatafeedsimulation.utils.AttributeGenerator;
import org.wso2.carbon.event.simulator.utils.EventConverter;


import java.util.Arrays;


/**
 * This simulator simulates the execution plan by sending events. These events are generated by
 * generated random values according to given configuration.
 * <p>
 * This simulator class implements EventSimulator Interface
 * <p>
 * For simulation It generates Random values for an event using
 * {@link AttributeGenerator#generateAttributeValue(StreamAttributeDto, String)}
 */
public class RandomDataEventSimulator implements EventSimulator {
    private static final Log log = LogFactory.getLog(RandomDataEventSimulator.class);
    /**
     * Percentage of send events
     */
    private double percentage = 0;

    /**
     * Flag used to pause the simulation.
     */
    public static boolean isPaused = false;

    /**
     * Flag used to stop the simulation.
     */
    public static boolean isStopped = false;

    private static final Object lock = new Object();

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    /**
     * Initialize RandomDataEventSimulator to start the simulation
     */
    public RandomDataEventSimulator() {
    }

    /**
     * start simulation for given configuration
     *
     * @param randomDataSimulationConfig RandomDataSimulationConfig
     * @return true if all event send successfully
     */
    public boolean send(RandomDataSimulationConfig randomDataSimulationConfig) {
//        // TODO: 13/12/16 Move this to execution deployer function if possible
//        ExecutionPlanDeployer.getExecutionPlanDeployer().getExecutionPlanRuntime().start();
//
//        EventCreator eventCreator = new EventCreator(ExecutionPlanDeployer.getExecutionPlanDeployer().getExecutionPlanDto(), randomDataSimulationConfig);
//        Thread eventCreatorThread = new Thread(eventCreator);
//        eventCreatorThread.start();
        synchronized (this) {
            sendEvent(ExecutionPlanDeployer.getExecutionPlanDeployer().getExecutionPlanDto(), randomDataSimulationConfig);
        }
        return true;
    }

    /**
     * send created event to siddhi input handler
     *
     * @param streamName Stream Name
     * @param event      Created Event
     */
    @Override
    public void send(String streamName, Event event) {
        try {
            /*
            get the input handler for particular input stream Name and send the event to that input handler
             */
            ExecutionPlanDeployer.getExecutionPlanDeployer().getInputHandlerMap().get(streamName).send(event.getEventData());
        } catch (InterruptedException e) {
            log.error("Error occurred during send event :" + e.getMessage());
        }
    }


    @Override
    public void pauseEvents() {

    }

    @Override
    public void stopEvents() {
          stop();
    }

    @Override
    public void resumeEvents() {

    }


    private void sendEvent(ExecutionPlanDto executionPlanDto, RandomDataSimulationConfig randomDataSimulationConfig) {
        try {
            int delay = randomDataSimulationConfig.getDelay();
            if (delay <= 0) {
                log.warn("Events will be sent continuously since the delay between events are set to "
                        + delay + "milliseconds");
                delay = 0;
            }

            double noOfEvents = randomDataSimulationConfig.getEvents();
            if (noOfEvents < 0) {
                log.error("No of events to be generated can't be in negative values");
                // TODO: 29/11/16 throw exception
            }

            StreamDefinitionDto streamDefinitionDto = executionPlanDto.getInputStreamDtoMap().get(randomDataSimulationConfig.getStreamName());

            try {
                    /*
                    Generate dummy attributes to warm up Random Data generation. Because It takes some ms to generate 1st value.
                    It effects the delay between two events and trade off in performance also
                    So to reduce this draw back Initially it generates some dummy attributes
                    */

                String[] dummyAttribute = new String[randomDataSimulationConfig.getAttributeSimulation().size()];
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < randomDataSimulationConfig.getAttributeSimulation().size(); j++) {
                        dummyAttribute[j] = AttributeGenerator.generateAttributeValue(randomDataSimulationConfig.getAttributeSimulation().get(j),
                                streamDefinitionDto.getStreamAttributeDtos().get(j).getAttributeType());
                    }
                }
                    /*
                    We no longer need dummyAttribute. It will get garbage collected when there
                    are no more references to it.
                     */
                dummyAttribute = null;
            } catch (Exception e) {
                log.error("Error occurred during generating dummi attributes" + e.getMessage());
            }

                /*
                at this point starts to generate random  attribute values and convert it into siddhi event
                and send that event to input handler up to no of events reached to events given by user
                 */

            for (int i = 0; i < noOfEvents; i++) {
                int noOfAttributes = randomDataSimulationConfig.getAttributeSimulation().size();
                if (!isPaused) {
                    try {
                        String[] attributeValue = new String[noOfAttributes];

                        //Generate Random values for each attribute
                        for (int j = 0; j < noOfAttributes; j++) {
                            attributeValue[j] = AttributeGenerator.generateAttributeValue(randomDataSimulationConfig.getAttributeSimulation().get(j),
                                    streamDefinitionDto.getStreamAttributeDtos().get(j).getAttributeType());
                        }

                        //convert Attribute values into event
                        Event event = EventConverter.eventConverter(randomDataSimulationConfig.getStreamName(), attributeValue, executionPlanDto);
                        //calculate percentage that event has send
                        percentage = ((i + 1) * 100) / noOfEvents;// TODO: 13/12/16 delete sout
                        System.out.println("Input Event " + Arrays.deepToString(event.getEventData()) + "Percentage :" + percentage );
                        //System.out.println("------------------------------------------------------");

                        //send the event to input handler
                        send(randomDataSimulationConfig.getStreamName(), event);

                        //delay between two events
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    } catch (Exception e) {
                        log.error("Error occurred : Failed to send an event" + e.getMessage());
                    }
                } else if (isStopped) {
                    break;
                } else {
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            continue;
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error("Error occurred");
        }

    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public static void stop() {
        isPaused = true;
        isStopped = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }


}