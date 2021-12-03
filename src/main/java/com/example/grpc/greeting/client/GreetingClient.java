package com.example.grpc.greeting.client;

import com.proto.greet.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GreetingClient {

    ManagedChannel channel;

    public static void main(String[] args) {
        System.out.println("HI im a gRPC client");
        GreetingClient main = new GreetingClient();

        main.run();
    }

    public void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
//        doUnaryCall(channel);
//        doServerStreamingCall(channel);

//        doClientStreamingCall(channel);
//        doBiDiStreamingCall(channel);
        doDeadlineCall(channel);
        System.out.println("Shutting down server");
        channel.shutdown();
    }

    private void doDeadlineCall(ManagedChannel channel) {
        GreetServiceGrpc.GreetServiceBlockingStub client = GreetServiceGrpc.newBlockingStub(channel);
        System.out.println("Sending request with deadline of 500 ms");
        callWithDeadline(client, 500);
        System.out.println("Sending request with deadline of 3000 ms");
        callWithDeadline(client, 3000);
        System.out.println("Sending request with deadline of 100 ms");
        callWithDeadline(client, 100);
    }

    private void callWithDeadline(GreetServiceGrpc.GreetServiceBlockingStub client, int deadline) {
        GreetRequest steve = GreetRequest.newBuilder().setGreeting(Greeting.newBuilder().setFirstName("Steve").build()).build();
        try {
            System.out.println(client.withDeadline(Deadline.after(deadline, TimeUnit.MILLISECONDS))
                    .greetWithDeadline(steve).getResult());
        } catch (StatusRuntimeException e) {
            if(e.getStatus() == Status.DEADLINE_EXCEEDED){
                System.out.println("Deadline exceeded");
            } else {
                e.printStackTrace();
            }
        }
    }

    private void doBiDiStreamingCall(ManagedChannel channel) {
        GreetServiceGrpc.GreetServiceStub asyncClient = GreetServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<GreetEveryoneRequest> requestObserver = asyncClient.greetEveryone(
                new StreamObserver<GreetEveryoneResponse>() {
                    @Override
                    public void onNext(GreetEveryoneResponse value) {
                        System.out.println("Response: " + value.getResult());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Server done sending data");
                        latch.countDown();
                    }
                }
        );

        Arrays.asList("Mike", "Paul", "Peter", "Steve").forEach(name -> {
            System.out.println("Sending: " + name);
            requestObserver.onNext(GreetEveryoneRequest.newBuilder()
                    .setGreeting(Greeting.newBuilder()
                            .setFirstName(name).build())
                    .build());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        requestObserver.onCompleted();

        try {
            latch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doClientStreamingCall(ManagedChannel channel) {
        //create a client
        GreetServiceGrpc.GreetServiceStub asyncClient = GreetServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<LongGreetRequest> requestObserver = asyncClient.longGreet(new StreamObserver<LongGreetResponse>() {
            @Override
            public void onNext(LongGreetResponse value) {
                System.out.println("Response from server");
                System.out.println(value.getResult());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                System.out.println("Server has completed");
                latch.countDown();
            }
        });

        requestObserver.onNext(LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder().setFirstName("Chris").build())
                .build());
        requestObserver.onNext(LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder().setFirstName("john").build())
                .build());
        requestObserver.onNext(LongGreetRequest.newBuilder()
                .setGreeting(Greeting.newBuilder().setFirstName("Dave").build())
                .build());

        //tell server client is done sending data
        requestObserver.onCompleted();

        try {
            latch.await(3L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doServerStreamingCall(ManagedChannel channel) {
        //server streaming
        GreetServiceGrpc.GreetServiceBlockingStub greetClient = GreetServiceGrpc.newBlockingStub(channel);


        GreetManyTimesRequest request = GreetManyTimesRequest.newBuilder().setGreeting(Greeting.newBuilder().setFirstName("Chris").setLastName("Crawford")).build();
        greetClient.greetManyTimes(request)
                .forEachRemaining(resp -> System.out.println(resp.getResult()));
    }

    private void doUnaryCall(ManagedChannel channel) {
        //create greet service client
        GreetServiceGrpc.GreetServiceBlockingStub greetClient = GreetServiceGrpc.newBlockingStub(channel);

        Greeting greeting = Greeting.newBuilder()
                .setFirstName("Chris")
                .setLastName("Crawford").build();
        GreetRequest request = GreetRequest.newBuilder().setGreeting(greeting).build();

        GreetResponse greet = greetClient.greet(request);
        System.out.println(greet.getResult());
//
    }
}
