package se.tre.freki.grpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.addCallback;

import se.tre.freki.core.DataPointsClient;

import com.google.common.util.concurrent.FutureCallback;
import io.grpc.stub.StreamObserver;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Implementation of the protocol buffers {@code DataPointAdder} service.
 */
class DataPointAdderImpl implements DataPointAdderGrpc.DataPointAdder {
  private final DataPointsClient dataPointsClient;

  @Inject
  DataPointAdderImpl(final DataPointsClient dataPointsClient) {
    this.dataPointsClient = checkNotNull(dataPointsClient);
  }

  @Override
  public void addFloat(final AddFloatRequest request,
                       final StreamObserver<AddDataPointResponse> responseObserver) {
    addCallback(
        dataPointsClient.addPoint(request.getMetric(), request.getTimestamp(), request.getValue(),
            request.getTags()), new AddPointCallback(responseObserver));
  }

  @Override
  public void addDouble(final AddDoubleRequest request,
                        final StreamObserver<AddDataPointResponse> responseObserver) {
    addCallback(
        dataPointsClient.addPoint(request.getMetric(), request.getTimestamp(), request.getValue(),
            request.getTags()), new AddPointCallback(responseObserver));
  }

  @Override
  public void addLong(final AddLongRequest request,
                      final StreamObserver<AddDataPointResponse> responseObserver) {
    addCallback(
        dataPointsClient.addPoint(request.getMetric(), request.getTimestamp(), request.getValue(),
            request.getTags()), new AddPointCallback(responseObserver));
  }

  private static class AddPointCallback implements FutureCallback<Void> {
    private final StreamObserver<AddDataPointResponse> responseObserver;

    public AddPointCallback(final StreamObserver<AddDataPointResponse> responseObserver) {
      this.responseObserver = responseObserver;
    }

    @Override
    public void onSuccess(@Nullable final Void result) {
      responseObserver.onValue(
          AddDataPointResponse
              .newBuilder()
              .setMessage("OK")
              .build());
      responseObserver.onCompleted();
    }

    @Override
    public void onFailure(final Throwable throwable) {
      responseObserver.onValue(
          AddDataPointResponse
              .newBuilder()
              .setMessage(throwable.getMessage())
              .build());
      responseObserver.onCompleted();
    }
  }
}
