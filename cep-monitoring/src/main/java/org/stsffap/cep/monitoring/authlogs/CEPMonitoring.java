package org.stsffap.cep.monitoring.authlogs;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.query.TaskKvStateRegistry;
import org.apache.flink.runtime.state.AbstractKeyedStateBackend;
import org.apache.flink.runtime.state.AbstractStateBackend;
import org.apache.flink.runtime.state.CheckpointStreamFactory;
import org.apache.flink.runtime.state.KeyGroupRange;
import org.apache.flink.runtime.state.OperatorStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.IngestionTimeExtractor;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stsffap.cep.monitoring.authlogs.events.LoginEvent;
import org.stsffap.cep.monitoring.authlogs.events.PotentialFraudSequence;
import org.stsffap.cep.monitoring.authlogs.sources.LoginEventSource;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author    Mahesh D
 */
public class CEPMonitoring {
    private static final Logger LOGGER = LoggerFactory.getLogger(CEPMonitoring.class);

    private static final long PAUSE = 1000;
    private static final int NUM_FAILED_ATTEMPTS = 2;

    /**
     * @author    Mahesh D
     */
    public static class MySinkForwarder implements SinkFunction<PotentialFraudSequence> {
        private static final Logger LOGGER = LoggerFactory.getLogger(MySinkForwarder.class);

        @Override
        public void invoke(PotentialFraudSequence potentialFraudSequence) throws Exception {
            LOGGER.info("======== WRITING TO CUSTOM END-POINTS ========");
            LOGGER.info("==== Event = {} ====", potentialFraudSequence.toString());
        }
    }

    /**
     * @author    Mahesh D
     */
    public static class MyStateStore extends AbstractStateBackend {
        private static final Logger LOGGER = LoggerFactory.getLogger(MyStateStore.class);
        private final MemoryStateBackend memoryStateBackend = new MemoryStateBackend();

        @Override
        public CheckpointStreamFactory createStreamFactory(JobID jobId, String operatorIdentifier) throws IOException {
            LOGGER.info("Method createStreamFactory called..");
            return memoryStateBackend.createStreamFactory(jobId, operatorIdentifier);
        }

        @Override
        public CheckpointStreamFactory createSavepointStreamFactory(JobID jobId, String operatorIdentifier, @Nullable String targetLocation) throws IOException {
            LOGGER.info("Method createSavepointStreamFactory called..");
            return memoryStateBackend.createSavepointStreamFactory(jobId, operatorIdentifier, targetLocation);
        }

        @Override
        public <K> AbstractKeyedStateBackend<K> createKeyedStateBackend(Environment env, JobID jobID, String operatorIdentifier, TypeSerializer<K> keySerializer, int numberOfKeyGroups, KeyGroupRange keyGroupRange, TaskKvStateRegistry kvStateRegistry) throws IOException {
            LOGGER.info("Method createKeyedStateBackend called..");
            return memoryStateBackend.createKeyedStateBackend(env, jobID, operatorIdentifier, keySerializer, numberOfKeyGroups, keyGroupRange, kvStateRegistry);
        }

        @Override
        public OperatorStateBackend createOperatorStateBackend(Environment env, String operatorIdentifier) throws Exception {
            LOGGER.info("Method createOperatorStateBackend called..");
            return memoryStateBackend.createOperatorStateBackend(env, operatorIdentifier);
        }
    }

    public static void main(String args[]) throws Exception {
        final String firstEventIdentifier  = "first";
        final String secondEventIdentifier = "second";

        BasicConfigurator.configure();

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setMaxParallelism(1);
        env.setParallelism(1);
        env.setStateBackend(new MyStateStore());

        final DataStream<LoginEvent> rawInputStream = env
                .addSource(new LoginEventSource(new LoginEventSource.LoginEventGenerator(), PAUSE))
                .assignTimestampsAndWatermarks(new IngestionTimeExtractor<>());

        final Pattern<LoginEvent,LoginEvent> failedLogins = Pattern.<LoginEvent>begin(firstEventIdentifier)
                .where(new IterativeCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent value, Context<LoginEvent> ctx) throws Exception {
                        return value.isFailedLogin();
                    }
                }).times(NUM_FAILED_ATTEMPTS)
                .next(secondEventIdentifier)
                .where(new IterativeCondition<LoginEvent>() {
                    @Override
                    public boolean filter(LoginEvent value, Context<LoginEvent> ctx) throws Exception {
                        return value.isSuccessfulLogin();
                    }
                });

        final PatternStream<LoginEvent> pattern = CEP.pattern(rawInputStream.keyBy(event -> event.getUserName()), failedLogins);

        // final DataStream<LoginFailureSequence> sequences = pattern.flatSelect((patternMap, out) ->
        //     patternMap.forEach((key, values) -> {
        //         LOGGER.info("Selection for key={}, size={}", key, values.size());
        //         final LoginFailureSequence loginFailureSequence = new LoginFailureSequence(NUM_FAILED_ATTEMPTS, values.get(0).getUserName(), values);
        //         loginFailureSequence.print();
        //         out.collect(loginFailureSequence);
        //     })
        // );

        final DataStream<PotentialFraudSequence> sequences = pattern.flatSelect((patternMap, out) ->
                out.collect(new PotentialFraudSequence(patternMap.get(firstEventIdentifier),
                        patternMap.get(secondEventIdentifier))));

        sequences.addSink(new MySinkForwarder());
        //! sequences.print();
        env.execute("CEP Audit Log Failure Sequence monitoring job");
    }
}
