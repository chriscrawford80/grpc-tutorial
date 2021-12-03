package com.example.grpc.greeting.client;

import com.proto.math.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class MathClient {


    public static void main(String[] args) {
        MathClient mathClient = new MathClient();
        mathClient.run();

    }

    public void run(){
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50052)
                .usePlaintext()
                .build();
//        doServerStreamingCall(channel);
        doClientStreamingCall(channel);

        channel.shutdown();
    }

    private void doClientStreamingCall(ManagedChannel channel) {
        CalculatorServiceGrpc.CalculatorServiceStub asyncClient = CalculatorServiceGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<AverageRequest> averageRequestStreamObserver = asyncClient.computeAverage(new StreamObserver<AverageResponse>() {
            @Override
            public void onNext(AverageResponse value) {
                System.out.println("Response from server");
                System.out.println(value.getResult());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                System.out.println("Server done");
                latch.countDown();
            }
        });

//        averageRequestStreamObserver.onNext(AverageRequest.newBuilder()
//                        .setValue(1)
//                .build());
//        averageRequestStreamObserver.onNext(AverageRequest.newBuilder()
//                        .setValue(2)
//                .build());
//        averageRequestStreamObserver.onNext(AverageRequest.newBuilder()
//                        .setValue(3)
//                .build());
//        averageRequestStreamObserver.onNext(AverageRequest.newBuilder()
//                        .setValue(4)
//                .build());
        IntStream.range(1,10000).forEach(v -> {
            averageRequestStreamObserver.onNext(AverageRequest.newBuilder()
                    .setValue(v)
                    .build());
        });

        //tell server done sending messages
        averageRequestStreamObserver.onCompleted();

        try {
            latch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void doServerStreamingCall(ManagedChannel channel) {
        CalculatorServiceGrpc.CalculatorServiceBlockingStub mathClient = CalculatorServiceGrpc.newBlockingStub(channel);
//        System.out.println(mathClient.add(AddRequest.newBuilder().setAdd(Add.newBuilder().setAddend(5).setAugend(1).build()).build()));

        Iterator<DecompResponse> response = mathClient.primeDecomp(DecompRequest.newBuilder().setValue(210).build());
        Iterable<DecompResponse> iterable = () -> response;
        StreamSupport.stream(iterable.spliterator(), false)
                .map(decompResponse -> {
                    int result = decompResponse.getResult();
                    System.out.println("Factor: " + result);
                    return result;
                }).reduce((a,b) -> a*b).ifPresent(System.out::println);

    }
}
