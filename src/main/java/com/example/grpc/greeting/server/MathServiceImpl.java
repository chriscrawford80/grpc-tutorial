package com.example.grpc.greeting.server;

import com.proto.math.*;
import io.grpc.stub.StreamObserver;

public class MathServiceImpl extends CalculatorServiceGrpc.CalculatorServiceImplBase {

    @Override
    public void add(AddRequest request, StreamObserver<AddResponse> responseObserver) {
        int sum = request.getAdd().getAddend() + request.getAdd().getAugend();
        AddResponse build = AddResponse.newBuilder().setResult(sum).build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void primeDecomp(DecompRequest request, StreamObserver<DecompResponse> responseObserver) {
//        super.primeDecomp(request, responseObserver);

        var k = 2;
        var N = request.getValue();

        while(N > 1){
            if(N%k == 0){
                responseObserver.onNext(DecompResponse.newBuilder().setResult(k).build());
                N = N / k;
            } else {
                k = k +1;
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<AverageRequest> computeAverage(StreamObserver<AverageResponse> responseObserver) {
        //How to handle each request
        return new StreamObserver<>() {
            int result = 0;
            int numOfRequest = 0;

            @Override
            public void onNext(AverageRequest value) {
                result += value.getValue();
                numOfRequest++;

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(
                        AverageResponse.newBuilder()
                                .setResult((double) result / numOfRequest)
                                .build());
                responseObserver.onCompleted();
            }
        };
    }
}
